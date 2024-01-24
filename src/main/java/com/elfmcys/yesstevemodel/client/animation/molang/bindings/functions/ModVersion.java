package com.elfmcys.yesstevemodel.client.animation.molang.bindings.functions;

import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import com.elfmcys.yesstevemodel.molang.runtime.Function;
import net.minecraftforge.common.util.MavenVersionStringHelper;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ModVersion implements Function {
    @Nullable
    @Override
    public Object evaluate(@Nonnull ExecutionContext<?> context, @Nonnull ArgumentCollection arguments) {
        String modId = arguments.getAsString(context, 0);
        if (modId == null) {
            return null;
        }

        return ModList.get().getModContainerById(modId)
                .map(modContainer -> MavenVersionStringHelper.artifactVersionToString((modContainer.getModInfo().getVersion())))
                .orElse(null);
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}
