package is.pig.minecraft.build.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
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
                                                .option(is.pig.minecraft.lib.ui.NoCheatingModeOption.create(
                                                                parent,
                                                                config::isNoCheatingMode,
                                                                config::setNoCheatingMode,
                                                                ConfigPersistence::save))
                                                .build())

                                // VISUALS CATEGORY
                                .category(ConfigCategory.createBuilder()
                                                .name(Component.literal("Visuals"))
                                                .tooltip(Component.literal("Customize how the shape selector looks."))

                                                .option(Option.<Color>createBuilder()
                                                                .name(Component.literal(
                                                                                "Shape Selector Highlight Color"))
                                                                .description(OptionDescription.of(
                                                                                Component.literal(
                                                                                                "The color and opacity of the shape selector.")))
                                                                .binding(
                                                                                new Color(0, 255, 230, 100),
                                                                                config::getHighlightColor,
                                                                                config::setHighlightColor)
                                                                .controller(opt -> ColorControllerBuilder.create(opt)
                                                                                .allowAlpha(true))
                                                                .build())

                                                .option(Option.<Color>createBuilder()
                                                                .name(Component.literal("Placement Overlay Color"))
                                                                .description(OptionDescription.of(
                                                                                Component.literal(
                                                                                                "The color and opacity of the directional placement overlay.")))
                                                                .binding(
                                                                                new Color(0, 255, 230, 100),
                                                                                config::getPlacementOverlayColor,
                                                                                config::setPlacementOverlayColor)
                                                                .controller(opt -> ColorControllerBuilder.create(opt)
                                                                                .allowAlpha(true))
                                                                .build())
                                                .build())

                                // FAST PLACEMENT CATEGORY
                                .category(ConfigCategory.createBuilder()
                                                .name(Component.literal("Fast Placement"))
                                                .tooltip(Component.literal("Configure fast block placement settings"))

                                                .option(Option.<Boolean>createBuilder()
                                                                .name(Component.literal("Enable Fast Place/Break"))
                                                                .description(OptionDescription.of(
                                                                                Component.literal(
                                                                                                "Enables fast placement and breaking features."),
                                                                                Component.literal(""),
                                                                                Component.literal(
                                                                                                "Same as pressing the Fast Place keybind (Button 6).")))
                                                                .binding(
                                                                                false,
                                                                                config::isFastPlaceEnabled,
                                                                                config::setFastPlaceEnabled)
                                                                .controller(TickBoxControllerBuilder::create)
                                                                .build())

                                                .option(Option.<Integer>createBuilder()
                                                                .name(Component.literal("Placement Delay"))
                                                                .description(OptionDescription.of(
                                                                                Component.literal(
                                                                                                "Minimum delay between block placements"),
                                                                                Component.literal(""),
                                                                                Component.literal(
                                                                                                "50ms = 20 blocks/second"),
                                                                                Component.literal(
                                                                                                "100ms = 10 blocks/second"),
                                                                                Component.literal(
                                                                                                "200ms = 5 blocks/second")))
                                                                .binding(
                                                                                0,
                                                                                config::getFastPlaceDelayMs,
                                                                                config::setFastPlaceDelayMs)
                                                                .controller(opt -> IntegerSliderControllerBuilder
                                                                                .create(opt)
                                                                                .range(50, 500)
                                                                                .step(10)
                                                                                .formatValue(value -> Component.literal(
                                                                                                value + " ms")))
                                                                .build())

                                                .option(Option.<Integer>createBuilder()
                                                                .name(Component.literal("Break Delay"))
                                                                .description(OptionDescription.of(
                                                                                Component.literal(
                                                                                                "Minimum delay between block breaks in creative mode."),
                                                                                Component.literal(""),
                                                                                Component.literal(
                                                                                                "Adjust this if you experience ghost blocks."),
                                                                                Component.literal(
                                                                                                "Higher values = safer (less ghost blocks), slower breaking.")))
                                                                .binding(
                                                                                150,
                                                                                config::getFastBreakDelayMs,
                                                                                config::setFastBreakDelayMs)
                                                                .controller(opt -> IntegerSliderControllerBuilder
                                                                                .create(opt)
                                                                                .range(50, 500)
                                                                                .step(10)
                                                                                .formatValue(value -> Component.literal(
                                                                                                value + " ms")))
                                                                .build())
                                                .build())

                                .save(ConfigPersistence::save)
                                .build()
                                .generateScreen(parent);
        }

}