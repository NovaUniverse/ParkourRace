package net.novauniverse.games.parkourrace.game.command;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.json.JSONObject;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Region;

import net.zeeraa.novacore.spigot.command.AllowedSenders;
import net.zeeraa.novacore.spigot.command.NovaCommand;
import net.zeeraa.novacore.spigot.utils.LocationUtils;
import net.zeeraa.novacore.spigot.utils.VectorArea;

public class CopyParkourRaceCheckpointCommand extends NovaCommand {
	public CopyParkourRaceCheckpointCommand(Plugin owner) {
		super("copyparkourracecheckpoint", owner);

		setAllowedSenders(AllowedSenders.PLAYERS);
		setPermission("parkourrace.copyparkourracecheckpoint");
		setPermissionDefaultValue(PermissionDefault.OP);
		setPermissionDescription("Make a checkpoint");
		setUsage("/copyparkourracecheckpoint");
		setDescription("Create checkpoint json and save it to the server clipboard");
		addHelpSubCommand();
	}

	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {
		Player player = (Player) sender;
		try {
			WorldEditPlugin worldEdit = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
			LocalSession session = worldEdit.getSession(player);
			Region region = worldEdit.getSession(player).getSelection(session.getSelectionWorld());

			int x1 = region.getMinimumPoint().getBlockX();
			int y1 = region.getMinimumPoint().getBlockY();
			int z1 = region.getMinimumPoint().getBlockZ();

			int x2 = region.getMaximumPoint().getBlockX();
			int y2 = region.getMaximumPoint().getBlockY();
			int z2 = region.getMaximumPoint().getBlockZ();

			VectorArea area = new VectorArea(x1, y1, z1, x2, y2, z2);

			JSONObject json = new JSONObject();

			json.put("unlock_area", area.toJSON());

			JSONObject spawnLocation = LocationUtils.toJSONObject(player.getLocation());
			spawnLocation.remove("world");
			json.put("spawn_location", spawnLocation);

			json.put("sequence", 1);

			String string = json.toString(4);

			StringSelection stringSelection = new StringSelection(string);

			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(stringSelection, null);

			player.sendMessage(ChatColor.GREEN + "ok");

			player.sendMessage(ChatColor.AQUA + string);

			return true;
		} catch (Exception e) {
			player.sendMessage(ChatColor.RED + e.getClass().getName() + " " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}
}