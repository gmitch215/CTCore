package me.gamercoder215.mcsurvivors.biome;

import com.mojang.serialization.Lifecycle;
import me.gamercoder215.mcsurvivors.MCSCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_19_R3.CraftServer;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

import static me.gamercoder215.mcsurvivors.MCSCore.getPluginLogger;
import static me.gamercoder215.mcsurvivors.MCSCore.print;

public final class MCSBiomeManager {

    public static void changeRegistryLock(boolean isLocked) {
        MappedRegistry<Biome> materials = getRegistry(Registries.BIOME);

        try {
            Field isFrozen = materials.getClass().getDeclaredField("ca");
            isFrozen.setAccessible(true);
            isFrozen.set(materials, isLocked);
        } catch (Exception e) {
            getPluginLogger().info("Error changing biome lock to " + isLocked);
            print(e);
        }
    }

    public static <T> MappedRegistry<T> getRegistry(ResourceKey<Registry<T>> key) {
        DedicatedServer server = ((CraftServer) Bukkit.getServer()).getServer();
        return (MappedRegistry<T>) server.registryAccess().registry(key).orElseThrow(AssertionError::new);
    }

    @NotNull
    public static void placeBiome(@NotNull MCSBiome b, Chunk c, boolean update) {
        LevelChunk nmsChunk = ((CraftChunk) c).getHandle();
        Holder<Biome> holder = b.getHolder();

        for (int x = 0; x < 16; x++)
            for (int y = c.getWorld().getMinHeight(); y <= c.getWorld().getMaxHeight(); y++)
                for (int z = 0; z < 16; z++) nmsChunk.setBiome(x >> 2, y >> 2, z >> 2, holder);

        b.save();

        if (update) updateChunk(c);
    }

    @NotNull
    public static void placeBiome(@NotNull MCSBiome b, Location loc, boolean update) {
        if (!isRegistered(b)) throw new IllegalArgumentException("Biome " + b.getName() + " is not registered!");

        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        BlockPos pos = new BlockPos(x, 0, z);
        ServerLevel w = ((CraftWorld) loc.getWorld()).getHandle();

        Holder<Biome> holder = b.getHolder();
        LevelChunk nmsChunk = w.getChunkAt(pos);

        for (int y = loc.getWorld().getMinHeight(); y <= loc.getWorld().getMaxHeight(); y++)
            nmsChunk.setBiome(x >> 2, y >> 2, z >> 2, holder);

        if (update) updateChunk(loc.getChunk());
    }

    public static void updateChunk(@NotNull Chunk c) {
        LevelChunk nms = ((CraftChunk) c).getHandle();
        for (Player p : c.getWorld().getPlayers()) {
            Location l = p.getLocation();
            ServerPlayer sp = ((CraftPlayer) p).getHandle();
            if (l.distance(c.getBlock(0, 0, 0).getLocation()) < Bukkit.getServer().getViewDistance() * 16)
                sp.connection.send(new ClientboundLevelChunkWithLightPacket(nms, nms.getLevel().getLightEngine(), null, null, true));
        }
    }

    public static void registerBiomes() {
        registerBiomes(true);
    }

    public static void registerBiomes(boolean log) {
        MCSCore plugin = JavaPlugin.getPlugin(MCSCore.class);

        changeRegistryLock(false);

        int i = 0;
        for (MCSBiome biome : MCSBiome.getAllBiomes()) {
            if (isRegistered(biome)) continue;

            registerBiome(biome);
            i++;
        }

        changeRegistryLock(true);
        if (log) plugin.getLogger().info("Registered " + i + " new biomes");
    }

    public static boolean isRegistered(@NotNull MCSBiome biome) {
        return isRegistered(biome.getResourceKey());
    }

    public static boolean isRegistered(@NotNull ResourceKey<Biome> key) {
        try {
            Holder<Biome> holder = getRegistry(Registries.BIOME).getHolderOrThrow(key);
            return holder != null;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public static void registerBiome(@NotNull MCSBiome biome) {
        MappedRegistry<Biome> reg = getRegistry(Registries.BIOME);
        ResourceKey<Biome> key = biome.getResourceKey();

        Biome forest = reg.get(Biomes.FOREST);
        Biome.BiomeBuilder builder = new Biome.BiomeBuilder();

        builder.mobSpawnSettings(forest.getMobSettings());
        builder.generationSettings(forest.getGenerationSettings());
        builder.downfall(0.8F);
        builder.temperature(0.7F);
        builder.temperatureAdjustment(biome.isFrozen() ? Biome.TemperatureModifier.FROZEN : Biome.TemperatureModifier.NONE);

        BiomeSpecialEffects.Builder effectB = new BiomeSpecialEffects.Builder();
        effectB.grassColorModifier(BiomeSpecialEffects.GrassColorModifier.NONE);
        effectB.fogColor(Integer.parseInt(biome.getFogColor(), 16));
        effectB.waterFogColor(Integer.parseInt(biome.getFogColor(), 16));
        effectB.waterColor(Integer.parseInt(biome.getWaterColor(), 16));
        effectB.skyColor(Integer.parseInt(biome.getSkyColor(), 16));

        effectB.foliageColorOverride(Integer.parseInt(biome.getFoliageColor(), 16));
        effectB.grassColorOverride(Integer.parseInt(biome.getGrassColor(), 16));

        builder.specialEffects(effectB.build());

        Biome nmsBiome = builder.build();
        reg.register(key, nmsBiome, Lifecycle.stable());
    }

}
