package io.alerium.aleriumchat.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ComponentUtil {

    public static Component translate(String text) {
        Component component = MiniMessage.miniMessage().deserialize(text);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(LegacyComponentSerializer.legacyAmpersand().serialize(component));
    }

    public static Component translate(Component component) {
        Component newComponent = MiniMessage.miniMessage().deserialize(MiniMessage.miniMessage().serialize(component));
        return LegacyComponentSerializer.legacyAmpersand().deserialize(LegacyComponentSerializer.legacyAmpersand().serialize(newComponent));
    }

}
