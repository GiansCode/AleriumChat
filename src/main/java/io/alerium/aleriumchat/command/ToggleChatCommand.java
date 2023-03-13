package io.alerium.aleriumchat.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import io.alerium.aleriumchat.manager.ChatManager;
import net.justugh.japi.message.MessageProvider;
import net.justugh.japi.util.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

@CommandAlias("togglechat")
@CommandPermission("simplechat.commands.togglechat")
public class ToggleChatCommand extends BaseCommand {

    @Dependency
    private ChatManager chatManager;

    @Dependency
    private MessageProvider messageProvider;

    @Default
    public void onDefault(CommandSender sender) {
        chatManager.setChatDisabled(!chatManager.isChatDisabled());
        sender.sendMessage(chatManager.isChatDisabled() ? messageProvider.getMessage("Toggle-Chat.Disabled")
                : messageProvider.getMessage("Toggle-Chat.Enabled"));

        Placeholder playerPlaceholder = new Placeholder("%player%", sender.getName());
        Bukkit.broadcastMessage(chatManager.isChatDisabled() ? messageProvider.getMessage("Toggle-Chat.Disabled-Broadcast", playerPlaceholder)
                : messageProvider.getMessage("Toggle-Chat.Enabled-Broadcast", playerPlaceholder));
    }

}
