package is.pig.minecraft.build.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import is.pig.minecraft.build.api.IPlatformEnvironment;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ServiceLoader;

public class ConfigPersistence {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static File getConfigFile(String fileName) {
        IPlatformEnvironment env = ServiceLoader.load(IPlatformEnvironment.class).findFirst()
            .orElseThrow(() -> new IllegalStateException("No IPlatformEnvironment found"));
        return env.getConfigDirectory().resolve(fileName).toFile();
    }

    public static <T> T load(String fileName, Class<T> configClass, T defaultInstance) {
        File file = getConfigFile(fileName);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                T loaded = GSON.fromJson(reader, configClass);
                return loaded != null ? loaded : defaultInstance;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return defaultInstance;
    }

    public static <T> void save(String fileName, T configInstance) {
        File file = getConfigFile(fileName);
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(configInstance, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
