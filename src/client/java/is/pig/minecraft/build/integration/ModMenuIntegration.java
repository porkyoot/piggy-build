package is.pig.minecraft.build.integration;
import is.pig.minecraft.api.*;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi; // Import de notre factory YACL

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> is.pig.minecraft.build.config.ConfigScreenFactory.create(parent);
    }
}