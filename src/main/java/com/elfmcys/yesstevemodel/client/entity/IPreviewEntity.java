package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.client.gui.PreviewAnimationInfo;
import org.jetbrains.annotations.NotNull;

public interface IPreviewEntity {
    @NotNull
    PreviewAnimationInfo getPreviewInfo();

    void waitForCapabilityUpdate();
}
