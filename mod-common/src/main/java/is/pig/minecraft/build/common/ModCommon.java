package is.pig.minecraft.build.common;

import is.pig.minecraft.build.api.IModAdapter;

public class ModCommon {
    private static IModAdapter adapter;

    public static void initialize(IModAdapter modAdapter) {
        adapter = modAdapter;
        System.out.println("piggy-build: ModCommon initialized with " + modAdapter.getClass().getSimpleName());
    }

    public static IModAdapter getAdapter() {
        return adapter;
    }
}
