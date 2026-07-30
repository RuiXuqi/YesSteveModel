package com.elfmcys.ysm.client.texture;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.client.model.ModelResourceFailureGate;
import com.elfmcys.ysm.natives.image.ImageSource;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

public class CustomTexture extends AbstractTexture {
    private final ImageSource source;
    private final Executor workers;
    private final ModelResourceFailureGate failureGate;
    private final TextureLoadState loadState = new TextureLoadState();

    public CustomTexture(ImageSource source, Executor workers) {
        this(source, workers, ModelResourceFailureGate.none());
    }

    public CustomTexture(ImageSource source, Executor workers,
                         ModelResourceFailureGate failureGate) {
        this.source = Objects.requireNonNull(source, "source");
        this.workers = Objects.requireNonNull(workers, "workers");
        this.failureGate = Objects.requireNonNull(failureGate, "failureGate");
    }

    @Override
    public void load(ResourceManager resourceManager) {
        if (failureGate.failure().isPresent()) {
            return;
        }
        var start = loadState.begin();
        if (start == null) {
            return;
        }
        if (start.previous() != null) {
            start.previous().cancel(true);
        }

        final CompletableFuture<Void> task;
        try {
            task = CompletableFuture.runAsync(() -> readAndDispatch(start.generation()), workers);
        } catch (RuntimeException error) {
            if (fail(start.generation(), error)) {
                YesSteveModel.LOGGER.debug("Failed to schedule model texture load from {}", source, error);
            }
            return;
        }
        if (!loadState.attach(start.generation(), task)) {
            task.cancel(true);
        }
        task.whenComplete((ignored, error) -> {
            if (error != null && !(unwrap(error) instanceof CancellationException)) {
                var cause = unwrap(error);
                if (fail(start.generation(), cause)) {
                    YesSteveModel.LOGGER.debug("Failed to load model texture from {}", source, cause);
                }
            } else {
                loadState.complete(start.generation(), task);
            }
        });
    }

    private void readAndDispatch(long generation) {
        if (!isCurrent(generation)) {
            return;
        }
        final NativeImage pixels;
        try (var image = source.open()) {
            pixels = image.decode();
        } catch (Exception error) {
            throw new CompletionException(error);
        }
        if (!isCurrent(generation)) {
            pixels.close();
            return;
        }
        try {
            RenderSystem.recordRenderCall(() -> upload(generation, pixels));
        } catch (RuntimeException error) {
            pixels.close();
            throw error;
        }
    }

    private void upload(long generation, NativeImage pixels) {
        try (pixels) {
            if (isCurrent(generation)) {
                try {
                    doUpload(pixels);
                } catch (RuntimeException error) {
                    if (fail(generation, error)) {
                        YesSteveModel.LOGGER.debug("Failed to upload model texture from {}", source, error);
                    }
                }
            }
        }
    }

    private void doUpload(NativeImage img) {
        TextureUtil.prepareImage(this.getId(), 0, img.getWidth(), img.getHeight());
        img.upload(0, 0, 0, 0, 0,
                img.getWidth(), img.getHeight(),
                false, false, false, false);
    }

    public Optional<Throwable> failure() {
        return loadState.failure().or(failureGate::failure);
    }

    private boolean isCurrent(long generation) {
        return loadState.isCurrent(generation) && failureGate.failure().isEmpty();
    }

    private boolean fail(long generation, Throwable cause) {
        if (!loadState.fail(generation, cause)) {
            return false;
        }
        failureGate.fail(cause);
        return true;
    }

    @Override
    public void close() {
        var task = loadState.deactivate();
        if (task != null) {
            task.cancel(true);
        }
        super.close();
    }

    private static Throwable unwrap(Throwable error) {
        while ((error instanceof CompletionException || error instanceof java.util.concurrent.ExecutionException)
                && error.getCause() != null) {
            error = error.getCause();
        }
        return error;
    }
}
