package pl.twojserwer.woolmixup.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.twojserwer.woolmixup.Arena;
import pl.twojserwer.woolmixup.ArenaManager;
import pl.twojserwer.woolmixup.ScoreStorage;
import pl.twojserwer.woolmixup.WoolMixUpPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class WoolMixUpCommand implements CommandExecutor, TabCompleter {

    // Rozmiar podawany przez admina to liczba KAFLI (cell-size z config.yml,
    // domyslnie 3x3 bloki kazdy) w jedna strone, NIE liczba blokow.
    // np. /woolmixup setup arena 21 -> 21x21 kafli -> 21*3 = 63x63 blokow.
    private static final int MIN_CELLS = 5;
    private static final int MAX_CELLS = 21;

    private final WoolMixUpPlugin plugin;
    private final ArenaManager arenaManager;
    private final ScoreStorage scoreStorage;

    public WoolMixUpCommand(WoolMixUpPlugin plugin, ArenaManager arenaManager, ScoreStorage scoreStorage) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.scoreStorage = scoreStorage;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Ta komenda dziala tylko w grze.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§eUzycie: /woolmixup <setup|start|stop|best> [nazwa] [rozmiar]");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "setup" -> handleSetup(player, args);
            case "start" -> handleStart(player, args);
            case "stop" -> plugin.stopGame(player);
            case "best" -> handleBest(player);
            case "list" -> handleList(player);
            default -> sender.sendMessage("§eUzycie: /woolmixup <setup|start|stop|best|list> [nazwa] [rozmiar]");
        }
        return true;
    }

    private void handleSetup(Player player, String[] args) {
        if (!player.hasPermission("woolmixup.admin")) {
            player.sendMessage("§cBrak uprawnien (woolmixup.admin).");
            return;
        }
        if (args.length < 2) {
            player.sendMessage("§eUzycie: /woolmixup setup <nazwa> [kafle=9] §7(np. 21 = plansza 21x21 kafli)");
            return;
        }
        String name = args[1];
        int cells = 9;
        if (args.length >= 3) {
            try {
                cells = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                player.sendMessage("§cRozmiar musi byc liczba.");
                return;
            }
        }
        if (cells < MIN_CELLS || cells > MAX_CELLS) {
            player.sendMessage("§cRozmiar musi byc miedzy " + MIN_CELLS + " a " + MAX_CELLS + " (w kaflach).");
            return;
        }
        if (arenaManager.getArena(name) != null) {
            player.sendMessage("§cArena o nazwie '" + name + "' juz istnieje.");
            return;
        }

        int cellSize = plugin.getCellSize();
        int blockSize = cells * cellSize;

        int minX = player.getLocation().getBlockX();
        int minZ = player.getLocation().getBlockZ();
        int y = player.getLocation().getBlockY() - 1;

        Arena arena = new Arena(name, player.getWorld().getName(), minX, y, minZ, blockSize);
        arenaManager.addArena(arena);

        player.sendMessage("§aStworzono arene '" + name + "' - " + cells + "x" + cells
                + " kafli (" + blockSize + "x" + blockSize + " blokow) na Twojej aktualnej pozycji. "
                + "Zagraj: §f/woolmixup start " + name);
    }

    private void handleStart(Player player, String[] args) {
        String arenaName;
        if (args.length >= 2) {
            arenaName = args[1];
        } else if (arenaManager.getArenas().size() == 1) {
            arenaName = arenaManager.getArenas().values().iterator().next().getName();
        } else if (arenaManager.getArenas().isEmpty()) {
            player.sendMessage("§cNie ma jeszcze zadnej areny. Poprosz admina o /woolmixup setup <nazwa>.");
            return;
        } else {
            player.sendMessage("§eJest kilka aren, podaj nazwe: /woolmixup start <nazwa>. Dostepne: "
                    + String.join(", ", arenaManager.getArenas().keySet()));
            return;
        }
        plugin.startGame(player, arenaName);
    }

    private void handleBest(Player player) {
        int best = scoreStorage.getBest(player.getUniqueId());
        player.sendMessage("§7Twoj rekord WoolMixUp: §f" + best + " §7rund.");
    }

    private void handleList(Player player) {
        if (arenaManager.getArenas().isEmpty()) {
            player.sendMessage("§7Brak stworzonych aren.");
            return;
        }
        int cellSize = plugin.getCellSize();
        player.sendMessage("§7Areny:");
        for (Arena arena : arenaManager.getArenas().values()) {
            int size = arena.getSize();
            int cells = cellSize > 0 ? size / cellSize : size;
            player.sendMessage("§f - " + arena.getName() + " §7(" + size + "x" + size
                    + " blokow, " + cells + "x" + cells + " kafli)");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("setup", "start", "stop", "best", "list");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("start"))) {
            return arenaManager.getArenas().values().stream().map(Arena::getName).toList();
        }
        return List.of();
    }
}
