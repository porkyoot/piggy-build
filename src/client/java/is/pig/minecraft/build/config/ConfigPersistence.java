package is.pig.minecraft.build.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Handles loading and saving of the application configuration.
 * Follows the Single Responsibility Principle by decoupling persistence from
 * the data model.
 */
public class ConfigPersistence {

    private static final Logger LOGGER = LoggerFactory.getLogger("piggy-build");
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("piggy-build.json")
            .toFile();
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Color.class, new ColorTypeAdapter())
            .registerTypeHierarchyAdapter(java.util.function.BiConsumer.class,
                    new TypeAdapter<java.util.function.BiConsumer<?, ?>>() {
                        @Override
                        public void write(JsonWriter out, java.util.function.BiConsumer<?, ?> value)
                                throws IOException {
                            out.beginObject().endObject();
                        }

                        @Override
                        public java.util.function.BiConsumer<?, ?> read(JsonReader in) throws IOException {
                            in.skipValue();
                            return null;
                        }
                    })
            .setExclusionStrategies(new com.google.gson.ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(com.google.gson.FieldAttributes f) {
                    return f.getName().equals("syncListeners");
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            })
            .create();

    /**
     * Loads the configuration from disk.
     * Use {@link PiggyBuildConfig#getInstance()} to access the current config.
     */
    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                PiggyBuildConfig loaded = GSON.fromJson(reader, PiggyBuildConfig.class);
                if (loaded != null) {
                    PiggyBuildConfig.setInstance(loaded);
                    LOGGER.debug("Configuration loaded successfully.");
                    LOGGER.info("[DEBUG] Loaded No Cheating Mode: {}", loaded.isNoCheatingMode());
                }
            } catch (com.google.gson.JsonSyntaxException | com.google.gson.JsonIOException e) {
                LOGGER.error("Failed to parse configuration file: {}", CONFIG_FILE.getAbsolutePath(), e);
                // Throwing a RuntimeException with a clear message to inform the user
                throw new RuntimeException("PiggyBuild Config Error: The configuration file '" + CONFIG_FILE.getName()
                        + "' is malformed. Please fix the syntax or delete the file to regenerate it. Details: "
                        + e.getMessage(), e);
            } catch (IOException e) {
                LOGGER.error("Failed to load configuration", e);
            }
        } else {
            save(); // Create default
        }
    }

    /**
     * Saves the current configuration to disk.
     */
    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(PiggyBuildConfig.getInstance(), writer);
            LOGGER.debug("Configuration saved successfully.");
        } catch (IOException e) {
            LOGGER.error("Failed to save configuration", e);
        }
    }

    /**
     * Custom GSON TypeAdapter for java.awt.Color.
     */
    private static class ColorTypeAdapter extends TypeAdapter<Color> {
        @Override
        public void write(JsonWriter out, Color value) throws IOException {
            out.beginObject();
            out.name("red").value(value.getRed());
            out.name("green").value(value.getGreen());
            out.name("blue").value(value.getBlue());
            out.name("alpha").value(value.getAlpha());
            out.endObject();
        }

        @Override
        public Color read(JsonReader in) throws IOException {
            in.beginObject();
            int r = 0, g = 0, b = 0, a = 255;
            while (in.hasNext()) {
                switch (in.nextName()) {
                    case "red" -> r = in.nextInt();
                    case "green" -> g = in.nextInt();
                    case "blue" -> b = in.nextInt();
                    case "alpha" -> a = in.nextInt();
                    default -> in.skipValue();
                }
            }
            in.endObject();
            return new Color(r, g, b, a);
        }
    }
}
