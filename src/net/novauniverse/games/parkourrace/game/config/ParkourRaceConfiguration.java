package net.novauniverse.games.parkourrace.game.config;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.json.JSONArray;
import org.json.JSONObject;

import net.zeeraa.novacore.spigot.gameengine.module.modules.game.Game;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.map.mapmodule.MapModule;
import net.zeeraa.novacore.spigot.utils.LocationUtils;
import net.zeeraa.novacore.spigot.utils.VectorArea;

public class ParkourRaceConfiguration extends MapModule {
	private List<Checkpoint> checkpoints;
	private Location spawnLocation;
	private VectorArea starterCageArea;
	private int startCountdown;
	private int laps;
	private int gameTime;

	public ParkourRaceConfiguration(JSONObject json) {
		super(json);

		checkpoints = new ArrayList<>();

		spawnLocation = LocationUtils.fromJSONObject(json.getJSONObject("spawn_location"), Bukkit.getServer().getWorlds().stream().findFirst().get());

		JSONArray checkpointListJSON = json.getJSONArray("checkpoints");
		for (int i = 0; i < checkpointListJSON.length(); i++) {
			JSONObject checkpointJSON = checkpointListJSON.getJSONObject(i);

			VectorArea unlockArea = VectorArea.fromJSON(checkpointJSON.getJSONObject("unlock_area"));
			Location spawnLocation = LocationUtils.fromJSONObject(checkpointJSON.getJSONObject("spawn_location"), Bukkit.getServer().getWorlds().stream().findFirst().get());
			int sequence = checkpointJSON.getInt("sequence");

			boolean ignoreInitialWarning = false;
			if (checkpointJSON.has("ignore_initial_warning")) {
				ignoreInitialWarning = checkpointJSON.getBoolean("ignore_initial_warning");
			}

			boolean isFinish = false;
			if (checkpointJSON.has("is_lap_finish")) {
				isFinish = checkpointJSON.getBoolean("is_lap_finish");
			}

			checkpoints.add(new Checkpoint(sequence, unlockArea, spawnLocation, ignoreInitialWarning, isFinish));
		}

		starterCageArea = VectorArea.fromJSON(json.getJSONObject("starter_cage"));
		startCountdown = json.getInt("start_countdown");
		laps = json.getInt("laps");
		gameTime = json.getInt("game_time");
	}

	@Override
	public void onGameStart(Game game) {
		checkpoints.forEach(c -> c.setWorld(game.getWorld()));
		spawnLocation.setWorld(game.getWorld());
	}

	public Location getSpawnLocation() {
		return spawnLocation;
	}

	public List<Checkpoint> getCheckpoints() {
		return checkpoints;
	}

	public VectorArea getStarterCageArea() {
		return starterCageArea;
	}

	public int getStartCountdown() {
		return startCountdown;
	}
	
	public int getLaps() {
		return laps;
	}
	
	public int getGameTime() {
		return gameTime;
	}
}