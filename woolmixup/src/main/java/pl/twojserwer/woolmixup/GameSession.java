package pl.twojserwer.woolmixup;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Jedna rozgrywka jednego gracza na jednej arenie. Kazda runda:
 *  1) plansza wypelnia sie losowymi kolorami (fillRandom),
 *  2) losujemy kolor docelowy i pokazujemy go graczowi (tytul + actionbar),
 *  3) po uplywie czasu rundy wszystkie bloki OPROCZ docelowego koloru znikaja
 *     (collapseExcept) - jesli gracz w tym momencie nie stoi na dobrym kolorze,
 *     spada w dol i przegrywa.
 * Runda za runda czas sie skraca (do dolnego limitu z configu).
 */
public class GameSession {

    private final JavaPlugin plugin;
    private final WoolMixUpPlugin wmu;
    private final Player player;
    private final Arena arena;
    private final List<Material> palette;
    private final ScoreStorage scoreStorage;
    private final Random random = new Random();
    private final boolean debug;
    private final int cellSize;

    private int round = 0;
    private Material targetColor;
    private BukkitTask roundEndTask;
    private BukkitTask fallCheckTask;
    private boolean active = true;

    public GameSession(JavaPlugin plugin, WoolMixUpPlugin wmu, Player player, Arena arena,
                        List<Material> palette, ScoreStorage scoreStorage, boolean debug) {
        this.plugin = plugin;
        this.wmu = wmu;
        this.player = player;
        this.arena = arena;
        this.palette = palette;
        this.scoreStorage = scoreStorage;
        this.debug = debug;
        this.cellSize = wmu.getCellSize();
    }

    public Player getPlayer() {
        return player;
    }

    public Arena getArena() {
        return arena;
    }

    public void start() {
        player.teleport(arena.getSpawnLocation());
        player.sendMessage(Component.text("WoolMixUp start! Biegnij na kolor pokazany na ekranie.", NamedTextColor.GREEN));
        startFallWatcher();
        nextRound();
    }

    private void nextRound() {
        if (!active) {
            return;
        }
        round++;
        arena.fillRandom(palette, random, cellSize);
        targetColor = palette.get(random.nextInt(palette.size()));

        double roundSeconds = Math.max(
                wmu.getMinTimeSeconds(),
                wmu.getBaseTimeSeconds() - (round - 1) * wmu.getTimeDecreasePerRound()
        );
        long roundTicks = Math.round(roundSeconds * 20);

        String colorName = prettyColorName(targetColor);
        player.showTitle(Title.title(
                Component.text(colorName, colorNameToTextColor(targetColor)),
                Component.text("Runda " + round, NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(600), Duration.ofMillis(150))
        ));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.4f);

        if (debug) {
            plugin.getLogger().info("[WoolMixUp] [debug] " + player.getName() + " runda " + round
                    + ", kolor=" + targetColor + ", czas=" + roundSeconds + "s");
        }

        roundEndTask = Bukkit.getScheduler().runTaskLater(plugin, this::resolveRound, roundTicks);
    }

    /** Wywolywane gdy czas rundy minie - zawala plansze, sprawdza czy gracz przezyl. */
    private void resolveRound() {
        if (!active) {
            return;
        }
        arena.collapseExcept(targetColor);

        // Malutkie opoznienie, zeby serwer zdazyl przeslac zmiany blokow zanim
        // sprawdzimy pozycje gracza (unikamy false-positive na tym samym ticku).
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!active) {
                return;
            }
            Material standingOn = arena.blockBelow(player.getLocation());
            if (standingOn == targetColor) {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                Bukkit.getScheduler().runTaskLater(plugin,
                        this::nextRound,
                        Math.round(wmu.getCountdownBeforeRoundSeconds() * 20));
            }
            // jesli nie stoi na dobrym kolorze, fallCheckTask wykryje spadniecie
            // samo (gracz fizycznie spada w dziure) i wywola fail().
        }, 2L);
    }

    private void startFallWatcher() {
        fallCheckTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!active) {
                return;
            }
            Location loc = player.getLocation();
            if (!arena.contains(loc)) {
                return; // gracz zszedl z areny recznie - nie liczymy tego jako fail
            }
            if (loc.getY() < arena.getY() - wmu.getFallThresholdBlocks()) {
                fail();
            }
        }, 5L, 5L);
    }

    public void fail() {
        if (!active) {
            return;
        }
        active = false;
        cancelTasks();

        int finishedRounds = Math.max(0, round - 1);
        boolean newRecord = scoreStorage.submitScore(player.getUniqueId(), finishedRounds);

        player.showTitle(Title.title(
                Component.text("Koniec gry!", NamedTextColor.RED),
                Component.text("Przetrwales rund: " + finishedRounds
                        + (newRecord ? " - NOWY REKORD!" : ""), NamedTextColor.YELLOW)
        ));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.7f);

        // wyciagamy gracza znad dziury, zeby nie leciec dalej w dol
        player.teleport(arena.getSpawnLocation());

        wmu.endSession(player.getUniqueId());
    }

    public void stop() {
        if (!active) {
            return;
        }
        active = false;
        cancelTasks();
        player.sendMessage(Component.text("WoolMixUp przerwane.", NamedTextColor.GRAY));
        wmu.endSession(player.getUniqueId());
    }

    private void cancelTasks() {
        if (roundEndTask != null) {
            roundEndTask.cancel();
        }
        if (fallCheckTask != null) {
            fallCheckTask.cancel();
        }
    }

    private static String prettyColorName(Material wool) {
        String raw = wool.name().replace("_WOOL", "").replace("_", " ").toLowerCase(Locale.ROOT);
        return raw.substring(0, 1).toUpperCase(Locale.ROOT) + raw.substring(1);
    }

    private static NamedTextColor colorNameToTextColor(Material wool) {
        return switch (wool) {
            case WHITE_WOOL -> NamedTextColor.WHITE;
            case ORANGE_WOOL -> NamedTextColor.GOLD;
            case MAGENTA_WOOL -> NamedTextColor.LIGHT_PURPLE;
            case LIGHT_BLUE_WOOL -> NamedTextColor.AQUA;
            case YELLOW_WOOL -> NamedTextColor.YELLOW;
            case LIME_WOOL -> NamedTextColor.GREEN;
            case PINK_WOOL -> NamedTextColor.LIGHT_PURPLE;
            case CYAN_WOOL -> NamedTextColor.DARK_AQUA;
            case PURPLE_WOOL -> NamedTextColor.DARK_PURPLE;
            case RED_WOOL -> NamedTextColor.RED;
            case BLUE_WOOL -> NamedTextColor.BLUE;
            case GREEN_WOOL -> NamedTextColor.DARK_GREEN;
            case BLACK_WOOL -> NamedTextColor.DARK_GRAY;
            case BROWN_WOOL -> NamedTextColor.DARK_RED;
            case GRAY_WOOL -> NamedTextColor.GRAY;
            case LIGHT_GRAY_WOOL -> NamedTextColor.GRAY;
            default -> NamedTextColor.WHITE;
        };
    }
}
