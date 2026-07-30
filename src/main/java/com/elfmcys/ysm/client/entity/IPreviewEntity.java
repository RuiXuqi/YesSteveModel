package com.elfmcys.ysm.client.entity;

import com.elfmcys.ysm.client.gui.PreviewAnimationInfo;
import org.jetbrains.annotations.NotNull;

public interface IPreviewEntity {
    @NotNull
    PreviewAnimationInfo getPreviewInfo();

    void setAllowEmitting(boolean allow);
}
