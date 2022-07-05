package net.novauniverse.games.parkourrace.game;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.md_5.bungee.api.ChatColor;
import net.novauniverse.games.parkourrace.game.config.Checkpoint;
import net.novauniverse.games.parkourrace.game.config.ParkourRaceConfiguration;
import net.novauniverse.games.parkourrace.game.data.PlayerData;
import net.zeeraa.novacore.commons.log.Log;
import net.zeeraa.novacore.commons.tasks.Task;
import net.zeeraa.novacore.spigot.abstraction.VersionIndependentUtils;
import net.zeeraa.novacore.spigot.abstraction.enums.VersionIndependentSound;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.GameEndReason;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.MapGame;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.elimination.PlayerQuitEliminationAction;
import net.zeeraa.novacore.spigot.tasks.SimpleTask;
import net.zeeraa.novacore.spigot.teams.Team;
import net.zeeraa.novacore.spigot.teams.TeamManager;
import net.zeeraa.novacore.spigot.utils.ChatColorRGBMapper;
import net.zeeraa.novacore.spigot.utils.ItemBuilder;
import net.zeeraa.novacore.spigot.utils.VectorArea;

public class ParkourRace extends MapGame implements Listener {
	private boolean started;
	private boolean ended;

	private Task checkTask;
	private Task compassTask;
	private Task startCountdownTask;

	private int startCountdown;

	private ParkourRaceConfiguration config;

	private List<PlayerData> playerDataList;

	public ParkourRace(Plugin plugin) {
		super(plugin);

		this.started = false;
		this.ended = false;

		this.startCountdown = 0;

		this.playerDataList = new ArrayList<>();

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
						for (int y = cage.getPosition1().getBlockX(); y <= cage.getPosition2().getBlockX(); y++) {
							for (int z = cage.getPosition1().getBlockX(); z <= cage.getPosition2().getBlockX(); z++) {
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
					Checkpoint checkpoint = config.getCheckpoints().stream().filter(c -> c.isInside(player)).findFirst().orElse(null);
					if (checkpoint != null) {
						int cSequence = checkpoint.getSequence();
						int pSequence = playerData.getSequence();

						if (cSequence <= pSequence) {
							// Player has already passed the checkpoint
							return;
						}

						if (cSequence - 1 > pSequence) {
							if (playerData.getLastSequenceWarning() != cSequence) {
								VersionIndependentSound.WITHER_HURT.play(player);
								player.sendMessage(ChatColor.RED + "You missed a checkpoint before this one. Right click with the [INSER SOMETHING HERE] to go back to your last checkpoint");
								playerData.setLastSequenceWarning(cSequence);
							}
							// Player has not passed the previous checkpoint and will receive a warning
							return;
						}

						if (cSequence - 1 == pSequence) {
							// Player should unlock checkpoint
							VersionIndependentSound.ORB_PICKUP.play(player);
							player.sendMessage(ChatColor.GREEN + "Checkpoint reached");
							playerData.setSequence(cSequence);
						}
					}
				});
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

	public void setupPlayerData(Player player) {
		if (playerDataList.stream().filter(pd -> pd.isOwnedBy(player)).count() == 0) {
			playerDataList.add(new PlayerData(player.getUniqueId()));
		}
	}

	public void teleportPlayer(Player player) {
		PlayerData playerData = playerDataList.stream().filter(pd -> pd.isOwnedBy(player)).findFirst().orElse(null);
		if (playerData == null) {
			Log.warn("ParkourRace", "Cant spawn " + player.getName() + " since they dont have any player data");
			return;
		}

		player.teleport(playerData.getRespawnLocation());
		player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true, true));
		player.setGameMode(GameMode.ADVENTURE);

		ChatColor color = ChatColor.AQUA;
		if (TeamManager.hasTeamManager()) {
			Team team = TeamManager.getTeamManager().getPlayerTeam(player);
			if (team != null) {
				color = team.getTeamColor();
			}
		}
		ItemBuilder bootsBuilder = new ItemBuilder(Material.LEATHER_BOOTS);
		bootsBuilder.setUnbreakable(true);
		bootsBuilder.setLeatherArmorColor(ChatColorRGBMapper.chatColorToRGBColorData(color).toBukkitColor());
		player.getInventory().setBoots(bootsBuilder.build());
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
		return false;
	}

	@Override
	public boolean hasEnded() {
		return false;
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

		Bukkit.getServer().getOnlinePlayers().forEach(p -> setupPlayerData(p));

		Task.tryStartTask(checkTask);
		Task.tryStartTask(compassTask);

		Bukkit.getServer().getOnlinePlayers().forEach(p -> teleportPlayer(p));

		Task.tryStartTask(startCountdownTask);
		started = true;
	}

	@Override
	public void onEnd(GameEndReason reason) {
		if (ended) {
			return;
		}

		Task.tryStopTask(checkTask);
		Task.tryStartTask(compassTask);
		Task.tryStartTask(startCountdownTask);

		ended = true;
	}

	public ParkourRaceConfiguration getConfig() {
		return config;
	}
}
