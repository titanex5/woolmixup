package pl.twojserwer.woolmixup;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.List;
import java.util.Random;

/**
 * Plaska, kwadratowa arena NxN zbudowana z kolorowej welny. Wspolrzedne
 * (minX, minZ) to poludniowo-zachodni rog, y to poziom podlogi (gracz stoi
 * na y+1).
 */
public class Arena {

    private final String name;
    private final String worldName;
    private final int minX;
    private final int y;
    private final int minZ;
    private final int size;

    public Arena(String name, String worldName, int minX, int y, int minZ, int size) {
        this.name = name;
        this.worldName = worldName;
        this.minX = minX;
        this.y = y;
        this.minZ = minZ;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public World getWorld() {
        return org.bukkit.Bukkit.getWorld(worldName);
    }

    public String getWorldName() {
        return worldName;
    }

    public int getY() {
        return y;
    }

    public int getSize() {
        return size;
    }

    /** Srodek areny (X/Z) na poziomie podlogi + 1, do teleportacji gracza. */
    public Location getSpawnLocation() {
        double centerX = minX + size / 2.0;
        double centerZ = minZ + size / 2.0;
        return new Location(getWorld(), centerX, y + 1, centerZ);
    }

    /** Wypelnia cala plansze losowymi kolorami z podanej puli, w kwadratowych
     *  "kaflach" o boku cellSize (np. 3 = kazdy kolor zajmuje pole 3x3 blokow). */
    public void fillRandom(List<Material> palette, Random random, int cellSize) {
        World world = getWorld();
        if (world == null) {
            return;
        }
        if (palette == null || palette.isEmpty()) {
            return;
        }

        int step = Math.max(1, cellSize);
        int cellsX = (size + step - 1) / step;
        int cellsZ = (size + step - 1) / step;
        Material[][] cells = new Material[cellsX][cellsZ];

        // Losujemy kolor dla kazdego pola 3x3 (lub cellSize x cellSize),
        // ale odrzucamy kolory takie same jak pole po lewej i nad nim.
        // Dzieki temu identyczne kolory nigdy nie stykaja sie bokiem.
        for (int cx = 0; cx < cellsX; cx++) {
            for (int cz = 0; cz < cellsZ; cz++) {
                List<Material> available = new java.util.ArrayList<>(palette);
                Material left = cx > 0 ? cells[cx - 1][cz] : null;
                Material above = cz > 0 ? cells[cx][cz - 1] : null;
                available.remove(left);
                available.remove(above);

                // Przy co najmniej 2 kolorach zawsze powinien zostac wybor.
                // Fallback zabezpiecza nietypowe konfiguracje.
                List<Material> choices = available.isEmpty() ? palette : available;
                Material color = choices.get(random.nextInt(choices.size()));
                cells[cx][cz] = color;

                int startX = cx * step;
                int startZ = cz * step;
                int maxDx = Math.min(startX + step, size);
                int maxDz = Math.min(startZ + step, size);
                for (int dx = startX; dx < maxDx; dx++) {
                    for (int dz = startZ; dz < maxDz; dz++) {
                        Block block = world.getBlockAt(minX + dx, y, minZ + dz);
                        block.setType(color);
                    }
                }
            }
        }
    }

    /** Zostawia tylko bloki podanego koloru, reszte zamienia w powietrze (dziura). */
    public void collapseExcept(Material keepColor) {
        World world = getWorld();
        if (world == null) {
            return;
        }
        for (int dx = 0; dx < size; dx++) {
            for (int dz = 0; dz < size; dz++) {
                Block block = world.getBlockAt(minX + dx, y, minZ + dz);
                if (block.getType() != keepColor) {
                    block.setType(Material.AIR);
                }
            }
        }
    }

    /** Czy podana lokalizacja (X/Z) miesci sie w obrysie areny. */
    public boolean contains(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().getName().equals(worldName)) {
            return false;
        }
        double x = loc.getX();
        double z = loc.getZ();
        return x >= minX && x < minX + size && z >= minZ && z < minZ + size;
    }

    /** Material bloku dokladnie pod stopami gracza (feetLoc.getY() - 1). */
    public Material blockBelow(Location feetLoc) {
        World world = getWorld();
        if (world == null) {
            return Material.AIR;
        }
        int bx = feetLoc.getBlockX();
        int bz = feetLoc.getBlockZ();
        return world.getBlockAt(bx, y, bz).getType();
    }

    public String getWorldNameSafe() {
        return worldName;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinZ() {
        return minZ;
    }
}
