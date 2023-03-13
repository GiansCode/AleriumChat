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

/**
 * Called when a player successfully sends another player
 * a private message.
 */
@Getter
public class PrivateMessageEvent extends Event implements Cancellable {

    @Getter(AccessLevel.NONE)
    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID sender;
    private final UUID receiver;
    private final String sourceServer;

    @Setter
    private Component message;

    @Setter
    private boolean cancelled;

    public PrivateMessageEvent(UUID sender, UUID receiver, Component message, String sourceServer) {
        this.sender = sender;
        this.receiver = receiver;
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

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

}
