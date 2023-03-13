package io.alerium.aleriumchat.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import io.alerium.aleriumchat.AleriumChat;
import io.alerium.aleriumchat.playerdata.PlayerDataManager;
import io.alerium.aleriumchat.playerdata.data.PlayerData;
import net.justugh.japi.message.MessageProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandAlias("socialspy")
@CommandPermission("simplechat.commands.socialspy")
public class SocialSpyCommand extends BaseCommand {

    @Dependency
    private AleriumChat plugin;

    @Dependency
    private PlayerDataManager dataManager;

    @Dependency
    private MessageProvider messageProvider;

    @Default
    private void onDefault(Player player) {
        PlayerData playerData = dataManager.getDataById(player.getUniqueId().toString());

        if (playerData == null) {
            player.sendMessage(messageProvider.getMessage("General.Invalid-Data"));
            return;
        }

        playerData.setSocialSpyEnabled(!playerData.isSocialSpyEnabled());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> dataManager.save(playerData));
        player.sendMessage(playerData.isSocialSpyEnabled() ? messageProvider.getMessage("Social-Spy.Enabled")
                : messageProvider.getMessage("Social-Spy.Disabled"));
    }

}
