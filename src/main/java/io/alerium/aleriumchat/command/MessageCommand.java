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

@CommandAlias("message|msg|whisper|pm|dm|tell")
@CommandPermission("simplechat.commands.message")
public class MessageCommand extends BaseCommand {

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
    @CommandCompletion("@RedisPlayers")
    public void onDefault(Player player, @Optional String targetName, @Optional String message) {
        if (targetName == null || message == null) {
            player.sendMessage(messageProvider.getMessage("Message.Syntax"));
            return;
        }

        PlayerData playerData = dataManager.getDataById(player.getUniqueId().toString());

        if (playerData == null) {
            player.sendMessage(messageProvider.getMessage("General.Invalid-Data"));
            return;
        }

        if (!dataManager.isOnline(targetName)) {
            player.sendMessage(messageProvider.getMessage("Message.Not-Online", new Placeholder("%target%", targetName)));
            return;
        }

        UUID targetUUID = dataManager.getPlayerUUID(targetName);

        CompletableFuture.supplyAsync(() -> dataManager.getDataFromDB(targetUUID.toString(), true))
                .thenAccept(targetData -> {
                    if (targetData != null && !targetData.isPrivateMessagesEnabled()
                            && !player.hasPermission("simplechat.messagesdisabled.bypass")) {
                        player.sendMessage(messageProvider.getMessage("General.Messages-Disabled",
                                new Placeholder("%player%", Bukkit.getOfflinePlayer(targetUUID).getName())));
                        return;
                    }

                    if (targetData != null && targetData.getIgnoredPlayers().contains(player.getUniqueId())
                            && !player.hasPermission("simplechat.ignored.bypass")) {
                        player.sendMessage(messageProvider.getMessage("General.Ignored",
                                new Placeholder("%player%", Bukkit.getOfflinePlayer(targetUUID).getName())));
                        return;
                    }

                    PrivateMessageFormat senderFormat = chatManager.getMessageFormat(player, true);
                    PrivateMessageFormat receiverFormat = chatManager.getMessageFormat(Bukkit.getOfflinePlayer(targetUUID), false);

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        PrivateMessageEvent privateMessageEvent = new PrivateMessageEvent(player.getUniqueId(), targetUUID,
                                Component.text(message), plugin.getRedisClient().getServerId());
                        Bukkit.getServer().getPluginManager().callEvent(privateMessageEvent);

                        if (privateMessageEvent.isCancelled()) {
                            return;
                        }

                        Component senderMessage = senderFormat.getFormat(player, Bukkit.getOfflinePlayer(targetUUID), privateMessageEvent.getMessage());

                        redisClient.sendListenerMessage(
                                new ListenerComponent(null, "private-message")
                                        .addData("sender", player.getUniqueId())
                                        .addData("targetUUID", targetUUID)
                                        .addData("rawMessage", serializer.serialize(privateMessageEvent.getMessage()))
                                        .addData("messageFormat", receiverFormat)
                        );

                        player.sendMessage(senderMessage);
                        playerData.setLastMessagedPlayer(targetUUID);
                        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> dataManager.save(playerData));
                    });
                }).exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
    }

}
