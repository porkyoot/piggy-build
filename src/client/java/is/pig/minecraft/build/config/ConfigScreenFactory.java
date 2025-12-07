package is.pig.minecraft.build.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Color;

public class ConfigScreenFactory {

    public static Screen create(Screen parent) {
        PiggyConfig config = PiggyConfig.getInstance();

        return YetAnotherConfigLib.createBuilder()
            .title(Component.literal("Piggy Build Configuration"))
            
            // VISUALS CATEGORY
            .category(ConfigCategory.createBuilder()
                .name(Component.literal("Visuals"))
                .tooltip(Component.literal("Customize how the shape selector looks."))
                
                .option(Option.<Color>createBuilder()
                    .name(Component.literal("Shape Selector Highlight Color"))
                    .description(OptionDescription.of(Component.literal("The color and opacity of the shape selector.")))
                    .binding(
                        new Color(0, 255, 230, 100),
                        config::getHighlightColor,
                        config::setHighlightColor
                    )
                    .controller(opt -> ColorControllerBuilder.create(opt)
                        .allowAlpha(true))
                    .build())
                
                .option(Option.<Color>createBuilder()
                    .name(Component.literal("Placement Overlay Color"))
                    .description(OptionDescription.of(Component.literal("The color and opacity of the flexible placement overlay.")))
                    .binding(
                        new Color(0, 255, 230, 100),
                        config::getPlacementOverlayColor,
                        config::setPlacementOverlayColor
                    )
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
                        Component.literal("Minimum delay between block placements"),
                        Component.literal(""),
                        Component.literal("50ms = 20 blocks/second"),
                        Component.literal("100ms = 10 blocks/second"),
                        Component.literal("200ms = 5 blocks/second")
                    ))
                    .binding(
                        0,
                        config::getFastPlaceDelayMs,
                        config::setFastPlaceDelayMs
                    )
                    .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                        .range(50, 500)
                        .step(10)
                        .formatValue(value -> Component.literal(value + " ms"))
                    )
                    .build())
                .build())

            .save(PiggyConfig::save)
            .build()
            .generateScreen(parent);
    }
}