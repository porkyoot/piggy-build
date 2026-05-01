package is.pig.minecraft.build;

import is.pig.minecraft.api.*;
import is.pig.minecraft.build.config.ConfigPersistence;
import is.pig.minecraft.build.mvc.controller.InputController;
import is.pig.minecraft.build.mlg.telemetry.MlgAttemptEvent;
import is.pig.minecraft.lib.util.telemetry.EventTranslatorRegistry;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PiggyBuildClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("piggy-build");
    private final InputController controller = new InputController();

    @Override
    public void onInitializeClient() {
        LOGGER.info("Ehlo from Piggy Build!");

        // 0. Initialize Telemetry & History
        is.pig.minecraft.build.mlg.telemetry.MlgHistoryManager.init();

        // 1. Load configuration
        ConfigPersistence.load();

        // 2. Initialize controller
        controller.initialize();

        // 3. Register Anti-Cheat HUD Overlay (Delegated to lib)
        is.pig.minecraft.lib.ui.AntiCheatHudOverlay.register();

        // 4. Register Config Sync Listener
        is.pig.minecraft.build.config.PiggyBuildConfig.getInstance()
                .registerConfigSyncListener((allowCheatsStart, featuresStart) -> {
                    Boolean allowCheats = (Boolean) allowCheatsStart;
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Boolean> features = (java.util.Map<String, Boolean>) featuresStart;

                    is.pig.minecraft.build.config.PiggyBuildConfig buildConfig = is.pig.minecraft.build.config.PiggyBuildConfig
                            .getInstance();
                    buildConfig.serverAllowCheats = allowCheats;
                    buildConfig.serverFeatures = features;

                    if (!allowCheats) {
                        buildConfig.setFastPlaceEnabled(false);
                        buildConfig.setFlexiblePlacementEnabled(false);
                    }

                    if (features != null) {
                        if (features.containsKey("fast_place") && !Boolean.TRUE.equals(features.get("fast_place"))) {
                            buildConfig.setFastPlaceEnabled(false);
                        }
                        if (features.containsKey("flexible_placement")
                                && !Boolean.TRUE.equals(features.get("flexible_placement"))) {
                            buildConfig.setFlexiblePlacementEnabled(false);
                        }
                    }

                    LOGGER.debug("[ANTI-CHEAT DEBUG] PiggyBuildConfig updated from server sync");
                });

        // 5. Register Telemetry Translators
        EventTranslatorRegistry.getInstance().register(MlgAttemptEvent.class, (event, i18n) -> 
            i18n.translate("piggy.build.telemetry.mlg_attempt", 
                    event.methodName(), 
                    event.fallDistance(), 
                    event.isFatal(), 
                    event.impactTicks())
        );
    }
}