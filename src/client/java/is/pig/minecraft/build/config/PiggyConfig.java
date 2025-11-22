package is.pig.minecraft.build.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.loader.api.FabricLoader;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class PiggyConfig {

    // Initialize INSTANCE with default values immediately to avoid NPEs
    private static PiggyConfig INSTANCE = new PiggyConfig();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("piggy-build.json").toFile();

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Color.class, new ColorTypeAdapter())
            .create();

    // --- CONFIG FIELDS ---
    private Color highlightColor = new Color(0, 255, 230, 100); // Teal default

    // --- GETTERS / SETTERS ---
    public static PiggyConfig getInstance() {
        // Ensure we try to load if it hasn't been loaded, 
        // but never return null (INSTANCE is initialized at class load)
        return INSTANCE;
    }

    public Color getHighlightColor() {
        return highlightColor;
    }

    public void setHighlightColor(Color highlightColor) {
        this.highlightColor = highlightColor;
    }

    public float getRedFloat() { return highlightColor.getRed() / 255.0f; }
    public float getGreenFloat() { return highlightColor.getGreen() / 255.0f; }
    public float getBlueFloat() { return highlightColor.getBlue() / 255.0f; }
    public float getAlphaFloat() { return highlightColor.getAlpha() / 255.0f; }

    // --- PERSISTENCE ---
    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                PiggyConfig loaded = GSON.fromJson(reader, PiggyConfig.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                }
            } catch (IOException e) {
                e.printStackTrace();
                // If load fails, we keep the default INSTANCE
            }
        } else {
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- COLOR ADAPTER ---
    public static class ColorTypeAdapter extends TypeAdapter<Color> {
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