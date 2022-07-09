package net.novauniverse.games.parkourrace.game;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatColor;
import net.novauniverse.games.parkourrace.game.data.PlayerData;
import net.novauniverse.games.parkourrace.game.event.ParkourRacePlayerCompleteEvent;
import net.novauniverse.games.parkourrace.game.event.ParkourRacePlayerCompleteLapEvent;
import net.novauniverse.games.parkourrace.game.event.ParkourRacePlayerReachCheckpointEvent;
import net.novauniverse.games.parkourrace.game.modules.config.Checkpoint;
import net.novauniverse.games.parkourrace.game.modules.config.ParkourRaceConfiguration;
import net.novauniverse.games.parkourrace.util.PlayerTeleportCallback;
import net.zeeraa.novacore.commons.log.Log;
import net.zeeraa.novacore.commons.tasks.Task;
import net.zeeraa.novacore.commons.utils.Callback;
import net.zeeraa.novacore.commons.utils.TextUtils;
import net.zeeraa.novacore.spigot.abstraction.VersionIndependentUtils;
import net.zeeraa.novacore.spigot.abstraction.enums.VersionIndependentSound;
import net.zeeraa.novacore.spigot.abstraction.events.VersionIndependentPlayerAchievementAwardedEvent;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.GameEndReason;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.MapGame;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.elimination.PlayerQuitEliminationAction;
import net.zeeraa.novacore.spigot.module.ModuleManager;
import net.zeeraa.novacore.spigot.module.modules.compass.CompassTracker;
import net.zeeraa.novacore.spigot.tasks.SimpleTask;
import net.zeeraa.novacore.spigot.teams.Team;
import net.zeeraa.novacore.spigot.teams.TeamManager;
import net.zeeraa.novacore.spigot.utils.ChatColorRGBMapper;
import net.zeeraa.novacore.spigot.utils.ItemBuilder;
import net.zeeraa.novacore.spigot.utils.PlayerUtils;
import net.zeeraa.novacore.spigot.utils.RandomFireworkEffect;
import net.zeeraa.novacore.spigot.utils.VectorArea;

public class ParkourRace extends MapGame implements Listener {
	public static final int ENDERPEARL_SLOT = 0;
	public static final int COMPASS_SLOT = 8;

	private boolean started;
	private boolean ended;

	private Task foodAndLapTask;
	private Task checkTask;
	private Task compassTask;
	private Task particleTask;
	private Task startCountdownTask;
	private Task gameCountdownTimer;

	private int timeLeft;
	private int placementCounter;
	private int startCountdown;

	private ParkourRaceConfiguration config;

	private List<PlayerData> playerDataList;

	private List<Callback> timerDecrementCallbacks;
	private List<PlayerTeleportCallback> teleportCallbacks;

