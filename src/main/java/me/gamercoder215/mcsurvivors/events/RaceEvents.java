package me.gamercoder215.mcsurvivors.events;

import com.google.common.collect.ImmutableMap;
import me.gamercoder215.mcsurvivors.MCSCore;
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

    public RaceEvents(@NotNull MCSCore plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static Vector dir(Player p, double mod) {
        return p.getLocation().getDirection().normalize().multiply(mod);
    }

    private static final Map<String, Consumer<Player>> TRACK_ACTIONS = ImmutableMap.<String, Consumer<Player>>builder()
            .put("orange",
                    p -> p.setVelocity(p.getVelocity()
                            .add(new Vector(0, 0.5, 0))
                            .add(dir(p, 0.25))
                    )
            )
            .put("lime",
                    p -> p.setVelocity(p.getVelocity()
                            .add(new Vector(0, 1, 0))
                            .add(dir(p, 0.5))
                    )
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
            List<ItemFrame> itemFrames = b.getWorld().getNearbyEntities(b.getLocation(), 0.5, 0.5, 0.5).stream()
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
