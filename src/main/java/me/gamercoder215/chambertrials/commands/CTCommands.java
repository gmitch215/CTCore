package me.gamercoder215.chambertrials.commands;

import me.gamercoder215.chambertrials.CTCore;
import me.gamercoder215.chambertrials.biome.CTBiome;
import me.gamercoder215.chambertrials.biome.CTBiomeManager;
import net.minecraft.ResourceLocationException;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_20_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.*;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.BukkitCommandHandler;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import static me.gamercoder215.chambertrials.CTCore.prefix;
import static me.gamercoder215.chambertrials.biome.CTBiome.*;
import static me.gamercoder215.chambertrials.biome.CTBiomeManager.changeRegistryLock;

@Command({"ct", "ctrials", "ctrial"})
@CommandPermission("ctrials.admin")
@Description("Main Chamber Trials Administrator Command")
@Usage("/ct <command>")
public final class CTCommands {

    private final CTCore plugin;

    private static <T extends Enum<T>> boolean hasEnum(Class<T> clazz, String name) {
        for (T t : clazz.getEnumConstants()) if (t.name().equalsIgnoreCase(name)) return true;

        return false;
    }

    private static String toHex(@NotNull Color c) {
        return String.format("%06x", c.asRGB() & 0xFFFFFF);
    }

    private static Color toChatColor(@NotNull ChatColor chatColor) {
        return switch (chatColor) {
            case AQUA -> Color.AQUA;
            case BLACK -> Color.BLACK;
            case BLUE -> Color.BLUE;
            case DARK_AQUA -> Color.fromRGB(0, 170, 170);
            case DARK_BLUE -> Color.fromRGB(0, 0, 170);
            case DARK_GRAY -> Color.fromRGB(85, 85, 85);
            case DARK_GREEN -> Color.fromRGB(0, 170, 0);
            case DARK_PURPLE -> Color.PURPLE;
            case DARK_RED -> Color.fromRGB(170, 0, 0);
            case GOLD -> Color.fromRGB(255, 170, 0);
            case GRAY -> Color.fromRGB(170, 170, 170);
            case GREEN -> Color.GREEN;
            case LIGHT_PURPLE -> Color.fromRGB(255, 85, 255);
            case RED -> Color.RED;
            case WHITE -> Color.WHITE;
            case YELLOW -> Color.YELLOW;
            default -> null;
        };
    }

