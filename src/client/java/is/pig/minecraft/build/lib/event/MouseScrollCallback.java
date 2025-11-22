package is.pig.minecraft.build.lib.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface MouseScrollCallback {
    
    Event<MouseScrollCallback> EVENT = EventFactory.createArrayBacked(MouseScrollCallback.class,
        (listeners) -> (amount) -> {
            for (MouseScrollCallback listener : listeners) {
                boolean result = listener.onScroll(amount);
                if (result) {
                    return true; // If a listener returns true, cancel the vanilla scroll
                }
            }
            return false;
        });

    /**
     * Called when the mouse is scrolled.
     * @param amount The scroll amount (positive = up, negative = down)
     * @return true to cancel the default behavior (changing the selected item), false to allow it.
     */
    boolean onScroll(double amount);
}