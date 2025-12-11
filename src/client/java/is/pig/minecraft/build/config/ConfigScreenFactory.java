package is.pig.minecraft.build.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.awt.Color;

public class ConfigScreenFactory {

    public static Screen create(Screen parent) {
        PiggyBuildConfig config = PiggyBuildConfig.getInstance();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Piggy Build Configuration"))

                // SAFETY CATEGORY
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Safety"))
                        .tooltip(is.pig.minecraft.lib.I18n.safetyTooltip())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("No Cheating Mode"))
                                .description(OptionDescription.of(
                                        Component.literal("Prevents usage of cheat features in Survival/Adventure mode."),
                                        Component.literal(""),
                                        Component.literal("When enabled, utility features are disabled unless you are in Creative mode."),
                                        Component.literal("This option is locked if the server enforces anti-cheat.")))
                                .binding(
                                        true,
                                        config::isNoCheatingMode,
                                        config::setNoCheatingMode)
                                .controller(TickBoxControllerBuilder::create)
                                .available(config.isGlobalCheatsEditable()) // Gray out if server enforces rules
                                .build())
                        .build())

                // VISUALS CATEGORY
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Visuals"))
                        .tooltip(Component.literal("Customize how the shape selector looks."))
                        .option(Option.<Color>createBuilder()
                                .name(Component.literal("Shape Selector Highlight Color"))
                                .binding(
                                        new Color(0, 255, 230, 100),
                                        config::getHighlightColor,
                                        config::setHighlightColor)
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true))
                                .build())
                        .option(Option.<Color>createBuilder()
                                .name(Component.literal("Placement Overlay Color"))
                                .binding(
                                        new Color(0, 255, 230, 100),
                                        config::getPlacementOverlayColor,
                                        config::setPlacementOverlayColor)
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true))
                                .build())
                        .build())

                // FLEXIBLE PLACEMENT CATEGORY
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Flexible Placement"))
                        .tooltip(Component.literal("Settings for Directional and Diagonal placement"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Enable Flexible Placement"))
                                .description(OptionDescription.of(
                                        Component.literal("Master switch for Directional and Diagonal placement."),
                                        Component.literal(""),
                                        Component.literal("If Anti-Cheat is active, this cannot be enabled.")))
                                .available(config.isFlexiblePlacementEditable()) // Gray out if enforced
                                .binding(
                                        true,
                                        config::isFlexiblePlacementEnabled,
                                        config::setFlexiblePlacementEnabled)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .build())

                // FAST PLACEMENT CATEGORY
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Fast Placement"))
                        .tooltip(Component.literal("Configure fast block placement settings"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Enable Fast Place/Break"))
                                .description(OptionDescription.of(
                                        Component.literal("Enables fast placement and breaking features."),
                                        Component.literal("If Anti-Cheat is active, this cannot be enabled.")))
                                .available(config.isFastPlaceEditable()) // Gray out if enforced
                                .binding(
                                        false,
                                        config::isFastPlaceEnabled,
                                        config::setFastPlaceEnabled)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.literal("Placement Delay"))
                                .binding(
                                        100,
                                        config::getFastPlaceDelayMs,
                                        config::setFastPlaceDelayMs)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                        .range(50, 500)
                                        .step(10)
                                        .formatValue(value -> Component.literal(value + " ms")))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.literal("Break Delay"))
                                .binding(
                                        150,
                                        config::getFastBreakDelayMs,
                                        config::setFastBreakDelayMs)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                        .range(50, 500)
                                        .step(10)
                                        .formatValue(value -> Component.literal(value + " ms")))
                                .build())
                        .build())

                .save(ConfigPersistence::save)
                .build()
                .generateScreen(parent);
    }
}