    public CTCommands(CTCore plugin, BukkitCommandHandler handler) {
        this.plugin = plugin;
        handler.registerValueResolver(CTBiome.class, ctx -> CTBiome.byName(ctx.popForParameter()))
                .registerValueResolver(Color.class, ctx -> {
                    String param = ctx.popForParameter();

                    if (hasEnum(ChatColor.class, param)) {
                        ChatColor c = ChatColor.valueOf(param.toUpperCase());
                        if (!c.isColor()) return null;

                        return toChatColor(c);
                    }

                    if (param.startsWith("#")) param = param.replaceFirst("#", "");

                    return Color.fromRGB(Integer.parseInt(param, 16));
                })
                .registerValueResolver(Biome.class, ctx -> {
                    String param = ctx.popForParameter();
                    if (hasEnum(Biome.class, param)) return Biome.valueOf(param.toUpperCase());

                    return null;
                })
                .registerValueResolver(PlaceOption.class, ctx -> {
                    String param = ctx.popForParameter();
                    if (hasEnum(PlaceOption.class, param)) return PlaceOption.valueOf(param.toUpperCase());

                    return null;
                });

        handler.getAutoCompleter()
                .registerParameterSuggestions(boolean.class, SuggestionProvider.of("true", "false"))
                .registerParameterSuggestions(Color.class, SuggestionProvider.of(
                        Arrays.stream(ChatColor.values())
                        .filter(ChatColor::isColor)
                        .map(ChatColor::name)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList()))
                )
                .registerParameterSuggestions(Biome.class, SuggestionProvider.of(Arrays.stream(Biome.values())
                        .map(Biome::name)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList())
                ))
                .registerParameterSuggestions(PlaceOption.class, SuggestionProvider.of(Arrays.stream(PlaceOption.values())
                        .map(PlaceOption::name)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList())
                ))
                .registerParameterSuggestions(CTBiome.class, SuggestionProvider.map(CTBiome::getAllBiomes, CTBiome::getName));

        handler.register(this);

        handler.registerBrigadier();
        handler.setLocale(Locale.ENGLISH);
        plugin.getLogger().info("Loaded MCSCommands...");
    }

    @Subcommand({"createbiome", "createb", "cbiome", "biome create"})
    public void createBiome(Player p, String name,
                            @Default(DEFAULT_WATER_COLOR) Color waterColor,
                            @Default(DEFAULT_FOG_COLOR) Color fogColor,
                            @Default(DEFAULT_SKY_COLOR) Color skyColor,
                            @Default(DEFAULT_GRASS_COLOR) Color grassColor,
                            @Default(DEFAULT_FOLIAGE_COLOR) Color foliageColor,
                            @Switch boolean frozen) {

        if (CTBiome.byName(name) != null) {
            p.sendMessage(prefix() + ChatColor.RED + "A biome with that name already exists!");
            return;
        }

        try {
            new ResourceLocation("mcsurvivors", name.toLowerCase());
        } catch (ResourceLocationException e) {
            p.sendMessage(prefix() + ChatColor.RED + "Invalid name:\n" + e.getMessage());
            return;
        }

        CTBiome biome = CTBiome.builder(name)
                .setFrozen(frozen)
                .setWaterColor(toHex(waterColor))
                .setFogColor(toHex(fogColor))
                .setSkyColor(toHex(skyColor))
                .setGrassColor(toHex(grassColor))
                .setFoliageColor(toHex(foliageColor))
                .build();

        changeRegistryLock(false);
        CTBiomeManager.registerBiome(biome);
        changeRegistryLock(true);

        p.sendMessage(prefix() + "Created biome " + ChatColor.GREEN + name + "!");
        success(p);
    }

    @Subcommand({"biome updatechunk", "updatechunk", "biome updatec", "biome uc", "uc"})
    public void updateChunk(Player p) {
        p.sendMessage(prefix() + "Updating chunk...");

        Chunk c = p.getLocation().getChunk();
        LevelChunk nms = (LevelChunk) ((CraftChunk) c).getHandle(ChunkStatus.BIOMES);
        ServerPlayer sp = ((CraftPlayer) p).getHandle();
        sp.connection.send(new ClientboundLevelChunkWithLightPacket(nms, nms.getLevel().getLightEngine(), null, null));

        p.sendMessage(prefix() + "Updated chunk!");
        success(p);
    }

    public enum PlaceOption {
        SELF,
        CHUNK

        ;

        public boolean equalsIgnoreCase(String s) {
            return name().equalsIgnoreCase(s);
        }
    }

    @Subcommand({"biome set", "setbiome"})
    public void setBiome(Player p, PlaceOption option, CTBiome biome) {
        if (biome == null) {
            p.sendMessage(prefix() + ChatColor.RED + "That biome does not exist!");
            return;
        }

        switch (option) {
            case SELF -> CTBiomeManager.placeBiome(biome, p.getLocation(), true);
            case CHUNK -> CTBiomeManager.placeBiome(biome, p.getLocation().getChunk(), true);
        }

        p.sendMessage(prefix() + "Biome set to " + biome.getName() + "!");
    }

    @Subcommand({"biome delete", "biome remove", "removebiome", "deletebiome"})
    public void deleteBiome(CommandSender sender, CTBiome biome, @Default("") String confirm) {
        if (biome == null) {
            sender.sendMessage(prefix() + ChatColor.RED + "That biome does not exist!");
            return;
        }

        if (!confirm.equalsIgnoreCase("confirm")) {
            sender.sendMessage(prefix() + ChatColor.RED + "Please confirm this action by typing /mcs deletebiome " + biome.getName() + " confirm! YOU CANNOT UNDO THIS!");
            return;
        }

        CTBiome.removeBiome(biome);
        if (sender instanceof Player p) success(p);
    }

    private static void setBorders(@NotNull Inventory inv, ItemStack item) {
        int size = inv.getSize();

        ItemStack bg = item.clone();
        ItemMeta bgMeta = bg.getItemMeta();
        bgMeta.setDisplayName(" ");
        bg.setItemMeta(bgMeta);

        for (int i = 0; i < 9; i++) inv.setItem(i, bg);
        for (int i = size - 9; i < size; i++) inv.setItem(i, bg);
        for (int i = 1; i < Math.floor((double) size / 9D) - 1; i++) {
            inv.setItem(i * 9, bg);
            inv.setItem(((i + 1) * 9) - 1, bg);
        }
    }

    private static void setBorders(@NotNull Inventory inv, Material bg) {
        setBorders(inv, new ItemStack(bg));
    }

    @Subcommand({"biome info", "biomeinfo", "biome information"})
    public void biomeInfo(Player p, CTBiome biome) {
        if (biome == null) {
            p.sendMessage(prefix() + ChatColor.RED + "That biome does not exist!");
            return;
        }

        Inventory info = Bukkit.createInventory(new CTCore.CancelHolder(), 45, "Biome Information | " + biome.getName());
        setBorders(info, Material.BLACK_STAINED_GLASS_PANE);

        p.openInventory(info);
        success(p);
    }

    @Subcommand({"biome list", "biomelist", "biome listall", "biomelistall"})
    public void listBiomes(CommandSender sender) {
        StringBuilder b = new StringBuilder();
        b.append(ChatColor.GOLD).append(ChatColor.UNDERLINE).append("Custom Biome List").append("\n");

        for (CTBiome biome : CTBiome.getAllBiomes())
            b.append(ChatColor.AQUA).append(biome.getName())
                    .append(ChatColor.GOLD)
                    .append(" | ")
                    .append(ChatColor.DARK_AQUA)
                    .append(biome.getResourceKey().location().toString())
                    .append("\n");

        sender.sendMessage(b.toString());
    }

    @Subcommand({"biome register", "registerbiome"})
    public void registerBiome(Player p, CTBiome biome) {
        if (biome == null) {
            p.sendMessage(prefix() + ChatColor.RED + "That biome does not exist!");
            return;
        }

        if (!isPluginOwner(p)) {
            p.sendMessage(prefix() + ChatColor.RED + "You do not have permission to register biomes!");
            return;
        }

        p.sendMessage("[DEBUG] Biome " + biome.getName() + " Registry Status: " + CTBiomeManager.isRegistered(biome));

        changeRegistryLock(false);
        CTBiomeManager.registerBiome(biome);
        changeRegistryLock(true);

        p.sendMessage("[DEBUG] Biome " + biome.getName() + " Registry Status: " + CTBiomeManager.isRegistered(biome));

        p.sendMessage(prefix() + "Registered biome " + biome.getName() + "!");
        success(p);
    }

    @Subcommand({"variable orange_velocity"})
    public void orange(CommandSender sender, @Default("0.2") double velocity) {
        FileConfiguration config = plugin.getConfig();
        config.set("Races.OrangeVelocity", velocity);
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            CTCore.print(e);
        }plugin.reloadConfig();


        sender.sendMessage(prefix() + ChatColor.GREEN + "Set orange velocity to " + ChatColor.GOLD + velocity + "!");
    }

    @Subcommand({"variable orange_velocity_directional"})
    public void orangeDirectional(CommandSender sender, @Default("0.15") double velocity) {
        FileConfiguration config = plugin.getConfig();
        config.set("Races.OrangeVelocityDirectional", velocity);
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            CTCore.print(e);
        }
        plugin.reloadConfig();

        sender.sendMessage(prefix() + ChatColor.GREEN + "Set orange directional velocity to " + ChatColor.GOLD + velocity + "!");
    }

    @Subcommand({"variable lime_velocity"})
    public void lime(CommandSender sender, @Default("0.75") double velocity) {
        FileConfiguration config = plugin.getConfig();
        config.set("Races.LimeVelocity", velocity);
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            CTCore.print(e);
        }
        plugin.reloadConfig();

        sender.sendMessage(prefix() + ChatColor.GREEN + "Set lime velocity to " + ChatColor.GOLD + velocity + "!");
    }

    @Subcommand({"reload"})
    public void reload(CommandSender sender) {
        plugin.reloadConfig();
        sender.sendMessage(prefix() + ChatColor.GREEN + "Reloaded configuration!");
    }


    private static boolean isPluginOwner(Player p) {
        return p.getName().equalsIgnoreCase("gmitch215");
    }

    // Sound Util

    private static void success(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 3F, 2F);
    }

}
