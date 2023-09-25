package me.gamercoder215.mcsurvivors.biome;

import me.gamercoder215.mcsurvivors.MCSCore;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import org.apache.commons.io.FileUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static me.gamercoder215.mcsurvivors.MCSCore.print;
import static me.gamercoder215.mcsurvivors.biome.MCSBiomeManager.getRegistry;

public final class MCSBiome {

    public static final boolean DEFAULT_FROZEN = false;
    public static final String DEFAULT_WATER_COLOR = "3F76E4";
    public static final String DEFAULT_FOG_COLOR = "C0D8FF";
    public static final String DEFAULT_SKY_COLOR = "78A7FF";
    public static final String DEFAULT_GRASS_COLOR = "91BD59";
    public static final String DEFAULT_FOLIAGE_COLOR = "77AB2F";

    public static final Set<MCSBiome> BIOME_CACHE = new HashSet<>();

    private final File folder;
    private final UUID id;

    private final String name;
    private boolean frozen = DEFAULT_FROZEN;
    private String waterColor = DEFAULT_WATER_COLOR;
    private String fogColor = DEFAULT_FOG_COLOR;
    private String skyColor = DEFAULT_SKY_COLOR;
    private String grassColor = DEFAULT_GRASS_COLOR;
    private String foliageColor = DEFAULT_FOLIAGE_COLOR;

    private MCSBiome(File folder, @NotNull UUID id, @NotNull String name) {
        this.folder = folder;
        this.name = name;
        this.id = id;
    }

    public File getFolder() {
        return folder;
    }

    public UUID getId() {
        return id;
    }

    public String getGrassColor() {
        return grassColor;
    }

