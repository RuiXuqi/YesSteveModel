package com.elfmcys.yesstevemodel.client.texture;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.util.CleanerUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.time.StopWatch;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CustomTextureManager {
    private final static int MAX_MILLI = 10;
    private final static AtomicLong COUNTER = new AtomicLong();

    private final static ConcurrentHashMap<AbstractTexture, WeakReference<TextureHolderImpl>> HOLDER_MAP = new ConcurrentHashMap<>();
    private final static ConcurrentLinkedQueue<Pair<TextureHolderImpl, AbstractTexture>> PENDING_TEXTURES = new ConcurrentLinkedQueue<>();

    private final static ConcurrentHashMap<AbstractTexture, Pair<ResourceLocation, AtomicInteger>> REMOVING_TEXTURES = new ConcurrentHashMap<>();
    private final static ConcurrentLinkedQueue<ResourceLocation> REMOVED_TEXTURES = new ConcurrentLinkedQueue<>();


    public static TextureHolder register(AbstractTexture texture, boolean immediately) {
        return register(texture, immediately, 10 * 20);
    }

    public static TextureHolder register(AbstractTexture texture, boolean immediately, int removingDelayTicks) {
        if (immediately) {
            RenderSystem.assertOnRenderThread();
        }
        var ref = HOLDER_MAP.get(texture);
        if (ref != null) {
            var holder = ref.get();
            if (holder != null) {
                if (immediately && !holder.ready) {
                    doRegister(texture, holder);
                }
                return holder;
            }
            HOLDER_MAP.remove(texture);
        }

        TextureHolderImpl holder;
        var removing = REMOVING_TEXTURES.remove(texture);
        if (removing != null) {
            holder = new TextureHolderImpl(removing.first(), removingDelayTicks);
        } else {
            holder = new TextureHolderImpl(removingDelayTicks);
        }
        if (texture instanceof PBRTextureSet pbrTextureSet) {
            for (var pbr : pbrTextureSet.getPBRTextures().values()) {
                if (holder.pbr == null) {
                    holder.pbr = new ArrayList<>(2);
                }
                holder.pbr.add(register(pbr, immediately, removingDelayTicks));
            }
        }
        HOLDER_MAP.put(texture, new WeakReference<>(holder));
        if (immediately) {
            doRegister(texture, holder);
        } else {
            PENDING_TEXTURES.add(Pair.of(holder, texture));
        }
        return holder;
    }

    public static void release(AbstractTexture texture) {
        HOLDER_MAP.remove(texture);
    }

    public static void tick() {
        RenderSystem.assertOnRenderThread();
        StopWatch stopWatch = StopWatch.createStarted();

        if (!REMOVING_TEXTURES.isEmpty()) {
            var removingIter = REMOVING_TEXTURES.entrySet().iterator();
            while (removingIter.hasNext()) {
                var removing = removingIter.next();
                if (removing.getValue().second().decrementAndGet() < 0) {
                    removingIter.remove();
                     REMOVED_TEXTURES.add(removing.getValue().first());
                }
            }
        }

        var manager = Minecraft.getInstance().getTextureManager();
        while (true) {
            var removed = REMOVED_TEXTURES.poll();
            if (removed == null) {
                break;
            }
            manager.release(removed);
            if (stopWatch.getTime() >= MAX_MILLI) {
                return;
            }
        }

        while (true) {
            var texturePair = PENDING_TEXTURES.poll();
            if (texturePair == null) {
                return;
            }

            doRegister(texturePair.right(), texturePair.left());
            if (stopWatch.getTime() >= MAX_MILLI) {
                return;
            }
        }
    }

    private static void doRegister(AbstractTexture texture, TextureHolderImpl holder) {
        if (!holder.ready) {
            Minecraft.getInstance().getTextureManager().register(holder.id, texture);
            CleanerUtil.ref(holder, holder.id, holder.delayTicks, (id, delayTicks) -> REMOVING_TEXTURES.put(texture, Pair.of(id, new AtomicInteger(delayTicks))));
            holder.setReady();
        }
    }

    private static class TextureHolderImpl implements TextureHolder {
        private final ResourceLocation id;
        private final int delayTicks;
        private List<TextureHolder> pbr;
        private volatile boolean ready;

        public TextureHolderImpl(ResourceLocation id, int delayTicks) {
            this.id = id;
            this.delayTicks = delayTicks;
        }

        @SuppressWarnings("removal")
        TextureHolderImpl(int delayTicks) {
            this.id = new ResourceLocation(YesSteveModel.MOD_ID, "textures/" + COUNTER.getAndIncrement());
            this.delayTicks = delayTicks;
            this.ready = false;
        }

        @Override
        public Optional<ResourceLocation> id() {
            return ready ? Optional.of(id) : Optional.empty();
        }

        public void setReady() {
            this.ready = true;
        }
    }
}
