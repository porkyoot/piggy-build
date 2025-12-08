package is.pig.minecraft.build.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class PiggyServerConfig {
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("piggy-build-server.json")
            .toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static PiggyServerConfig INSTANCE;

    public boolean allowCheats = true;

    public static PiggyServerConfig getInstance() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, PiggyServerConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
                INSTANCE = new PiggyServerConfig();
            }
        } else {
            INSTANCE = new PiggyServerConfig();
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
}
