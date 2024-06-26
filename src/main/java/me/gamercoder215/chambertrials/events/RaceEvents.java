package me.gamercoder215.chambertrials.events;

import com.google.common.collect.ImmutableMap;
import me.gamercoder215.chambertrials.CTCore;
import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class RaceEvents implements Listener {

    private CTCore plugin;

    public RaceEvents(@NotNull CTCore plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static Vector dir(Player p, double mod) {
        Location loc = p.getLocation();
        loc.setPitch(0.0f);
        return loc.getDirection().normalize().multiply(mod);
    }

    private final Map<String, Consumer<Player>> TRACK_ACTIONS = ImmutableMap.<String, Consumer<Player>>builder()
            .put("orange",
                    p -> p.setVelocity(new Vector(0, plugin.getConfig().getDouble("Races.OrangeVelocity"), 0)
                            .add(dir(p, plugin.getConfig().getDouble("Races.OrangeVelocityDirectional")))
                    )
            )
            .put("lime",
                    p -> p.setVelocity(p.getVelocity().add(new Vector(0, plugin.getConfig().getDouble("Races.LimeVelocity"), 0)))
            )
            .build();

    @EventHandler
    public void onJump(PlayerStatisticIncrementEvent e) {
        if (e.getStatistic() != Statistic.JUMP) return;

        Player p = e.getPlayer();
        List<Block> possibleLocations = Stream.of(
                p.getLocation(),
                p.getLocation().subtract(0, 1, 0)
        ).map(Location::getBlock).toList();

        for (Block b : possibleLocations) {
            List<ItemFrame> itemFrames = b.getWorld().getNearbyEntities(b.getLocation(), 0, 0.5, 0).stream()
                    .filter(en -> en instanceof ItemFrame)
                    .map(ItemFrame.class::cast)
                    .filter(en -> en.getScoreboardTags().stream().anyMatch(s -> s.endsWith("_boost")))
                    .toList();

            for (ItemFrame frame : itemFrames)
                for (String tag : frame.getScoreboardTags()) {
                    String color = tag.split("_")[0];
                    if (TRACK_ACTIONS.containsKey(color))
                        TRACK_ACTIONS.get(color).accept(p);
                }
        }
    }

}
