package com.elfmcys.ysm.network.message.model;

import com.elfmcys.ysm.model.domain.ModelHash;
import com.elfmcys.ysm.model.source.ModelAssetSelector;
import com.elfmcys.ysm.model.source.ModelAssetSubject;
import com.elfmcys.ysm.proto.network.model.ModelAssetsProto;
import com.elfmcys.ysm.util.ProtoBytes;

import java.io.IOException;
import java.util.EnumSet;

/** The only mapping boundary between source-domain asset identities and the unstable protocol. */
public final class ModelAssetProtoMapper {
    private ModelAssetProtoMapper() {
    }

    public static ModelAssetsProto.AssetSubject toProto(ModelAssetSubject subject) {
        var result = ModelAssetsProto.AssetSubject.newInstance();
        if (subject instanceof ModelAssetSubject.Model model) {
            var value = ModelAssetsProto.ModelAssetSubject.newInstance();
            ProtoBytes.set(value.getMutableModelHash(), model.modelHash());
            ProtoBytes.set(value.getMutableDescriptorHash(), model.descriptorHash());
            return result.setModel(value);
        }
        var pack = (ModelAssetSubject.Pack) subject;
        return result.setPack(ModelAssetsProto.PackAssetSubject.newInstance()
                .setNamespace(pack.namespace())
                .setHierarchy(pack.hierarchy()));
    }

    public static ModelAssetSubject fromProto(ModelAssetsProto.AssetSubject value) throws IOException {
        try {
            if (value.hasModel()) {
                var model = value.getModel();
                return new ModelAssetSubject.Model(hash(model.getModelHash(), "model hash"),
                        hash(model.getDescriptorHash(), "descriptor hash"));
            }
            if (value.hasPack()) {
                return new ModelAssetSubject.Pack(value.getPack().getNamespace(),
                        value.getPack().getHierarchy());
            }
            throw new IllegalArgumentException("Asset subject is missing");
        } catch (IllegalArgumentException error) {
            throw new IOException("Invalid model asset subject", error);
        }
    }

    public static ModelAssetsProto.AssetSelector toProto(ModelAssetSelector selector) {
        var result = ModelAssetsProto.AssetSelector.newInstance();
        if (selector instanceof ModelAssetSelector.ModelPreview) {
            return result.setModelPreview(ModelAssetsProto.ModelPreviewSelector.newInstance());
        }
        if (selector instanceof ModelAssetSelector.RenderTarget target) {
            var value = ModelAssetsProto.RenderTargetSelector.newInstance()
                    .setRenderTargetId(target.renderTargetId())
                    .setTextureName(target.textureName());
            target.components().stream().sorted().map(ModelAssetProtoMapper::toProto)
                    .forEach(value::addComponents);
            return result.setRenderTarget(value);
        }
        if (selector instanceof ModelAssetSelector.ModelPresentation presentation) {
            return result.setPresentation(ModelAssetsProto.ModelPresentationSelector.newInstance()
                    .setAsset(toProto(presentation.asset()))
                    .setIndex(presentation.index()));
        }
        var cover = (ModelAssetSelector.PackCover) selector;
        return result.setPackCover(ModelAssetsProto.PackCoverSelector.newInstance()
                .setExpectedContentHash(cover.expectedContentHash().bytes()));
    }

    public static ModelAssetSelector fromProto(ModelAssetsProto.AssetSelector value) throws IOException {
        try {
            if (value.hasModelPreview()) {
                return ModelAssetSelector.preview();
            }
            if (value.hasRenderTarget()) {
                var target = value.getRenderTarget();
                var components = EnumSet.noneOf(ModelAssetSelector.RenderTargetComponent.class);
                if (target.hasComponents()) {
                    target.getComponents().forEach(component -> components.add(fromProto(component)));
                }
                return ModelAssetSelector.renderTarget(target.getRenderTargetId(), target.getTextureName(), components);
            }
            if (value.hasPresentation()) {
                return ModelAssetSelector.presentation(fromProto(value.getPresentation().getAsset()),
                        value.getPresentation().getIndex());
            }
            if (value.hasPackCover()) {
                return ModelAssetSelector.packCover(hash(value.getPackCover().getExpectedContentHash(),
                        "expected content hash"));
            }
            throw new IllegalArgumentException("Asset selector is missing");
        } catch (IllegalArgumentException error) {
            throw new IOException("Invalid model asset selector", error);
        }
    }

