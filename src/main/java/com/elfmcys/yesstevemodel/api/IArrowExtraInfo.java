package com.elfmcys.yesstevemodel.api;

import javax.annotation.Nullable;

public interface IArrowExtraInfo {
    String EMPTY_MODEL_NAME = "empty";

    boolean isInGround();

    int inGroundTime();

    /**
     * @return 箭所拥有的模型对象
     */
    @Nullable
    Object getGeoInstance();
}
