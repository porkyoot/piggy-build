package is.pig.minecraft.build.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Color;

public class ConfigScreenFactory {

    public static Screen create(Screen parent) {
        PiggyConfig config = PiggyConfig.getInstance();

        return YetAnotherConfigLib.createBuilder()
            .title(Component.literal("Piggy Build Configuration"))
            .category(ConfigCategory.createBuilder()
                .name(Component.literal("Visuals"))
                .tooltip(Component.literal("Customize how the shape selector looks."))
                
                // --- COLOR PICKER ---
                .option(Option.<Color>createBuilder()
                    .name(Component.literal("Shape Selector Highlight Color"))
                    .description(OptionDescription.of(Component.literal("The color and opacity of the shape selector.")))
                    .binding(
                        new Color(0, 255, 230, 100), // Default
                        config::getHighlightColor,   // Getter
                        config::setHighlightColor    // Setter
                    )
                    .controller(opt -> ColorControllerBuilder.create(opt)
                        .allowAlpha(true)) // Allow transparency editing
                    .build())
                // --- PLACEMENT OVERLAY COLOR PICKER ---
                .option(Option.<Color>createBuilder()
                    .name(Component.literal("Placement Overlay Color"))
                    .description(OptionDescription.of(Component.literal("The color and opacity of the flexible placement overlay.")))
                    .binding(
                        new Color(0, 255, 230, 100), // Default
                        config::getPlacementOverlayColor, // Getter
                        config::setPlacementOverlayColor  // Setter
                    )
                    .controller(opt -> ColorControllerBuilder.create(opt)
                        .allowAlpha(true))
                    .build())
                .build())

            .save(PiggyConfig::save) // Auto-save when closing
            .build()
            .generateScreen(parent);
    }
}