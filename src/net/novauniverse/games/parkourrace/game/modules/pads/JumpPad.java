package net.novauniverse.games.parkourrace.game.modules.pads;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.json.JSONArray;
import org.json.JSONObject;

import net.novauniverse.games.parkourrace.game.ParkourRace;
import net.novauniverse.games.parkourrace.util.PlayerTeleportCallback;
import net.zeeraa.novacore.commons.log.Log;
import net.zeeraa.novacore.commons.tasks.Task;
import net.zeeraa.novacore.spigot.abstraction.enums.VersionIndependentSound;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.Game;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.map.mapmodule.MapModule;
import net.zeeraa.novacore.spigot.tasks.SimpleTask;

public class JumpPad extends MapModule implements Listener {
	private double yVelocity;
	private List<Material> materials;

	private List<UUID> inAir;

	private Task task;

	public JumpPad(JSONObject json) {
		super(json);

		this.task = null;
		this.materials = new ArrayList<Material>();
		this.inAir = new ArrayList<UUID>();

		this.yVelocity = json.getDouble("y_velocity");

		JSONArray materialsJson = json.getJSONArray("materials");
		for (int i = 0; i < materialsJson.length(); i++) {
			materials.add(Material.valueOf(materialsJson.getString(i)));
		}

		Log.debug("JumpPad", materials.size() + " materials loaded");
	}

	@Override
	public void onGameStart(Game game) {
		((ParkourRace) game).addTeleportCallback(new PlayerTeleportCallback() {
			@Override
			public void execute(Player player) {
				inAir.remove(player.getUniqueId());
			}
		});

		Bukkit.getServer().getPluginManager().registerEvents(this, game.getPlugin());

		task = new SimpleTask(game.getPlugin(), new BukkitRunnable() {
			@Override
			public void run() {
				Bukkit.getServer().getOnlinePlayers().stream().filter(p -> p.getGameMode() != GameMode.SPECTATOR).forEach(player -> {
					Material blockType = player.getLocation().clone().add(0, -1, 0).getBlock().getType();
					if (materials.contains(blockType)) {
						if (!inAir.contains(player.getUniqueId())) {
							VersionIndependentSound.WITHER_SHOOT.play(player);
							player.setVelocity(player.getVelocity().add(new Vector(0, yVelocity, 0)));
							inAir.add(player.getUniqueId());
						}
					}
				});
			}
		}, 2L);
		Task.tryStartTask(task);
	}

	@Override
	public void onGameEnd(Game game) {
		HandlerList.unregisterAll(this);
		Task.tryStopTask(task);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onEntityDamageEvent(EntityDamageEvent e) {
		if (e.getEntity() instanceof Player) {
			if (e.getCause() == DamageCause.FALL) {
				Player player = (Player) e.getEntity();
				if (inAir.contains(player.getUniqueId())) {
					e.setCancelled(true);
					inAir.remove(player.getUniqueId());
				}
			}
		}
	}
}