    public String getName() {
        return name;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public String getWaterColor() {
        return waterColor;
    }

    public String getFogColor() {
        return fogColor;
    }

    public String getSkyColor() {
        return skyColor;
    }

    public String getFoliageColor() {
        return foliageColor;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
        save();
    }

    public void setWaterColor(String waterColor) {
        this.waterColor = waterColor;
        save();
    }

    public void setFogColor(String fogColor) {
        this.fogColor = fogColor;
        save();
    }

    public void setSkyColor(String skyColor) {
        this.skyColor = skyColor;
        save();
    }

    public void setGrassColor(String grassColor) {
        this.grassColor = grassColor;
        save();
    }

    public void setFoliageColor(String foliageColor) {
        this.foliageColor = foliageColor;
        save();
    }

    @NotNull
    public ResourceLocation getResourceLocation() {
        return new ResourceLocation("mcsurvivors", getId().toString().substring(0, 8));
    }

    public ResourceKey<Biome> getResourceKey() {
        return ResourceKey.create(Registries.BIOME, getResourceLocation());
    }

    public Holder<Biome> getHolder() {
        return getRegistry(Registries.BIOME).getHolderOrThrow(getResourceKey());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MCSBiome that = (MCSBiome) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // Static Methods

    @NotNull
    public static Builder builder(String name) {
        return new Builder(name);
    }

    @NotNull
    public static Set<MCSBiome> getAllBiomes() {
        if (!BIOME_CACHE.isEmpty()) return BIOME_CACHE;
        Set<MCSBiome> biomes = new HashSet<>();
        if (MCSCore.getBiomesFolder().listFiles() == null) return biomes;

        for (File folder : MCSCore.getBiomesFolder().listFiles()) {
            if (!folder.isDirectory()) continue;

            try {
                biomes.add(read(folder));
            } catch (IOException | ReflectiveOperationException e) {
                print(e);
            } catch (IllegalStateException ignored) {}
        }

        BIOME_CACHE.addAll(biomes);

        return biomes;
    }

    @Nullable
    public static MCSBiome byId(@NotNull UUID id) {
        return getAllBiomes()
                .stream()
                .filter(b -> b.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public static MCSBiome byName(@NotNull String name) {
        return getAllBiomes()
                .stream()
                .filter(b -> b.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public static void removeBiome(@NotNull MCSBiome biome) {
        if (biome == null) return;

        File folder = biome.getFolder();
        try {
            FileUtils.deleteDirectory(folder);
        } catch (IOException e) {
            print(e);
        }

        MCSBiome.BIOME_CACHE.clear();
        MCSBiome.getAllBiomes();
    }

    public static boolean existsName(@NotNull String name) {
        return getAllBiomes()
                .stream()
                .anyMatch(b -> b.getName().equalsIgnoreCase(name));
    }

    // Reading & Writing

    public void save() {
        if (!folder.exists()) folder.mkdir();

        try {
            write();
        } catch (IOException e) {
            print(e);
        }
    }

    private void write() throws IOException {
        // ID

        File info = new File(folder, "info.dat");
        if (!info.exists()) info.createNewFile();

        ObjectOutputStream infoOs = new ObjectOutputStream(new FileOutputStream(info));
        infoOs.writeObject(id);
        infoOs.writeObject(name);
        infoOs.close();

        // Biome Data

        File biome = new File(folder, "data.yml");
        if (!biome.exists()) biome.createNewFile();

        FileConfiguration biomeC = YamlConfiguration.loadConfiguration(biome);
        biomeC.set("frozen", frozen);
        biomeC.set("grassColor", grassColor);
        biomeC.set("waterColor", waterColor);
        biomeC.set("fogColor", fogColor);
        biomeC.set("skyColor", skyColor);
        biomeC.set("foliageColor", foliageColor);
        biomeC.save(biome);
    }

    @NotNull
    private static MCSBiome read(File folder) throws IllegalStateException, IOException, ReflectiveOperationException {
        File info = new File(folder, "info.dat");
        if (!info.exists()) throw new IllegalStateException("Could not find: info.dat");

        ObjectInputStream infoIs = new ObjectInputStream(new FileInputStream(info));
        UUID id = (UUID) infoIs.readObject();
        String name = (String) infoIs.readObject();
        infoIs.close();

        MCSBiome b = new MCSBiome(folder, id, name);

        File biome = new File(folder, "data.yml");
        if (!biome.exists()) throw new IllegalStateException("Could not find: data.yml");

        FileConfiguration biomeC = YamlConfiguration.loadConfiguration(biome);

        b.frozen = biomeC.getBoolean("frozen", DEFAULT_FROZEN);

        b.grassColor = biomeC.getString("grassColor", DEFAULT_GRASS_COLOR);
        b.waterColor = biomeC.getString("waterColor", DEFAULT_WATER_COLOR);
        b.fogColor = biomeC.getString("fogColor", DEFAULT_FOG_COLOR);
        b.skyColor = biomeC.getString("skyColor", DEFAULT_SKY_COLOR);
        b.foliageColor = biomeC.getString("foliageColor", DEFAULT_FOLIAGE_COLOR);

        return b;
    }

    // Builder

    public static final class Builder {

        String name;
        boolean frozen = false;
        String grassColor = DEFAULT_GRASS_COLOR;
        String waterColor = DEFAULT_WATER_COLOR;
        String fogColor = DEFAULT_FOG_COLOR;
        String skyColor = DEFAULT_SKY_COLOR;
        String foliageColor = DEFAULT_FOLIAGE_COLOR;

        Builder(String name) {
            this.name = name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Builder setFrozen(boolean frozen) {
            this.frozen = frozen;
            return this;
        }

        public Builder setGrassColor(String grassColor) {
            this.grassColor = grassColor;
            return this;
        }

        public Builder setWaterColor(String waterColor) {
            this.waterColor = waterColor;
            return this;
        }

        public Builder setFogColor(String fogColor) {
            this.fogColor = fogColor;
            return this;
        }

        public Builder setSkyColor(String skyColor) {
            this.skyColor = skyColor;
            return this;
        }

        public Builder setFoliageColor(String foliageColor) {
            this.foliageColor = foliageColor;
            return this;
        }

        @NotNull
        public MCSBiome build() {
            UUID id = UUID.randomUUID();
            File folder = new File(MCSCore.getBiomesFolder(), id.toString());
            folder.mkdir();

            MCSBiome b = new MCSBiome(folder, id, name);

            b.frozen = frozen;
            b.grassColor = grassColor;
            b.waterColor = waterColor;
            b.fogColor = fogColor;
            b.skyColor = skyColor;
            b.foliageColor = foliageColor;
            b.save();

            BIOME_CACHE.clear();
            MCSBiome.getAllBiomes();

            return b;
        }
    }

}
