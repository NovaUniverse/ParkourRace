package net.novauniverse.games.parkourrace.game.modules.config;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import net.novauniverse.games.parkourrace.NovaParkourRace;
import net.novauniverse.games.parkourrace.game.data.ParkourRaceUtils;
import net.novauniverse.games.parkourrace.game.data.PlayerData;
import net.zeeraa.novacore.spigot.utils.LocationUtils;
import net.zeeraa.novacore.spigot.utils.VectorArea;
import xyz.xenondevs.particle.ParticleEffect;

public class Checkpoint {
	private int sequence;

	private VectorArea unlockArea;
	private Location spawnLocation;

	private boolean ignoreInitialWarning;
	private boolean lapFinish;

	public Checkpoint(int sequence, VectorArea unlockArea, Location spawnLocation, boolean ignoreInitialWarning, boolean lapFinish) {
		this.sequence = sequence;
		this.unlockArea = unlockArea;
		this.spawnLocation = spawnLocation;
		this.ignoreInitialWarning = ignoreInitialWarning;
		this.lapFinish = lapFinish;
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
		if (player.getWorld() == spawnLocation.getWorld()) {
			return unlockArea.isInsideBlock(player.getLocation().toVector());
		}
		return false;
	}

	public boolean isIgnoreInitialWarning() {
		return ignoreInitialWarning;
	}

	public boolean isLapFinish() {
		return lapFinish;
	}

	public void showParticles() {
		List<Vector> vectors = ParkourRaceUtils.getHollowCube(unlockArea.getPosition1(), unlockArea.getPosition2(), 0.5D);

		List<Player> spectators = new ArrayList<>();
		List<Player> completed = new ArrayList<>();
		List<Player> active = new ArrayList<>();
		List<Player> notReached = new ArrayList<>();

		Bukkit.getServer().getOnlinePlayers().forEach(player -> {
			if (player.getGameMode() == GameMode.SPECTATOR) {
				spectators.add(player);
			} else {
				PlayerData data = NovaParkourRace.getInstance().getGame().getPlayerData(player);
				if (data != null) {
					int pSeq = data.getSequence();

					if (pSeq == sequence - 1) {
						active.add(player);
					} else if (pSeq < sequence - 1) {
						notReached.add(player);
					} else {
						completed.add(player);
					}
				} else {
					// Player is not in game for some reason
					spectators.add(player);
				}
			}
		});

		vectors.forEach(vector -> {
			Location location = LocationUtils.getLocation(spawnLocation.getWorld(), vector);

			location.add(0.5, 0, 0.5);

			ParticleEffect.REDSTONE.display(location, Color.GREEN, spectators);

			ParticleEffect.REDSTONE.display(location, Color.BLUE, completed);
			ParticleEffect.REDSTONE.display(location, Color.GREEN, active);
			ParticleEffect.REDSTONE.display(location, Color.YELLOW, notReached);
		});
	}
}