package is.pig.minecraft.build.mlg.telemetry;

import is.pig.minecraft.lib.util.telemetry.JsonHistoryStore;

/**
 * Manages the persistence of MLG survival attempts to the piggy-mlg.json history store.
 */
public class MlgHistoryManager {
    private static JsonHistoryStore mlgStore;

    public static void init() {
        mlgStore = new JsonHistoryStore("piggy-mlg.json", event -> 
            event instanceof MlgAttemptEvent || event.getEventKey().contains("mlg")
        );
        mlgStore.register();
    }

    public static JsonHistoryStore getStore() {
        return mlgStore;
    }
}
