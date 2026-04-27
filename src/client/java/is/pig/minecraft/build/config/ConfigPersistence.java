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
import is.pig.minecraft.lib.config.PiggyConfigManager;

/**
 * Handles loading and saving of the application configuration.
 * Extends the universal PiggyConfigManager.
 */
public class ConfigPersistence extends PiggyConfigManager<PiggyBuildConfig> {

    private static final ConfigPersistence INSTANCE = new ConfigPersistence();

    private ConfigPersistence() {
        super("piggy-build.json", PiggyBuildConfig.class, "piggy-build");
    }

    @Override
    protected PiggyBuildConfig getConfigInstance() {
        return PiggyBuildConfig.getInstance();
    }

    @Override
    protected void setConfigInstance(PiggyBuildConfig instance) {
        PiggyBuildConfig.setInstance(instance);
    }

    /**
     * Loads the configuration from disk.
     */
    public static void load() {
        INSTANCE.load();
    }

    /**
     * Saves the current configuration to disk.
     */
    public static void save() {
        INSTANCE.save();
    }
}

