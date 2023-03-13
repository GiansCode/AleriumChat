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

@CommandAlias("toggleprivatemessages|tdm|tpm")
@CommandPermission("simplechat.commands.toggleprivatemessages")
public class TogglePrivateMessagesCommand extends BaseCommand {

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

        playerData.setPrivateMessagesEnabled(!playerData.isPrivateMessagesEnabled());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> dataManager.save(playerData));
        player.sendMessage(playerData.isPrivateMessagesEnabled() ? messageProvider.getMessage("Toggle-Private-Messages.Enabled")
                : messageProvider.getMessage("Toggle-Private-Messages.Disabled"));
    }

}
