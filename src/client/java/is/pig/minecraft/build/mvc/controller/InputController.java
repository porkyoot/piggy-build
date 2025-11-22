package is.pig.minecraft.build.mvc.controller;

import com.mojang.blaze3d.platform.InputConstants;
import is.pig.minecraft.build.lib.event.MouseScrollCallback;
import is.pig.minecraft.build.lib.math.PlacementCalculator;
import is.pig.minecraft.build.lib.ui.GenericRadialMenuScreen;
import is.pig.minecraft.build.mvc.model.BuildSession;
import is.pig.minecraft.build.mvc.model.BuildShape;
import is.pig.minecraft.build.mvc.model.PlacementSession;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class InputController {

    public static KeyMapping triggerKey;   // Touche X (Menu Formes)
    public static KeyMapping flexibleKey;  // Bouton 4 (Placement Flexible)
    
    private boolean wasKeyDown = false;
    
    // Garde-fou pour empêcher la boucle infinie (Recursion) lors du placement manuel
    private boolean isPlacing = false;

    public void initialize() {
        // 1. Enregistrement des touches
        triggerKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("Shape Selector", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, "Piggy Build"));
        flexibleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("Flexible Block Placement", InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_4, "Piggy Build"));

        // 2. Gestion du Scroll (Pour le rayon du menu)
        MouseScrollCallback.EVENT.register((amount) -> {
            if (triggerKey.isDown()) {
                BuildSession.getInstance().modifyRadius(amount > 0 ? 1 : -1);
                return true;
            }
            return false;
        });

        // 3. Gestion des Ticks (Logique continue)
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        // 4. INTERCEPTION DU CLIC DROIT (Placement Flexible)
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            
            // A. Si c'est nous qui posons le bloc manuellement, on laisse passer
            if (this.isPlacing) {
                return InteractionResult.PASS;
            }

            // B. On ignore la main gauche pour éviter les doublons
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }

            // C. Vérif Mode Flexible actif
            if (!PlacementSession.getInstance().isActive()) {
                return InteractionResult.PASS;
            }

            // D. Vérif Offset (Bord du bloc)
            Direction offset = PlacementSession.getInstance().getCurrentOffset();
            if (offset == null) {
                return InteractionResult.PASS; // Centre -> Comportement normal
            }

            // E. ACTION : Placement Flexible
            if (world.isClientSide) {
                Minecraft mc = Minecraft.getInstance();
                
                // On crée le "Faux Clic" sur la bonne face (offset)
                BlockHitResult newHit = new BlockHitResult(
                    hitResult.getLocation(), 
                    offset, 
                    hitResult.getBlockPos(), 
                    hitResult.isInside()
                );

                try {
                    // On lève le drapeau "C'est moi !"
                    this.isPlacing = true;
                    
                    // On force l'action
                    InteractionResult result = mc.gameMode.useItemOn(mc.player, hand, newHit);

                    if (result.consumesAction()) {
                        if (result.shouldSwing()) mc.player.swing(hand);
                        // SUCCESS = On dit au jeu "J'ai géré, arrête tout"
                        return InteractionResult.SUCCESS;
                    }
                } finally {
                    // On baisse le drapeau
                    this.isPlacing = false;
                }
                
                // CRUCIAL : Si on est ici, c'est que le placement a échoué (ex: bloc solide)
                // ou réussi via notre logique. Dans tous les cas, on bloque le clic original
                // pour ne pas poser un bloc sur la face initiale par erreur (Double Placement).
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });
    }

    private void onTick(Minecraft client) {
        if (client.player == null) return;

        // --- LOGIQUE MENU (Shape Selector) ---
        boolean isTriggerDown = triggerKey.isDown();

        // DÉTECTION DU CLIC (Just Pressed) pour ouvrir le menu
        if (isTriggerDown && !wasKeyDown && client.screen == null) {
            handleTriggerPress(client);
        }
        wasKeyDown = isTriggerDown;

        // --- LOGIQUE FLEXIBLE (Bouton 4) ---
        boolean isFlexDown = flexibleKey.isDown();
        PlacementSession.getInstance().setActive(isFlexDown);

        if (isFlexDown) {
            if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult hit = (BlockHitResult) client.hitResult;
                
                // Calcul de l'offset via notre librairie Math
                Direction offset = PlacementCalculator.getOffsetDirection(hit);
                PlacementSession.getInstance().setCurrentOffset(offset);
            } else {
                PlacementSession.getInstance().setCurrentOffset(null);
            }
        }
    }

    private void handleTriggerPress(Minecraft client) {
        // 1. VÉRIFICATION : Est-ce qu'on regarde un bloc valide ?
        if (client.hitResult == null || client.hitResult.getType() != HitResult.Type.BLOCK) {
            // CAS DU VIDE : On nettoie tout (Désactive le rendu)
            BuildSession.getInstance().clearAnchor();
            return; 
        }

        // 2. CAS VALIDE : On met à jour l'ancre
        BlockHitResult hit = (BlockHitResult) client.hitResult;
        BuildSession.getInstance().setAnchor(hit.getBlockPos(), hit.getDirection().getAxis());

        // 3. Préparation des données pour le menu
        BuildShape center = BuildShape.BLOCK;
        List<BuildShape> radials = Arrays.stream(BuildShape.values())
                .filter(s -> s != center)
                .collect(Collectors.toList());

        // 4. Ouverture du Menu
        client.setScreen(new GenericRadialMenuScreen<>(
            Component.literal("Build Menu"),
            center,
            radials,
            BuildSession.getInstance().getShape(),
            KeyBindingHelper.getBoundKeyOf(triggerKey),
            (newShape) -> BuildSession.getInstance().setShape(newShape),
            () -> {}, // Close callback
            
            // Texte du Rayon
            (shape) -> {
                if (shape == BuildShape.BLOCK) return null;
                int r = (int) BuildSession.getInstance().getRadius();
                return Component.literal(String.valueOf(r));
            },

            // Scroll Callback
            (amount) -> {
                modifyRadius(amount);
                return true;
            }
        ));
    }

    private void modifyRadius(double amount) {
        BuildSession.getInstance().modifyRadius(amount > 0 ? 1 : -1);
    }
}