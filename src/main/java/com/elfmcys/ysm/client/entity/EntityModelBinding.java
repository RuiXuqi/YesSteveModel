package com.elfmcys.ysm.client.entity;

import com.elfmcys.ysm.client.model.ClientModelService;
import com.elfmcys.ysm.client.model.ModelRenderTargetLease;
import com.elfmcys.ysm.client.model.catalog.ModelContentVersion;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.task.TaskScope;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

final class EntityModelBinding implements AutoCloseable {
    @FunctionalInterface
    interface ResourceFactory {
        @Nullable
        CustomEntity.ResourceHolder create(ModelRenderTargetLease lease, boolean fallback);
    }

    @Nullable
    private Hash256 modelHash;
    @Nullable
    private DesiredRenderTarget requestedRenderTarget;
    private int requestGeneration;
    private int fallbackRequestGeneration = -1;
    @Nullable
    private TaskScope primaryRequestScope;
    @Nullable
    private TaskScope fallbackRequestScope;
    @Nullable
    private CustomEntity.ResourceHolder resourceHolder;

    void updateModelHash(@Nullable Hash256 modelHash) {
        this.modelHash = modelHash;
    }

    @Nullable
    Hash256 modelHash() {
        return modelHash;
    }

    @Nullable
    CustomEntity.ResourceHolder resourceHolder() {
        return resourceHolder;
    }

    void synchronize(@Nullable String renderTargetId, String textureName,
                     @Nullable String fallbackRenderTargetId, ResourceFactory factory) {
        if (resourceHolder != null && !resourceHolder.isCurrent()) {
            resourceHolder.close();
            resourceHolder = null;
        }
        var service = ClientModelService.instance();
        var entry = modelHash == null ? null : service.catalog().find(modelHash).orElse(null);
        if (entry != null) {
            if (renderTargetId == null || renderTargetId.isBlank()) {
                cancelPrimaryRequest();
                releasePrimaryResource();
                requestFallback(fallbackRenderTargetId, factory);
                return;
            }
            var desired = new DesiredRenderTarget(entry.modelHash(), entry.contentVersion(),
                    renderTargetId, textureName);
            if (!desired.equals(requestedRenderTarget)) {
                request(desired, fallbackRenderTargetId, factory);
            }
            if (resourceHolder != null && !resourceHolder.fallback) {
                service.reportActiveModelUse(desired.modelHash(), desired.contentVersion(),
                        desired.renderTargetId(), desired.textureName());
            }
        } else {
            if (modelHash != null && requestedRenderTarget != null) {
                service.reportActiveModelFailure(requestedRenderTarget.modelHash(),
                        requestedRenderTarget.contentVersion(), fallbackRenderTargetId != null
                                && !fallbackRenderTargetId.isBlank());
            }
            cancelPrimaryRequest();
            releasePrimaryResource();
        }

        if (resourceHolder == null || (modelHash == null && !resourceHolder.fallback)) {
            requestFallback(fallbackRenderTargetId, factory);
        }
    }

    void clearModel() {
        modelHash = null;
        invalidateRequests();
    }

    void releaseRenderTarget() {
        invalidateRequests();
        if (resourceHolder != null) {
            resourceHolder.close();
            resourceHolder = null;
        }
    }

    private void request(DesiredRenderTarget desired, @Nullable String fallbackRenderTargetId,
                         ResourceFactory factory) {
        closePrimaryRequestScope();
        closeFallbackRequestScope();
        requestedRenderTarget = desired;
        int generation = ++requestGeneration;
        fallbackRequestGeneration = -1;
        requestFallback(fallbackRenderTargetId, factory);
        var service = ClientModelService.instance();
        var requestScope = service.openRequestScope();
        primaryRequestScope = requestScope;
        service.acquire(requestScope, desired.modelHash(), desired.renderTargetId(), desired.textureName())
                .whenComplete((lease, error) -> {
                    requestScope.close();
                    Minecraft.getInstance().execute(() -> {
                        if (primaryRequestScope == requestScope) {
                            primaryRequestScope = null;
                        }
                        if (lease == null) {
                            if (generation == requestGeneration && desired.equals(requestedRenderTarget)) {
                                service.reportActiveModelFailure(desired.modelHash(),
                                        desired.contentVersion(), fallbackRenderTargetId != null
                                                && !fallbackRenderTargetId.isBlank());
                            }
                            return;
                        }
                        if (generation != requestGeneration || !desired.equals(requestedRenderTarget)) {
                            lease.close();
                            return;
                        }
                        applyLease(lease, false, factory);
                    });
                });
    }

    private void requestFallback(@Nullable String renderTargetId, ResourceFactory factory) {
        if (resourceHolder != null) {
            return;
        }
        int generation = requestGeneration;
        if (fallbackRequestGeneration == generation || renderTargetId == null || renderTargetId.isBlank()) {
            return;
        }
        fallbackRequestGeneration = generation;
        closeFallbackRequestScope();
        var service = ClientModelService.instance();
        var requestScope = service.openRequestScope();
        fallbackRequestScope = requestScope;
        service.acquireDefault(requestScope, renderTargetId).whenComplete((lease, error) -> {
            requestScope.close();
            Minecraft.getInstance().execute(() -> {
                if (fallbackRequestScope == requestScope) {
                    fallbackRequestScope = null;
                }
                if (fallbackRequestGeneration == generation) {
                    fallbackRequestGeneration = -1;
                }
                if (lease == null) {
                    return;
                }
                if (generation != requestGeneration || (resourceHolder != null && !resourceHolder.fallback)) {
                    lease.close();
                    return;
                }
                applyLease(lease, true, factory);
            });
        });
    }

    private void applyLease(ModelRenderTargetLease lease, boolean fallback, ResourceFactory factory) {
        var next = factory.create(lease, fallback);
        if (next == null) {
            lease.close();
            return;
        }
        if (!fallback) {
            closeFallbackRequestScope();
            fallbackRequestGeneration = -1;
        }
        if (resourceHolder != null && resourceHolder != next) {
            resourceHolder.close();
        }
        resourceHolder = next;
    }

    private void cancelPrimaryRequest() {
        if (requestedRenderTarget == null && (resourceHolder == null || resourceHolder.fallback)) {
            return;
        }
        invalidateRequests();
    }

    private void invalidateRequests() {
        closePrimaryRequestScope();
        closeFallbackRequestScope();
        requestedRenderTarget = null;
        requestGeneration++;
        fallbackRequestGeneration = -1;
    }

    private void releasePrimaryResource() {
        if (resourceHolder != null && !resourceHolder.fallback) {
            resourceHolder.close();
            resourceHolder = null;
        }
    }

    private void closePrimaryRequestScope() {
        if (primaryRequestScope != null) {
            primaryRequestScope.close();
            primaryRequestScope = null;
        }
    }

    private void closeFallbackRequestScope() {
        if (fallbackRequestScope != null) {
            fallbackRequestScope.close();
            fallbackRequestScope = null;
        }
    }

    @Override
    public void close() {
        clearModel();
        releaseRenderTarget();
    }

    private record DesiredRenderTarget(Hash256 modelHash, ModelContentVersion contentVersion,
                                       String renderTargetId, String textureName) {
    }
}
