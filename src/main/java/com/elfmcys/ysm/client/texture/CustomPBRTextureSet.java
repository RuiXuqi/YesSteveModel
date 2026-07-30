package com.elfmcys.ysm.client.texture;

import com.elfmcys.ysm.format.schema.file.PBRImageSources;
import com.elfmcys.ysm.info.type.PBRTextureType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.jetbrains.annotations.Nullable;
import com.elfmcys.ysm.client.model.ModelResourceFailureGate;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Function;

public class CustomPBRTextureSet extends CustomTexture implements PBRTextureSet {
    private final @Nullable CustomTexture normal;
    private final @Nullable CustomTexture specular;
    private final Map<PBRTextureType, AbstractTexture> pbrTextures;

    public CustomPBRTextureSet(PBRImageSources sources, Executor workers) {
        this(sources, workers, ignored -> ModelResourceFailureGate.none());
    }

    public CustomPBRTextureSet(PBRImageSources sources, Executor workers,
                               Function<String, ModelResourceFailureGate> failureGates) {
        super(sources.uv(), workers, failureGates.apply("uv"));
        this.normal = sources.normal() == null ? null
                : new CustomTexture(sources.normal(), workers, failureGates.apply("normal"));
        this.specular = sources.specular() == null ? null
                : new CustomTexture(sources.specular(), workers, failureGates.apply("specular"));
        var textures = new EnumMap<PBRTextureType, AbstractTexture>(PBRTextureType.class);
        if (this.normal != null) {
            textures.put(PBRTextureType.NORMAL, this.normal);
        }
        if (this.specular != null) {
            textures.put(PBRTextureType.SPECULAR, this.specular);
        }
        pbrTextures = Collections.unmodifiableMap(textures);
    }

    @Nullable
    public CustomTexture getNormal() {
        return normal;
    }

    @Nullable
    public CustomTexture getSpecular() {
        return specular;
    }

    @Override
    public Map<PBRTextureType, ? extends AbstractTexture> getPBRTextures() {
        return pbrTextures;
    }

    @Override
    public void close() {
        if (normal != null) {
            normal.close();
        }
        if (specular != null) {
            specular.close();
        }
        super.close();
    }
}
