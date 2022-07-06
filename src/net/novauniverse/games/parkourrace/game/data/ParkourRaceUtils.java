package net.novauniverse.games.parkourrace.game.data;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.util.Vector;

public class ParkourRaceUtils {
	public static List<Vector> getHollowCube(Vector pos1, Vector pos2, double particleDistance) {
		List<Vector> result = new ArrayList<>();
		double minX = pos1.getBlockX();
		double minY = pos1.getBlockY();
		double minZ = pos1.getBlockZ();
		double maxX = pos2.getBlockX();
		double maxY = pos2.getBlockY();
		double maxZ = pos2.getBlockZ();

		for (double x = minX; x <= maxX; x = Math.round((x + particleDistance) * 1e2) / 1e2) {
			for (double y = minY; y <= maxY; y = Math.round((y + particleDistance) * 1e2) / 1e2) {
				for (double z = minZ; z <= maxZ; z = Math.round((z + particleDistance) * 1e2) / 1e2) {
					int components = 0;
					if (x == minX || x == maxX)
						components++;
					if (y == minY || y == maxY)
						components++;
					if (z == minZ || z == maxZ)
						components++;
					if (components >= 2) {
						result.add(new Vector(x, y, z));
					}
				}
			}
		}
		return result;
	}
}