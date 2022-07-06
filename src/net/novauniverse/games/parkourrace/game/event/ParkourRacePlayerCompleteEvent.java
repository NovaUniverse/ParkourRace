package net.novauniverse.games.parkourrace.game.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ParkourRacePlayerCompleteEvent extends Event {
	private static final HandlerList HANDLERS_LIST = new HandlerList();

	private Player player;
	private int lapNumber;

	public ParkourRacePlayerCompleteEvent(Player player, int lapNumber) {
		this.player = player;
		this.lapNumber = lapNumber;
	}

	public Player getPlayer() {
		return player;
	}

	public int getLapNumber() {
		return lapNumber;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS_LIST;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS_LIST;
	}
}