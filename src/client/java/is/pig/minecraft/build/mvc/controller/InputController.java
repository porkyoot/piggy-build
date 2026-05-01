package is.pig.minecraft.build.mvc.controller;
import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.InputAdapter;

public class InputController {

    private final ShapeMenuHandler menuHandler = new ShapeMenuHandler();
    private static DirectionalPlacementHandler placementHandler = new DirectionalPlacementHandler();
    private static final FastPlacementHandler fastPlacementHandler = new FastPlacementHandler();
    private final FastBreakHandler fastBreakHandler = new FastBreakHandler();
    private static final LightLevelOverlayHandler lightLevelOverlayHandler = new LightLevelOverlayHandler();
    private static final AutoParkourHandler autoParkourHandler = new AutoParkourHandler();
    private static final AutoMlgHandler autoMlgHandler = new AutoMlgHandler();

    public static FastPlacementHandler getFastPlacementHandler() {
        return fastPlacementHandler;
    }

    public static LightLevelOverlayHandler getLightLevelOverlayHandler() {
        return lightLevelOverlayHandler;
    }

    public void initialize() {
        InputAdapter input = PiggyServiceRegistry.getInputAdapter();
        input.registerKey("piggy-build:shape_selector", "GLFW_KEY_V", "Piggy Build");
        input.registerKey("piggy-build:directional", "GLFW_MOUSE_BUTTON_5", "Piggy Build");
        input.registerKey("piggy-build:diagonal", "GLFW_MOUSE_BUTTON_4", "Piggy Build");
        input.registerKey("piggy-build:fast_place", "GLFW_MOUSE_BUTTON_6", "Piggy Build");
        input.registerKey("piggy-build:light_level", "GLFW_KEY_L", "Piggy Build");
        input.registerKey("piggy-build:auto_parkour", "GLFW_KEY_P", "Piggy Build");
        input.registerKey("piggy-build:auto_mlg", "GLFW_KEY_M", "Piggy Build");
    }

    public void onTick(Object client) {
        menuHandler.onTick(client);
        placementHandler.onTick(client);
        fastPlacementHandler.onTick(client);
        fastBreakHandler.onTick(client);
        ShapePlacementHandler.onTick(client);
        lightLevelOverlayHandler.onTick(client);
        autoParkourHandler.onTick(client);
        autoMlgHandler.onTick(client);
    }

    public static DirectionalPlacementHandler getDirectionalPlacementHandler() {
        return placementHandler;
    }
}