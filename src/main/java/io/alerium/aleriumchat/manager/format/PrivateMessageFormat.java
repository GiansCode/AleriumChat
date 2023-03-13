package io.alerium.aleriumchat.manager.format;

import io.alerium.aleriumchat.AleriumChat;
import io.alerium.aleriumchat.manager.ChatManager;
import io.alerium.aleriumchat.util.ComponentUtil;
import lombok.Getter;
import net.justugh.japi.util.StringUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Getter
public class PrivateMessageFormat {

    private transient MiniMessage miniMessage;

    private final String id;
    private final int priority;
    private final String format;

    private final List<String> tooltip;
    private final List<String> clickActions;

    public PrivateMessageFormat(String id, int priority, String format, List<String> tooltip, List<String> clickActions) {
        this.id = id;
        this.priority = priority;
        this.format = format;
        this.tooltip = tooltip;
        this.clickActions = clickActions;

        this.miniMessage = MiniMessage.miniMessage();
    }

    public Component getFormat(OfflinePlayer player, OfflinePlayer target, Component originalMessage) {
        this.miniMessage = MiniMessage.miniMessage();

        Component chatMessage = originalMessage;

        if (AleriumChat.getInstance().getVaultPermissionHook().playerHas(null, player, "simplechat.coloredpms")) {
            chatMessage = ComponentUtil.translate(originalMessage);
        }

        return getFormatComponent(player, target, format, tooltip, clickActions, chatMessage);
    }

    private Component getFormatComponent(OfflinePlayer player, OfflinePlayer target, String content, List<String> tooltip, List<String> clickActions, Component message) {
        ChatManager chatManager = AleriumChat.getInstance().getChatManager();

        AtomicReference<Component> component = new AtomicReference<>(ComponentUtil.translate(chatManager.applyPlaceholders(player.getUniqueId(), target.getUniqueId(),
                content.replace("%message%", miniMessage.serialize(message)))));
        AtomicReference<Component> prefixTooltip = new AtomicReference<>(Component.empty());
        for (int i = 0; i < tooltip.size(); i++) {
            prefixTooltip.set(prefixTooltip.get().append(ComponentUtil.translate(chatManager.applyPlaceholders(player.getUniqueId(), target.getUniqueId(), tooltip.get(i)))));

            if (i < tooltip.size() - 1) {
                prefixTooltip.set(prefixTooltip.get().appendNewline());
            }
        }
        component.set(component.get().hoverEvent(HoverEvent.showText(prefixTooltip.get())));
        List<String> prefixCommandActions = applyActions(player, target, component, clickActions);

        if (!prefixCommandActions.isEmpty()) {
            component.set(component.get().clickEvent(ClickEvent.runCommand("/clickchatcommands private "
                    + AleriumChat.getInstance().getPlayerDataManager().registerNewClickAction(player, prefixCommandActions))));
        }

        return component.get();
    }

    private List<String> applyActions(OfflinePlayer player, OfflinePlayer target, AtomicReference<Component> component, List<String> actions) {
        ChatManager chatManager = AleriumChat.getInstance().getChatManager();

        List<String> commandActions = new ArrayList<>();
        actions.forEach(clickAction -> {
            if (!clickAction.startsWith("[SUGGESTCOMMAND]") && !clickAction.startsWith("[OPENURL]")) {
                commandActions.add(chatManager.applyPlaceholders(player.getUniqueId(), target.getUniqueId(), clickAction));
                return;
            }

            String[] data = clickAction.split(" ");
            String identifier = data[0].replace("[", "").replace("]", "");
            String actionData = StringUtil.join(1, data, " ");

            if (identifier.equalsIgnoreCase("SUGGESTCOMMAND")) {
                component.set(component.get().clickEvent(ClickEvent.suggestCommand(chatManager.applyPlaceholders(player.getUniqueId(), target.getUniqueId(), actionData))));
            }

            if (identifier.equalsIgnoreCase("OPENURL")) {
                component.set(component.get().clickEvent(ClickEvent.openUrl(chatManager.applyPlaceholders(player.getUniqueId(), target.getUniqueId(), actionData))));
            }
        });

        return commandActions;
    }

}
