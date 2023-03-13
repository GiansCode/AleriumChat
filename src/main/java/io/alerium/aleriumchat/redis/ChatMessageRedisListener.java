package io.alerium.aleriumchat.redis;

import io.alerium.aleriumchat.AleriumChat;
import io.alerium.aleriumchat.event.SimpleChatEvent;
import net.justugh.japi.JustAPIPlugin;
import net.justugh.japi.database.redis.listener.ListenerComponent;
import net.justugh.japi.database.redis.listener.RedisMessageListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.UUID;

public class ChatMessageRedisListener extends RedisMessageListener {

    private final AleriumChat plugin;

    public ChatMessageRedisListener(AleriumChat plugin) {
        super("chat-message");
        this.plugin = plugin;
        setSelfActivation(true);
    }

    @Override
    public void onReceive(ListenerComponent component) {
        UUID sender = component.getData("sender", UUID.class);
        Component message = MiniMessage.miniMessage().deserialize(component.getData("message", String.class));

        String serverId = JustAPIPlugin.getInstance().getRedisManager().getSourceID();
        List<String> whitelistedServers = plugin.getConfig().getStringList("Whitelisted-Servers");

        if (plugin.getConfig().getBoolean("Use-Whitelist-As-Blacklist")) {
            if (whitelistedServers.contains(serverId)) {
                return;
            }
        } else {
            if (!whitelistedServers.contains(serverId)) {
                return;
            }
        }

        if (AleriumChat.getInstance().getChatManager().isChatDisabled() &&
                !plugin.getVaultPermissionHook().playerHas(null, Bukkit.getOfflinePlayer(sender), "simplechat.disabled.bypass")) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            SimpleChatEvent chatEvent = new SimpleChatEvent(sender, message, component.getSource().getServerId());
            Bukkit.getServer().getPluginManager().callEvent(chatEvent);

            if (chatEvent.isCancelled()) {
                return;
            }

            Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(chatEvent.getMessage()));
        });
    }

}
