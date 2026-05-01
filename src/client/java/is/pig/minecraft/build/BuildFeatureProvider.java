package is.pig.minecraft.build;

import is.pig.minecraft.api.spi.FeatureProvider;
import is.pig.minecraft.api.spi.RenderPipelineAdapter;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.build.config.ConfigPersistence;
import is.pig.minecraft.build.mvc.controller.*;
import is.pig.minecraft.build.mlg.telemetry.MlgAttemptEvent;
import is.pig.minecraft.lib.util.telemetry.EventTranslatorRegistry;
import is.pig.minecraft.api.ResourceLocation;

import java.util.Collection;
import java.util.List;

public class BuildFeatureProvider implements FeatureProvider {

    private final DirectionalPlacementHandler directionalPlacementHandler = new DirectionalPlacementHandler();
    private final FastBreakHandler fastBreakHandler = new FastBreakHandler();
    private final AutoMlgHandler autoMlgHandler = new AutoMlgHandler();
    private final AutoParkourHandler autoParkourHandler = new AutoParkourHandler();
    private final LightLevelOverlayHandler lightLevelOverlayHandler = new LightLevelOverlayHandler();
    private final ShapeMenuHandler shapeMenuHandler = new ShapeMenuHandler();

    @Override
    public void onInitialize() {
        ConfigPersistence.load();
        is.pig.minecraft.build.mlg.telemetry.MlgHistoryManager.init();

        EventTranslatorRegistry.getInstance().register(MlgAttemptEvent.class, (event, i18n) -> 
            i18n.translate("piggy.build.telemetry.mlg_attempt", 
                    event.methodName(), 
                    event.fallDistance(), 
                    event.isFatal(), 
                    event.impactTicks())
        );
    }

    @Override
    public void onTick(Object client) {
        directionalPlacementHandler.onTick(client);
        fastBreakHandler.onTick(client);
        autoMlgHandler.onTick(client);
        autoParkourHandler.onTick(client);
        lightLevelOverlayHandler.onTick(client);
        shapeMenuHandler.onTick(client);
    }

    @Override
    public void onRender(Object client, Object stack, float partialTicks) {
        RenderPipelineAdapter renderer = PiggyServiceRegistry.getRenderPipelineAdapters().stream().findFirst().orElse(null);
        if (renderer == null) return;

        renderer.renderLightLevel(client, stack, partialTicks);
        renderer.renderBuildSession(client, stack, partialTicks);
        renderer.renderPlacementSession(client, stack, partialTicks);
    }

    @Override
    public String getFeatureId() {
        return "piggy-build";
    }
}
