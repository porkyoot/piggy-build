package is.pig.minecraft.build.bootstrap;

import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;
import java.util.Set;

public class PiggyMixinConfigPlugin implements IMixinConfigPlugin {
    private static final int MC_VERSION = getMinecraftMinorVersion();
    private boolean isLegacyConfig;
    private boolean isModernConfig;

    @Override
    public void onLoad(String mixinPackage) {
        isLegacyConfig = mixinPackage.contains(".legacy");
        isModernConfig = mixinPackage.contains(".modern");
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (isLegacyConfig && MC_VERSION >= 26) {
            return false;
        }
        if (isModernConfig && MC_VERSION < 26) {
            return false;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static int getMinecraftMinorVersion() {
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
