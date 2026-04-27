package is.pig.minecraft.build.bootstrap;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import is.pig.minecraft.build.api.IModAdapter;
import is.pig.minecraft.build.common.PiggyCore;
import java.util.ServiceLoader;

public class PiggyModBootstrap implements ModInitializer {
    @Override
    public void onInitialize() {
        int version = getMinecraftMinorVersion();
        IModAdapter selectedAdapter = null;

        ServiceLoader<IModAdapter> loader = ServiceLoader.load(IModAdapter.class);
        for (IModAdapter adapter : loader) {
            if (version < 26 && adapter.getClass().getSimpleName().contains("Legacy")) {
                selectedAdapter = adapter;
                break;
            } else if (version >= 26 && adapter.getClass().getSimpleName().contains("Modern")) {
                selectedAdapter = adapter;
                break;
            }
        }

        if (selectedAdapter != null) {
            PiggyCore.init(selectedAdapter);
        } else {
            System.err.println("piggy-build: Failed to find suitable ModAdapter for version " + version);
        }
    }

    private int getMinecraftMinorVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft")
                .map(mod -> {
                    String versionStr = mod.getMetadata().getVersion().getFriendlyString();
                    String[] parts = versionStr.split("\\.");
                    if (parts.length >= 2) {
                        try {
                            return Integer.parseInt(parts[1]);
                        } catch (NumberFormatException e) {
                            return 21; // Default
                        }
                    }
                    return 21;
                }).orElse(21);
    }
}
