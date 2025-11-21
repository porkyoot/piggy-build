package is.pig.minecraft.build;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface MouseScrollCallback {
    
    Event<MouseScrollCallback> EVENT = EventFactory.createArrayBacked(MouseScrollCallback.class,
        (listeners) -> (amount) -> {
            for (MouseScrollCallback listener : listeners) {
                boolean result = listener.onScroll(amount);
                if (result) {
                    return true; // Si un listener renvoie true, on annule le scroll vanilla
                }
            }
            return false;
        });

    /**
     * Appelé lors du scroll de la souris.
     * @param amount La quantité de scroll (positif = haut, négatif = bas)
     * @return true pour annuler le comportement par défaut (changer d'item), false pour laisser faire.
     */
    boolean onScroll(double amount);
}