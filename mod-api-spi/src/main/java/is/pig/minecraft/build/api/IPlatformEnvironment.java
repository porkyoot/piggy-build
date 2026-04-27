package is.pig.minecraft.build.api;

import java.nio.file.Path;

public interface IPlatformEnvironment {
    Path getConfigDirectory();
    boolean isClient();
    boolean isDedicatedServer();
}
