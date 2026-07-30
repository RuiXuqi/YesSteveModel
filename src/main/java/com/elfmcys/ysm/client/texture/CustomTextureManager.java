package com.elfmcys.ysm.client.texture;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.util.CleanerUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.time.StopWatch;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CustomTextureManager {
    private final static int MAX_MILLI = 20;
    private static long COUNTER = 0;

    private final static IdentityHashMap<AbstractTexture, TextureRegistration> REGISTRATIONS = new IdentityHashMap<>();
    private final static Queue<RegistrationEvent> PENDING_REGISTRATIONS = new ArrayDeque<>();
    private final static Queue<CleanupEvent> CLEANUP_EVENTS = new ConcurrentLinkedQueue<>();
    private final static Queue<RemovalEvent> PENDING_REMOVALS = new ArrayDeque<>();

    public static TextureHolder register(AbstractTexture texture, boolean immediately) {
        return register(texture, immediately, 10 * 20);
    }

    public static TextureHolder register(AbstractTexture texture, boolean immediately, int removingDelayTicks) {
        RenderSystem.assertOnRenderThread();

        var registration = REGISTRATIONS.get(texture);
        if (registration == null) {
            registration = new TextureRegistration(new TextureRegistrationState<>(nextId()));
            REGISTRATIONS.put(texture, registration);
        } else {
            var current = registration.holder();
            if (current != null && registration.state.isActive(current.token)) {
                if (immediately && !current.ready) {
                    doRegister(texture, registration, current);
                }
                return current;
            }
        }

        var activation = registration.state.activate(removingDelayTicks);
        var holder = new TextureHolderImpl(registration.state.id(), activation.token(), activation.ready());
        registration.holder(holder);
        CleanerUtil.ref(holder, new CleanupEvent(texture, activation.token()), CLEANUP_EVENTS::add);

        if (texture instanceof PBRTextureSet pbrTextureSet) {
            for (var pbr : pbrTextureSet.getPBRTextures().values()) {
                if (holder.pbr == null) {
                    holder.pbr = new ArrayList<>(2);
                }
                holder.pbr.add(register(pbr, immediately, removingDelayTicks));
            }
        }
        if (!holder.ready) {
            if (immediately) {
                doRegister(texture, registration, holder);
            } else {
                PENDING_REGISTRATIONS.add(new RegistrationEvent(texture, activation.token()));
            }
        }
        return holder;
    }

    public static void release(AbstractTexture texture) {
        RenderSystem.assertOnRenderThread();
        var registration = REGISTRATIONS.get(texture);
        if (registration == null) {
            return;
        }
        registration.clearHolder();
        discardIfUnregistered(texture, registration, registration.state.releaseCurrent());
    }

    public static void tick() {
        RenderSystem.assertOnRenderThread();

        CleanupEvent cleanup;
        while ((cleanup = CLEANUP_EVENTS.poll()) != null) {
            var registration = REGISTRATIONS.get(cleanup.texture());
            if (registration != null) {
                discardIfUnregistered(cleanup.texture(), registration,
                        registration.state.release(cleanup.token()));
            }
        }

        for (var entry : REGISTRATIONS.entrySet()) {
            entry.getValue().state.tickRemoval().ifPresent(token ->
                    PENDING_REMOVALS.add(new RemovalEvent(entry.getKey(), token)));
        }

        StopWatch stopWatch = StopWatch.createStarted();
        while (true) {
            var pending = PENDING_REGISTRATIONS.poll();
            if (pending == null) {
                break;
            }
            var registration = REGISTRATIONS.get(pending.texture());
            if (registration != null && registration.state.needsRegistration(pending.token())) {
                var holder = registration.holder();
                if (holder == null || holder.token != pending.token()) {
                    discardIfUnregistered(pending.texture(), registration,
                            registration.state.release(pending.token()));
                } else {
                    doRegister(pending.texture(), registration, holder);
                }
            }
            if (stopWatch.getTime() >= MAX_MILLI) {
                return;
            }
        }

        var manager = Minecraft.getInstance().getTextureManager();
        while (true) {
            var removal = PENDING_REMOVALS.poll();
            if (removal == null) {
                break;
            }
            var registration = REGISTRATIONS.get(removal.texture());
            if (registration != null && registration.state.shouldRelease(removal.token())) {
                manager.release(registration.state.id());
                if (registration.state.markReleased(removal.token())) {
                    REGISTRATIONS.remove(removal.texture());
                }
            }
            if (stopWatch.getTime() >= MAX_MILLI) {
                return;
            }
        }
    }

    private static void doRegister(AbstractTexture texture, TextureRegistration registration,
                                   TextureHolderImpl holder) {
        if (registration.state.isReady(holder.token)) {
            holder.setReady();
            return;
        }
        if (!registration.state.needsRegistration(holder.token)) {
            return;
        }
        Minecraft.getInstance().getTextureManager().register(registration.state.id(), texture);
        if (registration.state.markRegistered(holder.token)) {
            holder.setReady();
        }
    }

    private static void discardIfUnregistered(AbstractTexture texture, TextureRegistration registration,
                                               TextureRegistrationState.ReleaseResult result) {
        if (result == TextureRegistrationState.ReleaseResult.DISCARD
                && REGISTRATIONS.get(texture) == registration) {
            REGISTRATIONS.remove(texture);
        }
    }

    @SuppressWarnings("removal")
    private static ResourceLocation nextId() {
        return new ResourceLocation(YesSteveModel.MOD_ID, "textures/" + ++COUNTER);
    }

    private static final class TextureRegistration {
        private final TextureRegistrationState<ResourceLocation> state;
        private WeakReference<TextureHolderImpl> holder;

        private TextureRegistration(TextureRegistrationState<ResourceLocation> state) {
            this.state = state;
        }

        private TextureHolderImpl holder() {
            return holder == null ? null : holder.get();
        }

        private void holder(TextureHolderImpl holder) {
            this.holder = new WeakReference<>(holder);
        }

        private void clearHolder() {
            holder = null;
        }
    }

    private record RegistrationEvent(AbstractTexture texture, TextureRegistrationState.Token token) {
    }

    private record CleanupEvent(AbstractTexture texture, TextureRegistrationState.Token token) {
    }

    private record RemovalEvent(AbstractTexture texture, TextureRegistrationState.Token token) {
    }

    private static class TextureHolderImpl implements TextureHolder {
        private final ResourceLocation id;
        private final TextureRegistrationState.Token token;
        private List<TextureHolder> pbr;
        private volatile boolean ready;

        private TextureHolderImpl(ResourceLocation id, TextureRegistrationState.Token token, boolean ready) {
            this.id = id;
            this.token = token;
            this.ready = ready;
        }

        @Override
        public Optional<ResourceLocation> id() {
            return ready ? Optional.of(id) : Optional.empty();
        }

        private void setReady() {
            ready = true;
        }
    }
}
