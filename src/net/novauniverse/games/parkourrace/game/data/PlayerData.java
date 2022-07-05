package net.novauniverse.games.parkourrace.game.data;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import net.novauniverse.games.parkourrace.NovaParkourRace;
import net.novauniverse.games.parkourrace.game.config.Checkpoint;

public class PlayerData {
	private UUID uuid;
	private int sequence;
	private int lastSequenceWarning;
	private boolean completed;

	public PlayerData(UUID uuid) {
		this.uuid = uuid;
		this.sequence = 0;
		this.lastSequenceWarning = 0;
		this.completed = false;
	}

	public UUID getUuid() {
		return uuid;
	}

	public int getSequence() {
		return sequence;
	}

	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	public Location getRespawnLocation() {
		Checkpoint checkpoint = NovaParkourRace.getInstance().getGame().getConfig().getCheckpoints().stream().filter(c -> c.getSequence() == sequence).findFirst().orElse(null);
		if (checkpoint != null) {
			return checkpoint.getSpawnLocation();
		}

		return NovaParkourRace.getInstance().getGame().getConfig().getSpawnLocation();
	}

	public boolean isOnline() {
		Player player = Bukkit.getServer().getPlayer(uuid);
		if (player != null) {
			return player.isOnline();
		}
		return false;
	}

	public Checkpoint getNextCheckpoint() {
		return NovaParkourRace.getInstance().getGame().getConfig().getCheckpoints().stream().filter(c -> c.getSequence() == sequence + 1).findFirst().orElse(null);
	}

	public boolean isOwnedBy(Player player) {
		return player.getUniqueId().equals(uuid);
	}

	public Player getPlayer() {
		return Bukkit.getServer().getPlayer(uuid);
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	public boolean isCompleted() {
		return completed;
	}

	public int getLastSequenceWarning() {
		return lastSequenceWarning;
	}

	public void setLastSequenceWarning(int lastSequenceWarning) {
		this.lastSequenceWarning = lastSequenceWarning;
	}
}