    public static boolean isPublicPresentation(ModelAssetSelector selector) {
        return !(selector instanceof ModelAssetSelector.RenderTarget);
    }

    private static ModelAssetsProto.RenderTargetComponent toProto(
            ModelAssetSelector.RenderTargetComponent component) {
        return switch (component) {
            case DEFINITION -> ModelAssetsProto.RenderTargetComponent.RENDER_TARGET_COMPONENT_DEFINITION;
            case TEXTURE_SET -> ModelAssetsProto.RenderTargetComponent.RENDER_TARGET_COMPONENT_TEXTURE_SET;
            case COMMON_BEHAVIOR -> ModelAssetsProto.RenderTargetComponent.RENDER_TARGET_COMPONENT_COMMON_BEHAVIOR;
        };
    }

    private static ModelAssetSelector.RenderTargetComponent fromProto(
            ModelAssetsProto.RenderTargetComponent component) {
        return switch (component) {
            case RENDER_TARGET_COMPONENT_DEFINITION -> ModelAssetSelector.RenderTargetComponent.DEFINITION;
            case RENDER_TARGET_COMPONENT_TEXTURE_SET -> ModelAssetSelector.RenderTargetComponent.TEXTURE_SET;
            case RENDER_TARGET_COMPONENT_COMMON_BEHAVIOR -> ModelAssetSelector.RenderTargetComponent.COMMON_BEHAVIOR;
            default -> throw new IllegalArgumentException("Unsupported render target component: " + component);
        };
    }

    private static ModelAssetsProto.PresentationAsset toProto(ModelAssetSelector.PresentationAsset asset) {
        return switch (asset) {
            case MODEL_ICON -> ModelAssetsProto.PresentationAsset.PRESENTATION_ASSET_MODEL_ICON;
            case AUTHOR_AVATAR -> ModelAssetsProto.PresentationAsset.PRESENTATION_ASSET_AUTHOR_AVATAR;
            case GUI_FOREGROUND -> ModelAssetsProto.PresentationAsset.PRESENTATION_ASSET_GUI_FOREGROUND;
            case GUI_BACKGROUND -> ModelAssetsProto.PresentationAsset.PRESENTATION_ASSET_GUI_BACKGROUND;
        };
    }

    private static ModelAssetSelector.PresentationAsset fromProto(ModelAssetsProto.PresentationAsset asset) {
        return switch (asset) {
            case PRESENTATION_ASSET_MODEL_ICON -> ModelAssetSelector.PresentationAsset.MODEL_ICON;
            case PRESENTATION_ASSET_AUTHOR_AVATAR -> ModelAssetSelector.PresentationAsset.AUTHOR_AVATAR;
            case PRESENTATION_ASSET_GUI_FOREGROUND -> ModelAssetSelector.PresentationAsset.GUI_FOREGROUND;
            case PRESENTATION_ASSET_GUI_BACKGROUND -> ModelAssetSelector.PresentationAsset.GUI_BACKGROUND;
            default -> throw new IllegalArgumentException("Unsupported presentation asset: " + asset);
        };
    }

    private static ModelHash hash(us.hebi.quickbuf.RepeatedByte value, String field) {
        if (value.length() != ModelHash.SIZE) {
            throw new IllegalArgumentException(field + " must contain exactly " + ModelHash.SIZE + " bytes");
        }
        return new ModelHash(value.array(), 0, value.length());
    }
}