	public ParkourRace(Plugin plugin) {
		super(plugin);

		this.started = false;
		this.ended = false;

		this.startCountdown = 0;
		this.placementCounter = 1;
		this.timeLeft = 0;

		this.playerDataList = new ArrayList<>();
		this.timerDecrementCallbacks = new ArrayList<>();
		this.teleportCallbacks = new ArrayList<>();

		this.gameCountdownTimer = new SimpleTask(plugin, new BukkitRunnable() {
			@Override
			public void run() {
				if (timeLeft > 0) {
					if (timeLeft == 30 || timeLeft == 60) {
						Bukkit.getServer().broadcastMessage(ChatColor.RED + "" + timeLeft + " seconds left " + TextUtils.ICON_WARNING);
						Bukkit.getServer().getOnlinePlayers().forEach(player -> {
							VersionIndependentSound.NOTE_PLING.play(player);
							VersionIndependentUtils.get().sendTitle(player, "", ChatColor.RED + TextUtils.ICON_WARNING + " " + timeLeft + " seconds left " + TextUtils.ICON_WARNING, 0, 40, 10);
						});
					}

					if (timeLeft <= 10) {
						Bukkit.getServer().getOnlinePlayers().forEach(player -> {
							VersionIndependentSound.NOTE_PLING.play(player);
							VersionIndependentUtils.get().sendTitle(player, "", ChatColor.RED + TextUtils.ICON_WARNING + " " + timeLeft + " second" + (timeLeft == 1 ? "" : "s") + " left " + TextUtils.ICON_WARNING, 0, 20, 10);
						});
					}

					timeLeft--;
				} else {
					endGame(GameEndReason.TIME);
				}
				timerDecrementCallbacks.forEach(callback -> callback.execute());
			}
		}, 20L);

		this.foodAndLapTask = new SimpleTask(plugin, new Runnable() {
			@Override
			public void run() {
				playerDataList.stream().filter(p -> p.isOnline() && !p.isCompleted()).forEach(pd -> {
					VersionIndependentUtils.get().sendActionBarMessage(pd.getPlayer(), ChatColor.GREEN + "Lap " + pd.getLap());
				});

				Bukkit.getServer().getOnlinePlayers().forEach(player -> {
					player.setFoodLevel(20);
					player.setSaturation(20.0F);
				});
			}
		}, 10L);

		this.particleTask = new SimpleTask(plugin, new Runnable() {
			@Override
			public void run() {
				config.getCheckpoints().forEach(c -> c.showParticles());
			}
		}, 5L);

		this.startCountdownTask = new SimpleTask(plugin, new Runnable() {
			@Override
			public void run() {
				if (startCountdown > 0) {
					VersionIndependentSound.NOTE_PLING.broadcast(1.0F, 1.0F);
					Bukkit.getServer().getOnlinePlayers().forEach(player -> VersionIndependentUtils.get().sendTitle(player, ChatColor.GREEN + "Starting in " + startCountdown, "", 0, 25, 0));
					startCountdown--;
				} else {
					VersionIndependentSound.NOTE_PLING.broadcast(1.0F, 1.25F);
					Bukkit.getServer().getOnlinePlayers().forEach(player -> VersionIndependentUtils.get().sendTitle(player, ChatColor.GREEN + "GO", "", 0, 20, 15));
					Task.tryStopTask(startCountdownTask);

					VectorArea cage = config.getStarterCageArea();
					for (int x = cage.getPosition1().getBlockX(); x <= cage.getPosition2().getBlockX(); x++) {
						for (int y = cage.getPosition1().getBlockY(); y <= cage.getPosition2().getBlockY(); y++) {
							for (int z = cage.getPosition1().getBlockZ(); z <= cage.getPosition2().getBlockZ(); z++) {
								Location location = new Location(getWorld(), x, y, z);
								location.getBlock().setType(Material.AIR);
							}
						}
					}

					sendBeginEvent();
				}
			}
		}, 20L);

		this.checkTask = new SimpleTask(plugin, new Runnable() {
			@Override
			public void run() {
				playerDataList.stream().filter(d -> !d.isCompleted()).filter(d -> d.isOnline()).forEach(playerData -> {
					Player player = playerData.getPlayer();

					if (player.getGameMode() == GameMode.SPECTATOR) {
						return;
					}

					Checkpoint checkpoint = config.getCheckpoints().stream().filter(c -> c.isInside(player)).findFirst().orElse(null);
					if (checkpoint != null) {
						int cSequence = checkpoint.getSequence();
						int pSequence = playerData.getSequence();

						if (cSequence <= pSequence) {
							// Player has already passed the checkpoint
							return;
						}

						if (cSequence - 1 > pSequence) {
							// Player has not passed the previous checkpoint and will receive a warning
							if (playerData.getLastSequenceWarning() != cSequence) {
								if (pSequence == 0 && checkpoint.isIgnoreInitialWarning()) {
									return;
								}

								VersionIndependentSound.WITHER_HURT.play(player);
								player.sendMessage(ChatColor.RED + "You missed a checkpoint. Right click with the enderpearl to go back to your last checkpoint");
								playerData.setLastSequenceWarning(cSequence);
							}
							return;
						}

						if (cSequence - 1 == pSequence) {
							// Player should unlock checkpoint / lap
							if (checkpoint.isLapFinish()) {
								VersionIndependentSound.LEVEL_UP.play(player);
								if (playerData.getLap() >= config.getLaps()) {
									playerData.setCompleted(true);
									players.remove(playerData.getUuid());
									player.setGameMode(GameMode.SPECTATOR);
									PlayerUtils.clearPlayerInventory(player);
									PlayerUtils.clearPotionEffects(player);

									ChatColor color = ChatColor.AQUA;
									if (TeamManager.hasTeamManager()) {
										Team team = TeamManager.getTeamManager().getPlayerTeam(player);
										if (team != null) {
											color = team.getTeamColor();
										}
									}

									Bukkit.getServer().broadcastMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Player Completed> " + color + ChatColor.BOLD + player.getName() + ChatColor.GREEN + ChatColor.BOLD + " completed all " + config.getLaps() + " laps in " + TextUtils.ordinal(placementCounter) + " place");
									VersionIndependentUtils.get().sendTitle(player, ChatColor.GREEN + "" + ChatColor.BOLD + TextUtils.ordinal(placementCounter) + " place", "", 10, 60, 10);

									Firework fw = (Firework) getConfig().getSpawnLocation().getWorld().spawnEntity(getConfig().getSpawnLocation(), EntityType.FIREWORK);
									FireworkMeta fwm = fw.getFireworkMeta();

									fwm.setPower(3);
									fwm.addEffect(RandomFireworkEffect.randomFireworkEffect());

									if (random.nextBoolean()) {
										fwm.addEffect(RandomFireworkEffect.randomFireworkEffect());
									}

									fw.setFireworkMeta(fwm);

									Event event = new ParkourRacePlayerCompleteEvent(player, playerData.getLap());
									Bukkit.getServer().getPluginManager().callEvent(event);

									placementCounter++;
								} else {
									playerData.incrementLap();
									playerData.setSequence(0);
									VersionIndependentSound.LEVEL_UP.play(player);
									player.sendMessage(ChatColor.GREEN + "Lap " + playerData.getLap());
									VersionIndependentUtils.get().sendTitle(player, "", ChatColor.GREEN + "Lap " + playerData.getLap(), 10, 20, 10);
									Event event = new ParkourRacePlayerCompleteLapEvent(player, playerData.getLap());
									Bukkit.getServer().getPluginManager().callEvent(event);
								}
							} else {
								playerData.setSequence(cSequence);
								VersionIndependentSound.ORB_PICKUP.play(player);
								player.sendMessage(ChatColor.GREEN + "Checkpoint reached");
								VersionIndependentUtils.get().sendTitle(player, "", ChatColor.GREEN + "Checkpoint", 10, 20, 10);
								Event event = new ParkourRacePlayerReachCheckpointEvent(player);
								Bukkit.getServer().getPluginManager().callEvent(event);
							}
						}
					}
				});

				if (players.size() == 0) {
					endGame(GameEndReason.ALL_FINISHED);
				}
			}
		}, 0L);

		this.compassTask = new SimpleTask(plugin, new Runnable() {
			@Override
			public void run() {
				playerDataList.stream().filter(d -> d.isOnline()).forEach(d -> {
					Checkpoint checkpoint = d.getNextCheckpoint();
					if (checkpoint != null) {
						d.getPlayer().setCompassTarget(checkpoint.getSpawnLocation());

					}
				});
			}
		}, 5L);
	}

