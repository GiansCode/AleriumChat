package io.alerium.aleriumchat.redis;

import io.alerium.aleriumchat.AleriumChat;
import io.alerium.aleriumchat.event.PrivateMessageEvent;
import io.alerium.aleriumchat.manager.ChatManager;
import io.alerium.aleriumchat.manager.format.PrivateMessageFormat;
import io.alerium.aleriumchat.playerdata.data.PlayerData;
import net.justugh.japi.database.redis.listener.ListenerComponent;
import net.justugh.japi.database.redis.listener.RedisMessageListener;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PrivateMessageRedisListener extends RedisMessageListener {

    private final ChatManager chatManager;

    public PrivateMessageRedisListener(ChatManager chatManager) {
        super("private-message");
        this.chatManager = chatManager;
        setSelfActivation(true);
    }

    @Override
    public void onReceive(ListenerComponent component) {
        UUID sender = component.getData("sender", UUID.class);
        UUID targetUUID = component.getData("targetUUID", UUID.class);
        String rawMessage = component.getData("rawMessage", String.class);
        PrivateMessageFormat messageFormat = component.getData("messageFormat", PrivateMessageFormat.class);

        Player target = Bukkit.getPlayer(targetUUID);

        if (target == null) {
            return;
        }

        Bukkit.getScheduler().runTask(AleriumChat.getInstance(), () -> {
            PrivateMessageEvent privateMessageEvent = new PrivateMessageEvent(sender, target.getUniqueId(), Component.text(rawMessage), component.getSource().getServerId());
            Bukkit.getServer().getPluginManager().callEvent(privateMessageEvent);

            if (privateMessageEvent.isCancelled()) {
                return;
            }

            PlayerData playerData = AleriumChat.getInstance().getPlayerDataManager().getDataById(targetUUID.toString());
            playerData.setLastMessagedPlayer(sender);
            Bukkit.getScheduler().runTaskAsynchronously(AleriumChat.getInstance(), () -> AleriumChat.getInstance().getPlayerDataManager().save(playerData));

            target.sendMessage(messageFormat.getFormat(Bukkit.getOfflinePlayer(sender), target, privateMessageEvent.getMessage()));
            chatManager.alertSocialSpy(sender, target.getUniqueId(), rawMessage);
        });
    }

}
