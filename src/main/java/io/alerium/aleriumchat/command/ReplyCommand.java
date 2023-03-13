package io.alerium.aleriumchat.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.alerium.aleriumchat.AleriumChat;
import io.alerium.aleriumchat.event.PrivateMessageEvent;
import io.alerium.aleriumchat.manager.ChatManager;
import io.alerium.aleriumchat.manager.format.PrivateMessageFormat;
import io.alerium.aleriumchat.playerdata.PlayerDataManager;
import io.alerium.aleriumchat.playerdata.data.PlayerData;
import net.justugh.japi.database.redis.client.RedisClient;
import net.justugh.japi.database.redis.listener.ListenerComponent;
import net.justugh.japi.message.MessageProvider;
import net.justugh.japi.util.Placeholder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@CommandAlias("reply|r")
@CommandPermission("simplechat.commands.reply")
public class ReplyCommand extends BaseCommand {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();

    @Dependency
    private AleriumChat plugin;

    @Dependency
    private RedisClient redisClient;

    @Dependency
    private MessageProvider messageProvider;

    @Dependency
    private ChatManager chatManager;

    @Dependency
    private PlayerDataManager dataManager;

    @Default
    public void onDefault(Player player, @Optional String message) {
        if (message == null) {
            player.sendMessage(messageProvider.getMessage("Reply.Syntax"));
            return;
        }

        PlayerData playerData = dataManager.getDataById(player.getUniqueId().toString());

        if (playerData == null) {
            player.sendMessage(messageProvider.getMessage("General.Invalid-Data"));
            return;
        }

        UUID lastMessagedPlayer = playerData.getLastMessagedPlayer();

        if (lastMessagedPlayer == null) {
            player.sendMessage(messageProvider.getMessage("Reply.No-One-Messaged"));
            return;
        }

        if (!dataManager.isOnline(lastMessagedPlayer)) {
            player.sendMessage(messageProvider.getMessage("Reply.Not-Online",
                    new Placeholder("%target%", Bukkit.getOfflinePlayer(lastMessagedPlayer).getName())));
            return;
        }

        CompletableFuture.supplyAsync(() -> dataManager.getDataFromDB(lastMessagedPlayer.toString(), true))
                .thenAccept(targetData -> {
                    if (targetData != null && !targetData.isPrivateMessagesEnabled()
                            && !player.hasPermission("simplechat.messagesdisabled.bypass")) {
                        player.sendMessage(messageProvider.getMessage("General.Messages-Disabled",
                                new Placeholder("%player%", Bukkit.getOfflinePlayer(lastMessagedPlayer).getName())));
                        return;
                    }

                    if (targetData != null && targetData.getIgnoredPlayers().contains(player.getUniqueId())
                            && !player.hasPermission("simplechat.ignored.bypass")) {
                        player.sendMessage(messageProvider.getMessage("General.Ignored",
                                new Placeholder("%player%", Bukkit.getOfflinePlayer(lastMessagedPlayer).getName())));
                        return;
                    }

                    PrivateMessageFormat senderFormat = chatManager.getMessageFormat(player, true);
                    PrivateMessageFormat receiverFormat = chatManager.getMessageFormat(Bukkit.getOfflinePlayer(lastMessagedPlayer), false);

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        PrivateMessageEvent privateMessageEvent = new PrivateMessageEvent(player.getUniqueId(), lastMessagedPlayer,
                                Component.text(message), plugin.getRedisClient().getServerId());
                        Bukkit.getServer().getPluginManager().callEvent(privateMessageEvent);

                        if (privateMessageEvent.isCancelled()) {
                            return;
                        }

                        Component senderMessage = senderFormat.getFormat(player, Bukkit.getOfflinePlayer(lastMessagedPlayer), privateMessageEvent.getMessage());

                        redisClient.sendListenerMessage(
                                new ListenerComponent(null, "private-message")
                                        .addData("sender", player.getUniqueId())
                                        .addData("targetUUID", lastMessagedPlayer)
                                        .addData("rawMessage", serializer.serialize(privateMessageEvent.getMessage()))
                                        .addData("messageFormat", receiverFormat)
                        );

                        player.sendMessage(senderMessage);
                    });
                }).exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
    }

}
