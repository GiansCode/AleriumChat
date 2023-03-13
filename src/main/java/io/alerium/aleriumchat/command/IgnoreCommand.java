package io.alerium.aleriumchat.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.alerium.aleriumchat.AleriumChat;
import io.alerium.aleriumchat.playerdata.PlayerDataManager;
import io.alerium.aleriumchat.playerdata.data.PlayerData;
import net.justugh.japi.message.MessageProvider;
import net.justugh.japi.util.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

@CommandAlias("ignore")
@CommandPermission("simplechat.commands.ignore")
public class IgnoreCommand extends BaseCommand {

    @Dependency
    private AleriumChat plugin;

    @Dependency
    private PlayerDataManager dataManager;

    @Dependency
    private MessageProvider messageProvider;

    @Default
    @Syntax("<player>")
    public void onDefault(Player player, @Optional String playerName) {
        if (playerName == null) {
            player.sendMessage(messageProvider.getMessage("Ignore.Usage"));
            return;
        }

        PlayerData playerData = dataManager.getDataById(player.getUniqueId().toString());

        if (playerData == null) {
            player.sendMessage(messageProvider.getMessage("General.Invalid-Data"));
            return;
        }

        CompletableFuture.supplyAsync(() -> Bukkit.getOfflinePlayer(playerName)).thenAccept(target -> {
            if (playerData.getIgnoredPlayers().contains(target.getUniqueId())) {
                playerData.getIgnoredPlayers().remove(target.getUniqueId());
                dataManager.save(playerData);
                player.sendMessage(messageProvider.getMessage("Ignore.Removed", new Placeholder("%player%", target.getName())));
                return;
            }

            playerData.getIgnoredPlayers().add(target.getUniqueId());
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> dataManager.save(playerData));
            player.sendMessage(messageProvider.getMessage("Ignore.Added", new Placeholder("%player%", target.getName())));
        }).exceptionally(e -> {
            e.printStackTrace();
            return null;
        });
    }

}
