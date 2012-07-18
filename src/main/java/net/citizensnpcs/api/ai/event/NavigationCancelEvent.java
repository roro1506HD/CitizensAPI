package net.citizensnpcs.api.ai.event;

import net.citizensnpcs.api.ai.Navigator;

import org.bukkit.event.HandlerList;

public class NavigationCancelEvent extends NavigationEvent {
    public NavigationCancelEvent(Navigator navigator) {
        super(navigator);
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
