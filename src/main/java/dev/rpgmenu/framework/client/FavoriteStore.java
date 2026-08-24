package dev.rpgmenu.framework.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.rpgmenu.framework.RpgMenuFramework;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

/** Per-server UI metadata. It never mutates the ItemStack. */
public final class FavoriteStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private final Set<String> favorites;

    private FavoriteStore(Path file, Set<String> favorites) { this.file = file; this.favorites = favorites; }

    public static FavoriteStore open(Minecraft minecraft) {
        String server = minecraft.getCurrentServer() == null ? "singleplayer" : minecraft.getCurrentServer().ip;
        String name = sha256(server == null ? "unknown" : server);
        Path directory = FMLPaths.CONFIGDIR.get().resolve(RpgMenuFramework.MOD_ID).resolve("favorites").normalize();
        Path file = directory.resolve(name + ".json").normalize();
        if (!file.startsWith(directory)) return new FavoriteStore(directory.resolve("fallback.json"), new HashSet<>());
        try {
            if (Files.isRegularFile(file) && Files.size(file) <= 1_048_576) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    Set<String> values = GSON.fromJson(reader, new TypeToken<Set<String>>() {}.getType());
                    return new FavoriteStore(file, values == null ? new HashSet<>() : new HashSet<>(values));
                }
            }
        } catch (Exception exception) {
            RpgMenuFramework.LOGGER.warn("Could not read favorites metadata", exception);
        }
        return new FavoriteStore(file, new HashSet<>());
    }

    public boolean contains(ItemStack stack) { return favorites.contains(key(stack)); }
    public void toggle(ItemStack stack) {
        String key = key(stack);
        if (!favorites.remove(key)) favorites.add(key);
        save();
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) { GSON.toJson(favorites, writer); }
        } catch (Exception exception) {
            RpgMenuFramework.LOGGER.warn("Could not save favorites metadata", exception);
        }
    }

    private static String key(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + "@" + Integer.toUnsignedString(ItemStack.hashItemAndComponents(stack), 16);
    }

    private static String sha256(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest) result.append(String.format("%02x", value));
            return result.toString();
        } catch (Exception impossible) {
            return Integer.toUnsignedString(input.hashCode(), 16);
        }
    }
}
