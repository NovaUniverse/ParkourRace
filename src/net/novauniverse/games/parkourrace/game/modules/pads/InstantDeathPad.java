package net.novauniverse.games.parkourrace.game.modules.pads;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONArray;
import org.json.JSONObject;

import net.zeeraa.novacore.commons.log.Log;
import net.zeeraa.novacore.commons.tasks.Task;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.Game;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.map.mapmodule.MapModule;
import net.zeeraa.novacore.spigot.tasks.SimpleTask;

public class InstantDeathPad extends MapModule implements Listener {
	private List<Material> materials;

	private Task task;

	public InstantDeathPad(JSONObject json) {
		super(json);

		this.task = null;
		this.materials = new ArrayList<Material>();
		
		JSONArray materialsJson = json.getJSONArray("materials");
		for (int i = 0; i < materialsJson.length(); i++) {
			materials.add(Material.valueOf(materialsJson.getString(i)));
		}

		Log.debug("InstantDeathPad", materials.size() + " materials loaded");
	}

	@Override
	public void onGameStart(Game game) {
		task = new SimpleTask(game.getPlugin(), new BukkitRunnable() {
			@Override
			public void run() {
				Bukkit.getServer().getOnlinePlayers().stream().filter(p -> p.getGameMode() != GameMode.SPECTATOR).forEach(player -> {
					Material blockType = player.getLocation().clone().add(0, -1, 0).getBlock().getType();
					if (materials.contains(blockType)) {
						if (!player.isDead()) {
							player.damage(1000.0D);
						}
					}
				});
			}
		}, 2L);
		Task.tryStartTask(task);
	}

	@Override
	public void onGameEnd(Game game) {
		Task.tryStopTask(task);
	}
}