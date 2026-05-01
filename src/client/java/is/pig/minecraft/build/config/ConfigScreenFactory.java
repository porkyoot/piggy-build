package is.pig.minecraft.build.config;
import is.pig.minecraft.api.*;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;

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
                        .option(Option.<Integer>createBuilder()
                                .name(Component.literal("Click Speed (CPS)"))
                                .description(OptionDescription.of(Component.literal(
                                        "Clicks per second. Higher = Faster. 0 = Unlimited (Instant).")))
                                .binding(10, config::getTickDelay, config::setTickDelay)
                                .controller(opt -> IntegerSliderControllerBuilder
                                        .create(opt).range(0, 20).step(1).formatValue(v -> Component.literal(v == 0 ? "Unlimited" : v + " CPS")))
                                .build())
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
                        .option(Option.<Color>createBuilder()
                                .name(Component.literal("Light Level Overlay Color"))
                                .binding(
                                        new Color(255, 0, 0, 255),
                                        config::getLightLevelOverlayColor,
                                        config::setLightLevelOverlayColor)
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true))
                                .build())
                        .option(Option.<Color>createBuilder()
                                .name(Component.literal("Sky Light Level Overlay Color"))
                                .binding(
                                        new Color(255, 165, 0, 255),
                                        config::getSkyLightLevelOverlayColor,
                                        config::setSkyLightLevelOverlayColor)
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true))
                                .build())
                        .build())

                // FEATURES CATEGORY
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Features"))
                        .tooltip(Component.literal("Configuration for all build features and their keybindings."))
                        
                        // Flexible Placement Group
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("Flexible Placement"))
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
                                .option(Option.<String>createBuilder()
                                        .name(Component.literal("Directional Keybinding"))
                                        .available(false)
                                        .binding(
                                                "",
                                                () -> is.pig.minecraft.build.mvc.controller.InputController.directionalKey.getTranslatedKeyMessage().getString(),
                                                v -> {})
                                        .controller(dev.isxander.yacl3.api.controller.StringControllerBuilder::create)
                                        .build())
                                .option(Option.<String>createBuilder()
                                        .name(Component.literal("Diagonal Keybinding"))
                                        .available(false)
                                        .binding(
                                                "",
                                                () -> is.pig.minecraft.build.mvc.controller.InputController.diagonalKey.getTranslatedKeyMessage().getString(),
                                                v -> {})
                                        .controller(dev.isxander.yacl3.api.controller.StringControllerBuilder::create)
                                        .build())
                                .build())
                                
                        // Fast Placement Group
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("Fast Placement"))
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
                                .option(Option.<String>createBuilder()
                                        .name(Component.literal("Fast Place Keybinding"))
                                        .available(false)
                                        .binding(
                                                "",
                                                () -> is.pig.minecraft.build.mvc.controller.InputController.fastPlaceKey.getTranslatedKeyMessage().getString(),
                                                v -> {})
                                        .controller(dev.isxander.yacl3.api.controller.StringControllerBuilder::create)
                                        .build())
                                .build())

                        // Auto Parkour Group
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("Auto Parkour"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Enable Auto Parkour"))
                                        .description(OptionDescription.of(
                                                Component.literal("Automatically place blocks below you when running and jumping."),
                                                Component.literal("If Anti-Cheat is active, this cannot be enabled.")))
                                        .available(config.isAutoParkourEditable())
                                        .binding(
                                                false,
                                                config::isAutoParkourEnabled,
                                                config::setAutoParkourEnabled)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<String>createBuilder()
                                        .name(Component.literal("Auto Parkour Keybinding"))
                                        .available(false)
                                        .binding(
                                                "",
                                                () -> is.pig.minecraft.build.mvc.controller.InputController.autoParkourKey.getTranslatedKeyMessage().getString(),
                                                v -> {})
                                        .controller(dev.isxander.yacl3.api.controller.StringControllerBuilder::create)
                                        .build())
                                .build())

                        // Auto MLG Group
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("Auto MLG"))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Enable Auto MLG"))
                                        .description(OptionDescription.of(
                                                Component.literal("Automatically perform water/slime/boat MLGs to prevent fall damage."),
                                                Component.literal("If Anti-Cheat is active, this cannot be enabled.")))
                                        .available(config.isAutoMlgEditable())
                                        .binding(
                                                false,
                                                config::isAutoMlgEnabled,
                                                config::setAutoMlgEnabled)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<String>createBuilder()
                                        .name(Component.literal("Auto MLG Keybinding"))
                                        .available(false)
                                        .binding(
                                                "",
                                                () -> is.pig.minecraft.build.mvc.controller.InputController.autoMlgKey.getTranslatedKeyMessage().getString(),
                                                v -> {})
                                        .controller(dev.isxander.yacl3.api.controller.StringControllerBuilder::create)
                                        .build())
                                .build())
                        .build())

                .save(ConfigPersistence::save)
                .build()
                .generateScreen(parent);
    }
}