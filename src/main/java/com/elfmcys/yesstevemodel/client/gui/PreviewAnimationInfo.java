package com.elfmcys.yesstevemodel.client.gui;

import org.apache.commons.lang3.StringUtils;

public class PreviewAnimationInfo {
    private String preview = "";
    private String hover = "";
    private String focus = "";

    public String getPreview() {
        return preview;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }

    public boolean hasPreview() {
        return StringUtils.isNoneBlank(this.preview);
    }

    public boolean hasPreview(String previewAnimation) {
        return hasPreview() && previewAnimation.equals(this.preview);
    }

    public String getHover() {
        return hover;
    }

    public String getFocus() {
        return focus;
    }

    public void setHover(String hover) {
        this.hover = hover;
    }

    public void setFocus(String focus) {
        this.focus = focus;
    }
}
