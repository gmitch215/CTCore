package me.gamercoder215.mcsurvivors.biome;

import com.mojang.serialization.Lifecycle;
import me.gamercoder215.mcsurvivors.MCSCore;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.Holder;
import net.minecraft.core.IRegistry;
import net.minecraft.core.RegistryMaterials;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeFog;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkStatus;
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
        RegistryMaterials<BiomeBase> materials = getRegistry(Registries.an);

        try {
            Field isFrozen = materials.getClass().getDeclaredField("ca");
            isFrozen.setAccessible(true);
            isFrozen.set(materials, isLocked);
        } catch (Exception e) {
            getPluginLogger().info("Error changing biome lock to " + isLocked);
            print(e);
        }
    }

    public static <T> RegistryMaterials<T> getRegistry(ResourceKey<IRegistry<T>> key) {
        DedicatedServer server = ((CraftServer) Bukkit.getServer()).getServer();
        return (RegistryMaterials<T>) server.aX().c(key).orElseThrow(AssertionError::new);
    }

    @NotNull
    public static void placeBiome(@NotNull MCSBiome b, Chunk c, boolean update) {
        net.minecraft.world.level.chunk.Chunk nms = (net.minecraft.world.level.chunk.Chunk) ((CraftChunk) c).getHandle(ChunkStatus.f);
        Holder<BiomeBase> holder = b.getHolder();

        for (int x = 0; x < 16; x++)
            for (int y = c.getWorld().getMinHeight(); y <= c.getWorld().getMaxHeight(); y++)
                for (int z = 0; z < 16; z++) nms.setBiome(x >> 2, y >> 2, z >> 2, holder);

        b.save();

        if (update) updateChunk(c);
    }

    @NotNull
    public static void placeBiome(@NotNull MCSBiome b, Location loc, boolean update) {
        if (!isRegistered(b)) throw new IllegalArgumentException("Biome " + b.getName() + " is not registered!");

        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        BlockPosition pos = new BlockPosition(x, 0, z);
        WorldServer w = ((CraftWorld) loc.getWorld()).getHandle();

        Holder<BiomeBase> holder = b.getHolder();
        net.minecraft.world.level.chunk.Chunk nmsChunk = w.l(pos);

        for (int y = loc.getWorld().getMinHeight(); y <= loc.getWorld().getMaxHeight(); y++)
            nmsChunk.setBiome(x >> 2, y >> 2, z >> 2, holder);

        if (update) updateChunk(loc.getChunk());
    }

    public static void updateChunk(@NotNull Chunk c) {
        net.minecraft.world.level.chunk.Chunk nms = (net.minecraft.world.level.chunk.Chunk) ((CraftChunk) c).getHandle(ChunkStatus.f);
        for (Player p : c.getWorld().getPlayers()) {
            Location l = p.getLocation();
            EntityPlayer sp = ((CraftPlayer) p).getHandle();
            if (l.distance(c.getBlock(0, 0, 0).getLocation()) < Bukkit.getServer().getViewDistance() * 16)
                sp.b.a(new ClientboundLevelChunkWithLightPacket(nms, nms.D().l_(), null, null, true));
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

    public static boolean isRegistered(@NotNull ResourceKey<BiomeBase> key) {
        try {
            Holder<BiomeBase> holder = getRegistry(Registries.an).f(key);
            return holder != null;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public static void registerBiome(@NotNull MCSBiome biome) {
        RegistryMaterials<BiomeBase> reg = getRegistry(Registries.an);
        ResourceKey<BiomeBase> key = biome.getResourceKey();

        BiomeBase forest = reg.a(Biomes.i);
        BiomeBase.a builder = new BiomeBase.a();

        builder.a(forest.b());
        builder.a(forest.d());
        builder.b(0.8F);
        builder.a(0.7F);
        builder.a(biome.isFrozen() ? BiomeBase.TemperatureModifier.b : BiomeBase.TemperatureModifier.a);

        BiomeFog.a effectB = new BiomeFog.a();
        effectB.a(BiomeFog.GrassColor.a);
        effectB.a(Integer.parseInt(biome.getFogColor(), 16));
        effectB.c(Integer.parseInt(biome.getFogColor(), 16));
        effectB.b(Integer.parseInt(biome.getWaterColor(), 16));
        effectB.d(Integer.parseInt(biome.getSkyColor(), 16));

        effectB.e(Integer.parseInt(biome.getFoliageColor(), 16));
        effectB.f(Integer.parseInt(biome.getGrassColor(), 16));

        builder.a(effectB.a());

        BiomeBase nmsBiome = builder.a();
        reg.a(key, nmsBiome, Lifecycle.stable());
    }

}
