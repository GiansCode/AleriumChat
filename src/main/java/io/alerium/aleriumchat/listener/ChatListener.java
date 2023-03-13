package io.alerium.aleriumchat.listener;

import io.alerium.aleriumchat.AleriumChat;
import io.alerium.aleriumchat.event.SimpleChatEvent;
import io.alerium.aleriumchat.manager.ChatManager;
import io.alerium.aleriumchat.manager.format.ChatFormat;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.justugh.japi.JustAPIPlugin;
import net.justugh.japi.database.redis.listener.ListenerComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.logging.Level;

public class ChatListener implements Listener {

    private final AleriumChat plugin;
    private final ChatManager chatManager;
    private final MiniMessage miniMessage;

    public ChatListener(AleriumChat plugin) {
        this.plugin = plugin;
        this.chatManager = plugin.getChatManager();
        this.miniMessage = MiniMessage.miniMessage();
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        ChatFormat playerFormat = chatManager.getFormat(player);

        event.setCancelled(true);

        if (playerFormat == null) {
            AleriumChat.getInstance().getLogger().log(Level.SEVERE, "Unable to locate format for {0}", player.getName());
            AleriumChat.getInstance().getLogger().log(Level.INFO, "To prevent this, ensure a 'default' format is setup and that they have the default group.");
            return;
        }

        if (chatManager.isChatDisabled() && !player.hasPermission("simplechat.disabled.bypass")) {
            player.sendMessage(plugin.getMessageProvider().getMessage("General.Chat-Disabled"));
            return;
        }

        Component message = playerFormat.getFormat(player, event.message());

        if (!plugin.getConfig().getBoolean("Enable-Cross-Server-Chat")) {
            SimpleChatEvent chatEvent = new SimpleChatEvent(player.getUniqueId(), message, JustAPIPlugin.getInstance().getRedisManager().getSourceID());
            Bukkit.getServer().getPluginManager().callEvent(chatEvent);

            if (chatEvent.isCancelled()) {
                return;
            }

            Bukkit.broadcast(chatEvent.getMessage());
            return;
        }

        plugin.getRedisClient().sendListenerMessage(new ListenerComponent(null, "chat-message")
                .addData("sender", player.getUniqueId()).addData("message", miniMessage.serialize(message)));
    }

}
