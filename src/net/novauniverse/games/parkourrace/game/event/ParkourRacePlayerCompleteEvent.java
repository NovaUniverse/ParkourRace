package net.novauniverse.games.parkourrace.game.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ParkourRacePlayerCompleteEvent extends Event {
	private static final HandlerList HANDLERS_LIST = new HandlerList();

	private Player player;
	private int plecement;

	public ParkourRacePlayerCompleteEvent(Player player, int plecement) {
		this.player = player;
		this.plecement = plecement;
	}

	public Player getPlayer() {
		return player;
	}

	public int getPlecement() {
		return plecement;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS_LIST;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS_LIST;
	}
}