	public void addTimerDecrementCallback(Callback callback) {
		timerDecrementCallbacks.add(callback);
	}

	public void addTeleportCallback(PlayerTeleportCallback callback) {
		teleportCallbacks.add(callback);
	}

	public void setupPlayerData(Player player) {
		if (playerDataList.stream().filter(pd -> pd.isOwnedBy(player)).count() == 0) {
			playerDataList.add(new PlayerData(player.getUniqueId()));
		}
	}

	@Override
	public void tpToSpectator(Player player) {
		PlayerUtils.clearPlayerInventory(player);
		PlayerUtils.clearPotionEffects(player);
		if (config.isNightvision()) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, true));
		}
		player.setGameMode(GameMode.SPECTATOR);
		player.teleport(getActiveMap().getSpectatorLocation());
	}

	public void teleportPlayer(Player player) {
		PlayerData playerData = playerDataList.stream().filter(pd -> pd.isOwnedBy(player)).findFirst().orElse(null);
		if (playerData == null) {
			Log.warn("ParkourRace", "Cant spawn " + player.getName() + " since they dont have any player data");
			return;
		}

		PlayerUtils.clearPlayerInventory(player);

		player.teleport(playerData.getRespawnLocation());
		player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true, true));
		if (config.isNightvision()) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, true));
		}
		player.setGameMode(GameMode.ADVENTURE);
		player.setFireTicks(0);

		ChatColor color = ChatColor.AQUA;
		if (TeamManager.hasTeamManager()) {
			Team team = TeamManager.getTeamManager().getPlayerTeam(player);
			if (team != null) {
				color = team.getTeamColor();
			}
		}

		PlayerUtils.setMaxHealth(player, 2.0D);

		ItemBuilder compassBuilder = new ItemBuilder(Material.COMPASS);
		compassBuilder.setAmount(1);
		compassBuilder.setName(ChatColor.GOLD + "Next checkpoint");
		player.getInventory().setItem(COMPASS_SLOT, compassBuilder.build());

		ItemBuilder returnItemBuilder = new ItemBuilder(Material.ENDER_PEARL);
		returnItemBuilder.setAmount(1);
		returnItemBuilder.setName(ChatColor.RED + "Respawn");
		player.getInventory().setItem(ENDERPEARL_SLOT, returnItemBuilder.build());

		ItemBuilder bootsBuilder = new ItemBuilder(Material.LEATHER_BOOTS);
		bootsBuilder.setAmount(1);
		bootsBuilder.setUnbreakable(true);
		bootsBuilder.setLeatherArmorColor(ChatColorRGBMapper.chatColorToRGBColorData(color).toBukkitColor());
		player.getInventory().setBoots(bootsBuilder.build());

		teleportCallbacks.forEach(c -> c.execute(player));
	}

	@Override
	public String getName() {
		return "parkour_race";
	}

	@Override
	public String getDisplayName() {
		return "Parkour Race";
	}

	@Override
	public PlayerQuitEliminationAction getPlayerQuitEliminationAction() {
		return PlayerQuitEliminationAction.DELAYED;
	}

	@Override
	public boolean eliminatePlayerOnDeath(Player player) {
		return false;
	}

	@Override
	public boolean isPVPEnabled() {
		return false;
	}

	@Override
	public boolean autoEndGame() {
		return false;
	}

	@Override
	public boolean hasStarted() {
		return started;
	}

	@Override
	public boolean hasEnded() {
		return ended;
	}

	@Override
	public boolean isFriendlyFireAllowed() {
		return false;
	}

	@Override
	public boolean canAttack(LivingEntity attacker, LivingEntity target) {
		return false;
	}

	@Override
	public void onStart() {
		if (started) {
			return;
		}

		config = (ParkourRaceConfiguration) this.getActiveMap().getMapData().getMapModule(ParkourRaceConfiguration.class);
		if (config == null) {
			Bukkit.getServer().broadcastMessage(ChatColor.DARK_RED + "Failed to start: Configuration error");
			Log.fatal("ParkourRace", "Failed to start. Missing map module parkourrace.config");
			return;
		}

		startCountdown = config.getStartCountdown();
		timeLeft = config.getGameTime();

		Bukkit.getServer().getOnlinePlayers().stream().filter(p -> players.contains(p.getUniqueId())).forEach(p -> setupPlayerData(p));

		Task.tryStartTask(checkTask);
		Task.tryStartTask(compassTask);
		Task.tryStartTask(particleTask);
		Task.tryStartTask(foodAndLapTask);
		Task.tryStartTask(gameCountdownTimer);

		Bukkit.getServer().getOnlinePlayers().stream().filter(p -> players.contains(p.getUniqueId())).forEach(p -> teleportPlayer(p));
		Bukkit.getServer().getOnlinePlayers().stream().filter(p -> !players.contains(p.getUniqueId())).forEach(p -> tpToSpectator(p));

		Task.tryStartTask(startCountdownTask);

		ModuleManager.disable(CompassTracker.class);

		Bukkit.getServer().getWorlds().forEach(w -> {
			VersionIndependentUtils.get().setGameRule(w, "keepInventory", "true");
			VersionIndependentUtils.get().setGameRule(w, "announceAdvancements", "false");
		});

		started = true;

	}

	@Override
	public void onEnd(GameEndReason reason) {
		if (ended) {
			return;
		}

		config.getCheckpoints().forEach(c -> {
			Location location = c.getSpawnLocation();
			Firework fw = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
			FireworkMeta fwm = fw.getFireworkMeta();

			fwm.setPower(0);
			fwm.addEffect(RandomFireworkEffect.randomFireworkEffect());

			if (random.nextBoolean()) {
				fwm.addEffect(RandomFireworkEffect.randomFireworkEffect());
			}

			fw.setFireworkMeta(fwm);
			fw.detonate();
		});

		switch (reason) {
		case ALL_FINISHED:
			Bukkit.getServer().broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "Game Over> All players finished");
			break;

		case TIME:
			Bukkit.getServer().broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "Game Over> Time is up");
			break;

		case OPERATOR_ENDED_GAME:
			Bukkit.getServer().broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "Game Over> An admin has ended the game");
			break;

		default:
			break;
		}

		Bukkit.getServer().getOnlinePlayers().forEach(player -> {
			VersionIndependentUtils.get().resetEntityMaxHealth(player);
			player.setFoodLevel(20);
			PlayerUtils.clearPlayerInventory(player);
			PlayerUtils.resetPlayerXP(player);
			player.setGameMode(GameMode.SPECTATOR);
		});

		Task.tryStopTask(foodAndLapTask);
		Task.tryStopTask(checkTask);
		Task.tryStopTask(compassTask);
		Task.tryStopTask(particleTask);
		Task.tryStopTask(startCountdownTask);
		Task.tryStopTask(gameCountdownTimer);

		ended = true;
	}

	public PlayerData getPlayerData(Player player) {
		return playerDataList.stream().filter(pd -> pd.getUuid().equals(player.getUniqueId())).findFirst().orElse(null);
	}

	public ParkourRaceConfiguration getConfig() {
		return config;
	}

	public int getTimeLeft() {
		return timeLeft;
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent e) {
		if (e.getWhoClicked().getGameMode() != GameMode.CREATIVE) {
			e.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerDeath(PlayerDeathEvent e) {
		e.setKeepInventory(true);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerRespawn(Player player) {
		new BukkitRunnable() {
			@Override
			public void run() {
				teleportPlayer(player);
			}
		}.runTaskLater(getPlugin(), 1L);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerDropItem(PlayerDropItemEvent e) {
		e.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerInteract(PlayerInteractEvent e) {
		if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
			e.setCancelled(true);
		}

		if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Player player = e.getPlayer();
			if (VersionIndependentUtils.getInstance().isInteractEventMainHand(e)) {
				ItemStack item = VersionIndependentUtils.get().getItemInMainHand(player);
				if (item != null) {
					if (item.getType() == Material.ENDER_PEARL) {
						teleportPlayer(player);
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onVersionIndependentPlayerAchievementAwarded(VersionIndependentPlayerAchievementAwardedEvent e) {
		e.setCancelled(true);
	}
}