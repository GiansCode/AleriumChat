package io.alerium.aleriumchat.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
@Setter
public class SimpleChatEvent extends Event implements Cancellable {

    @Getter(AccessLevel.NONE)
    private final static HandlerList HANDLERS = new HandlerList();

    private final String sourceServer;
    private final UUID sender;

    private Component message;
    private boolean cancelled;

    public SimpleChatEvent(UUID sender, Component message, String sourceServer) {
        this.sender = sender;
        this.message = message;
        this.sourceServer = sourceServer;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
