package io.alerium.aleriumchat.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.alerium.aleriumchat.AleriumChat;
import io.alerium.aleriumchat.manager.ChatManager;
import io.alerium.aleriumchat.manager.format.ChatFormat;
import net.justugh.japi.message.MessageProvider;
import net.justugh.japi.util.Placeholder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicInteger;

@CommandAlias("simplechat")
@CommandPermission("simplechat.command")
public class SimpleChatCommand extends BaseCommand {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Dependency
    private AleriumChat plugin;

    @Dependency
    private MessageProvider messageProvider;

    @Dependency
    private ChatManager chatManager;

    @Default
    public void onDefault(CommandSender sender) {
        onHelp(sender);
    }

    @Subcommand("help")
    @CommandPermission("simplechat.commands.help")
    public void onHelp(CommandSender sender) {
        sender.sendMessage(messageProvider.getMessageList("Simple-Chat.Help").toArray(new String[]{}));
    }

    @Subcommand("list")
    @CommandPermission("simplechat.commands.list")
    public void onList(Player player) {
        AtomicInteger index = new AtomicInteger(1);
        chatManager.getChatFormats().forEach(format -> {
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                    messageProvider.getMessage("Simple-Chat.List.Line-Format",
                            new Placeholder("%index%", index.getAndIncrement() + ""), new Placeholder("%id%", format.getId()),
                            new Placeholder("%format%", LegacyComponentSerializer.legacyAmpersand()
                                    .serialize(format.getFormat(player, Component.text("Hello!")))))));
        });
    }

    @Subcommand("test")
    @CommandPermission("simplechat.commands.test")
    @Syntax("<format> <message>")
    @CommandCompletion("@FormatIDs")
    public void onTest(Player player, String formatId, String message) {
        ChatFormat format = chatManager.getFormatByID(formatId);

        if (format == null) {
            player.sendMessage(messageProvider.getMessage("Simple-Chat.Test.Invalid-Format"));
            return;
        }

        player.sendMessage(format.getFormat(player, Component.text(message)));
    }

    @Subcommand("reload")
    @CommandPermission("simplechat.commands.reload")
    public void onReload(CommandSender sender) {
        plugin.loadConfigurationFiles();
        sender.sendMessage(messageProvider.getMessage("Simple-Chat.Reload.Successfully-Reloaded"));
    }

}
