package net.novauniverse.games.parkourrace.game.config;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import net.zeeraa.novacore.spigot.utils.VectorArea;

public class Checkpoint {
	private int sequence;
	private VectorArea unlockArea;
	private Location spawnLocation;

	public Checkpoint(int sequence, VectorArea unlockArea, Location spawnLocation) {
		this.sequence = sequence;
		this.unlockArea = unlockArea;
		this.spawnLocation = spawnLocation;
	}

	public void setWorld(World world) {
		this.spawnLocation.setWorld(world);
	}
	
	public int getSequence() {
		return sequence;
	}
	
	public VectorArea getUnlockArea() {
		return unlockArea;
	}
	
	public Location getSpawnLocation() {
		return spawnLocation;
	}
	
	public boolean isInside(Player player) {
		if(player.getWorld() == spawnLocation.getWorld()) {
			return unlockArea.isInsideBlock(player.getLocation().toVector());
		}
		return false;
	}
}