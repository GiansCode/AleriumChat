package io.alerium.aleriumchat.manager.format;

import io.alerium.aleriumchat.AleriumChat;
import io.alerium.aleriumchat.manager.ChatManager;
import io.alerium.aleriumchat.util.ComponentUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.justugh.japi.JustAPIPlugin;
import net.justugh.japi.util.StringUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Getter
@AllArgsConstructor
public class ChatFormat {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private final String id;
    private final int priority;
    private final String chatColor;

    // Prefix Settings
    private final String prefixContent;
    private final List<String> prefixTooltip;
    private final List<String> prefixClickActions;

    // Name Settings
    private final String nameContent;
    private final List<String> nameTooltip;
    private final List<String> nameClickActions;

    // Suffix Settings
    private final String suffixContent;
    private final List<String> suffixTooltip;
    private final List<String> suffixClickActions;

    public Component getFormat(Player player, Component originalMessage) {
        Component chatMessage = originalMessage;
        Component message = Component.empty();

        if (player.hasPermission("simplechat.coloredchat")) {
            chatMessage = ComponentUtil.translate(chatMessage);
        }

        if (AleriumChat.getInstance().getConfig().getBoolean("Inject-Prefix-Chat")) {
            message = message.append(Component.text(JustAPIPlugin.getInstance().getRedisManager().getSourceID())).appendSpace();
        }

        message = message.append(getFormatComponent(player, prefixContent, prefixTooltip, prefixClickActions))
                .append(getFormatComponent(player, nameContent, nameTooltip, nameClickActions))
                .append(getFormatComponent(player, suffixContent, suffixTooltip, suffixClickActions))
                .appendSpace()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(chatColor).append(chatMessage));

        return message;
    }

    private Component getFormatComponent(Player player, String content, List<String> tooltip, List<String> clickActions) {
        ChatManager chatManager = AleriumChat.getInstance().getChatManager();

        AtomicReference<Component> component = new AtomicReference<>(ComponentUtil.translate(chatManager.applyPlaceholders(player.getUniqueId(), null, content)));
        AtomicReference<Component> prefixTooltip = new AtomicReference<>(Component.empty());
        for (int i = 0; i < tooltip.size(); i++) {
            prefixTooltip.set(prefixTooltip.get().append(ComponentUtil.translate(chatManager.applyPlaceholders(player.getUniqueId(), null, tooltip.get(i)))));

            if (i < tooltip.size() - 1) {
                prefixTooltip.set(prefixTooltip.get().appendNewline());
            }
        }
        component.set(component.get().hoverEvent(HoverEvent.showText(prefixTooltip.get())));
        List<String> prefixCommandActions = applyActions(player, component, clickActions);

        if (!prefixCommandActions.isEmpty()) {
            component.set(component.get().clickEvent(ClickEvent.runCommand("/clickchatcommands chat "
                    + chatManager.registerNewClickAction(prefixCommandActions))));
        }

        return component.get();
    }

    private List<String> applyActions(Player player, AtomicReference<Component> component, List<String> actions) {
        ChatManager chatManager = AleriumChat.getInstance().getChatManager();

        List<String> commandActions = new ArrayList<>();
        actions.forEach(clickAction -> {
            if (!clickAction.startsWith("[SUGGESTCOMMAND]") && !clickAction.startsWith("[OPENURL]")) {
                commandActions.add(chatManager.applyPlaceholders(player.getUniqueId(), null, clickAction));
                return;
            }

            String[] data = clickAction.split(" ");
            String identifier = data[0].replace("[", "").replace("]", "");
            String actionData = StringUtil.join(1, data, " ");

            if (identifier.equalsIgnoreCase("SUGGESTCOMMAND")) {
                component.set(component.get().clickEvent(ClickEvent.suggestCommand(chatManager.applyPlaceholders(player.getUniqueId(), null, actionData))));
            }

            if (identifier.equalsIgnoreCase("OPENURL")) {
                component.set(component.get().clickEvent(ClickEvent.openUrl(chatManager.applyPlaceholders(player.getUniqueId(), null, actionData))));
            }
        });

        return commandActions;
    }

}
