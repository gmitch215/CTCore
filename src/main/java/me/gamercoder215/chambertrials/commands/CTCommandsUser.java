package me.gamercoder215.chambertrials.commands;

import fr.skytasul.glowingentities.GlowingEntities;
import me.gamercoder215.chambertrials.CTCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.bukkit.BukkitCommandHandler;

public final class CTCommandsUser {

    private final CTCore plugin;
    private final GlowingEntities glow;

    public CTCommandsUser(CTCore plugin, BukkitCommandHandler handler) {
        this.plugin = plugin;
        this.glow = new GlowingEntities(plugin);
    }

    @Command({"hidepeople", "hidepeeps"})
    public void hidePeople(Player p) {
        for (Player t : Bukkit.getOnlinePlayers()) {
            if (t.equals(p)) continue;
            p.hidePlayer(plugin, t);
        }
    }

    @Command({"showpeople", "showpeeps"})
    public void showPeople(Player p) {
        for (Player t : Bukkit.getOnlinePlayers()) {
            if (t.equals(p)) continue;
            p.showPlayer(plugin, p);
        }
    }

    @Command({"showteammates", "showfriends"})
    public void highlightTeammates(Player p) {
        Team t = Bukkit.getScoreboardManager().getMainScoreboard().getTeams()
                .stream()
                .filter(team -> team.hasEntry(p.getName()))
                .findFirst()
                .orElse(null);

        if (t == null) {
            p.sendMessage(ChatColor.RED + "You are not on a team!");
            return;
        }

        for (String targetName : t.getEntries()) {
            Player target = Bukkit.getPlayer(targetName);
            if (target == null) continue;

            try {
                glow.setGlowing(target, p, t.getColor());
            } catch (ReflectiveOperationException e) {
                CTCore.print(e);
            }
        }
    }

    @Command({"hideteammates", "hidefriends"})
    public void unhighlightTeammates(Player p) {
        Team t = Bukkit.getScoreboardManager().getMainScoreboard().getTeams()
                .stream()
                .filter(team -> team.hasEntry(p.getName()))
                .findFirst()
                .orElse(null);

        if (t == null) {
            p.sendMessage(ChatColor.RED + "You are not on a team!");
            return;
        }

        for (String targetName : t.getEntries()) {
            Player target = Bukkit.getPlayer(targetName);
            if (target == null) continue;

            try {
                glow.unsetGlowing(target, p);
            } catch (ReflectiveOperationException e) {
                CTCore.print(e);
            }
        }
    }

}
