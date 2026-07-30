package com.elfmcys.ysm.format.parser;

import com.elfmcys.ysm.format.parser.pojo.manifest.ModelManifest;
import com.elfmcys.ysm.format.parser.pojo.manifest.metadata.ModelMetadata;
import com.elfmcys.ysm.format.parser.pojo.manifest.settings.ConfigForms;
import com.elfmcys.ysm.format.parser.pojo.manifest.settings.ExtraAnimationButton;
import com.elfmcys.ysm.format.parser.pojo.manifest.settings.ExtraAnimationClassify;
import com.elfmcys.ysm.format.parser.pojo.manifest.settings.ModelProperties;
import mixel.common.ImageOuterClass;
import mixel.common.StringPairOuterClass;
import mixel.manifest.ManifestOuterClass;
import mixel.manifest.info.MetadataOuterClass;
import mixel.manifest.info.ModelSettingsOuterClass;
import mixel.manifest.info.SettingsOuterClass;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import us.hebi.quickbuf.RepeatedMessage;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class ModelInfoBuilder {
    private ModelInfoBuilder() {
    }

    static void writeInfo(@Nullable ManifestOuterClass.Manifest manifest,
                          ModelManifest sourceManifest,
                          String originVersion,
                          ImageBlobReader imageReader) throws IOException {
        var info = manifest == null ? null : manifest.getMutableInfo();
        var properties = sourceManifest.properties;

        var settings = info == null ? null : info.getMutableSettings();
        if (info != null) {
            info.getMutableProperties()
                    .setOriginVer(originVersion)
                    .setFree(properties.free);

            settings.setDefaultTexture(Objects.requireNonNull(properties.defaultTexture, ""))
                    .setPreviewAnimation(Objects.requireNonNull(properties.previewAnimation, ""))
                    .setDisablePreviewRotation(properties.disablePreviewRotation);
            addStringPairs(settings::getMutableExtraAnimation, properties.extraAnimation);
            for (var button : properties.extraAnimationButtons) {
                if (button != null) {
                    settings.addExtraAnimationButtons(toProto(button));
                }
            }
            for (var classify : properties.extraAnimationClassify) {
                if (classify != null) {
                    settings.addExtraAnimationClassify(toProto(classify));
                }
            }
        }

        if (properties.guiForeground != null) {
            imageReader.read(properties.guiForeground, "gui-foreground",
                    RawImageCompressor.GUI_IMAGE, settings == null ? null : settings::setGuiForeground);
        }
        if (properties.guiBackground != null) {
            imageReader.read(properties.guiBackground, "gui-background",
                    RawImageCompressor.GUI_IMAGE, settings == null ? null : settings::setGuiBackground);
        }

        if (sourceManifest.metadata != null) {
            writeMetadata(info == null ? null : info.getMutableMetadata(), sourceManifest.metadata, imageReader);
        }
    }

    static ModelSettingsOuterClass.ModelSettings modelSettings(ModelProperties properties) {
        return ModelSettingsOuterClass.ModelSettings.newInstance()
                .setHeightScale(properties.heightScale)
                .setWidthScale(properties.widthScale)
                .setRenderLayersFirst(properties.renderLayersFirst)
                .setForceCulling(properties.forceCulling)
                .setGuiNoLighting(properties.guiNoLighting)
                .setMergeMultilineExpr(properties.mergeMultilineExpr);
    }

    private static void writeMetadata(@Nullable MetadataOuterClass.Metadata dst,
                                      ModelMetadata src,
                                      ImageBlobReader imageReader)
            throws IOException {
        if (dst != null) {
            dst.setName(Objects.requireNonNull(src.name, ""))
                    .setTips(Objects.requireNonNull(src.tips, ""));
            dst.getMutableLicense()
                    .setType((src.license != null && src.license.type != null) ? src.license.type : "All Rights Reserved")
                    .setDesc((src.license != null && src.license.desc != null) ? src.license.desc : "");
        }
        if (src.authors != null) {
            for (var author : src.authors) {
                if (author == null) {
                    continue;
                }
                var proto = dst == null ? null : MetadataOuterClass.Author.newInstance()
                        .setName(Objects.requireNonNull(author.name, ""))
                        .setRole(Objects.requireNonNull(author.role, ""))
                        .setComment(Objects.requireNonNull(author.comment, ""));
                if (proto != null) {
                    addStringPairs(proto::getMutableContacts, author.contact);
                }
                if (StringUtils.isNotBlank(author.avatar)) {
                   imageReader.read(author.avatar, "avatar",
                           RawImageCompressor.AUTHOR_AVATAR, proto == null ? null : proto::setAvatar);
                }
                if (proto != null) {
                    Objects.requireNonNull(dst, "dst").addAuthors(proto);
                }
            }
        }

        if (dst != null) {
            addStringPairs(dst::getMutableLinks, src.link);
        }
    }

    private static SettingsOuterClass.ExtraAnimationButton toProto(ExtraAnimationButton src) {
        var dst = SettingsOuterClass.ExtraAnimationButton.newInstance()
                .setId(Objects.requireNonNull(src.id, ""))
                .setName(Objects.requireNonNull(src.name, ""))
                .setSound(Objects.requireNonNull(src.sound, ""));
        if (src.configForms != null) {
            for (var form : src.configForms) {
                if (form != null) {
                    dst.addConfigForms(toProto(form));
                }
            }
        }
        return dst;
    }

    private static SettingsOuterClass.ConfigForms toProto(ConfigForms src) {
        var dst = SettingsOuterClass.ConfigForms.newInstance()
                .setType(Objects.requireNonNullElse(src.type, ""))
                .setTitle(Objects.requireNonNullElse(src.title, ""))
                .setDescription(Objects.requireNonNullElse(src.description, ""))
                .setValue(Objects.requireNonNullElse(src.value, ""))
                .setStep((float) src.step)
                .setMin((float) src.min)
                .setMax((float) src.max);
        addStringPairs(dst::getMutableLabels, src.labels);
        return dst;
    }

    private static SettingsOuterClass.ExtraAnimationClassify toProto(ExtraAnimationClassify src) {
        var dst = SettingsOuterClass.ExtraAnimationClassify.newInstance()
                .setId(src.id);
        addStringPairs(dst::getMutableExtraAnimation, src.extraAnimation);
        return dst;
    }

    private static void addStringPairs(
            Supplier<RepeatedMessage<StringPairOuterClass.StringPair>> mutableList,
            @Nullable Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        var list = mutableList.get();
        for (var entry : values.entrySet()) {
            list.add(StringPairOuterClass.StringPair.newInstance()
                    .setKey(entry.getKey())
                    .setValue(entry.getValue()));
        }
    }

    @FunctionalInterface
    interface ImageBlobReader {
        void read(String path, String hashType, RawImageCompressor.Policy policy,
                  @Nullable Consumer<ImageOuterClass.Image> consumer) throws IOException;
    }
}
