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
                PiggyConfig config = PiggyConfig.getInstance();

                return YetAnotherConfigLib.createBuilder()
                                .title(Component.literal("Piggy Build Configuration"))

                                // SAFETY CATEGORY
                                .category(ConfigCategory.createBuilder()
                                                .name(Component.literal("Safety"))
                                                .tooltip(Component.literal("Configure safety and anti-cheat settings."))
                                                .option(Option.<Boolean>createBuilder()
                                                                .name(Component.literal("No Cheating Mode"))
                                                                .description(OptionDescription.of(
                                                                                Component.literal(
                                                                                                "Prevents usage of Fast Place/Break in Survival mode."),
                                                                                Component.literal(""),
                                                                                Component.literal(
                                                                                                "§cWARNING: Disabling this allows cheats on servers!"),
                                                                                Component.literal(
                                                                                                "§cThis WILL result in a BAN on most servers.")))
                                                                .binding(
                                                                                true,
                                                                                config::isNoCheatingMode,
                                                                                newValue -> {
                                                                                        if (!newValue && config
                                                                                                        .isNoCheatingMode()) {
                                                                                                // User is attempting to
                                                                                                // disable it
                                                                                                Minecraft client = Minecraft
                                                                                                                .getInstance();
                                                                                                client.setScreen(
                                                                                                                new ConfirmScreen(
                                                                                                                                (confirmed) -> {
                                                                                                                                        if (confirmed) {
                                                                                                                                                config.setNoCheatingMode(
                                                                                                                                                                false);
                                                                                                                                                ConfigPersistence
                                                                                                                                                                .save();
                                                                                                                                        } else {
                                                                                                                                                config.setNoCheatingMode(
                                                                                                                                                                true); // Revert
                                                                                                                                                ConfigPersistence
                                                                                                                                                                .save();
                                                                                                                                        }
                                                                                                                                        // Re-open
                                                                                                                                        // config
                                                                                                                                        // screen
                                                                                                                                        client.setScreen(
                                                                                                                                                        ConfigScreenFactory
                                                                                                                                                                        .create(parent));
                                                                                                                                },
                                                                                                                                Component.literal(
                                                                                                                                                "Disable No Cheating Mode?"),
                                                                                                                                Component.literal(
                                                                                                                                                "§cWARNING: Disabling this allows the use of cheats (Fast Place/Break) in Survival mode.\nUsing these on servers is detectable and WILL RESULT IN A BAN.\n\nAre you sure you want to continue?"),
                                                                                                                                Component.literal(
                                                                                                                                                "Yes, I understand the risks"),
                                                                                                                                Component.literal(
                                                                                                                                                "Cancel")));
                                                                                        } else {
                                                                                                config.setNoCheatingMode(
                                                                                                                newValue);
                                                                                        }
                                                                                })
                                                                .controller(TickBoxControllerBuilder::create)
                                                                .build())
                                                .build())

                                // VISUALS CATEGORY
                                .category(ConfigCategory.createBuilder()
                                                .name(Component.literal("Visuals"))
                                                .tooltip(Component.literal("Customize how the shape selector looks."))

                                                .option(Option.<Color>createBuilder()
                                                                .name(Component.literal(
                                                                                "Shape Selector Highlight Color"))
                                                                .description(OptionDescription
                                                                                .of(Component.literal(
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