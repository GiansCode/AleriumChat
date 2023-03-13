package io.alerium.aleriumchat.listener;

import io.alerium.aleriumchat.AleriumChat;
import io.alerium.aleriumchat.playerdata.PlayerDataManager;
import io.alerium.aleriumchat.playerdata.data.PlayerData;
import io.alerium.aleriumchat.redis.OnlineStatusRedisListener;
import net.justugh.japi.database.redis.listener.ListenerComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class PlayerDataListener implements Listener {

    private final AleriumChat plugin;
    private final PlayerDataManager playerDataManager;

    public PlayerDataListener(AleriumChat plugin) {
        this.plugin = plugin;
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    @EventHandler(ignoreCancelled = true)
    public void onLogin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerDataManager.getOnlinePlayers().put(player.getName(), player.getUniqueId());
        plugin.getRedisClient().sendListenerMessage(new ListenerComponent(null, "online-status")
                .addData("action", OnlineStatusRedisListener.StatusAction.JOIN)
                .addData("playerName", player.getName()).addData("playerUUID", player.getUniqueId()));

        CompletableFuture.supplyAsync(() -> playerDataManager.getDataFromDB(player.getUniqueId().toString(), true))
                .thenAccept(playerData -> {
                    if (playerData == null) {
                        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                            boolean socialSpyEnabled = AleriumChat.getInstance().getConfig().getBoolean("Social-Spy.On-By-Default")
                                    && player.hasPermission("simplechat.socialspy");

                            PlayerData newData = new PlayerData(player.getUniqueId(), true,
                                    socialSpyEnabled, null, new ArrayList<>());
                            playerDataManager.getData().add(newData);
                            playerDataManager.save(newData);
                        });
                        return;
                    }

                    playerDataManager.getData().add(playerData);
                }).exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        playerDataManager.getData().removeIf(data -> data.getUuid().equals(player.getUniqueId()));
        playerDataManager.getOnlinePlayers().remove(player.getName());
        plugin.getRedisClient().sendListenerMessage(new ListenerComponent(null, "online-status")
                .addData("action", OnlineStatusRedisListener.StatusAction.QUIT)
                .addData("playerName", player.getName()).addData("playerUUID", player.getUniqueId()));
    }

}
