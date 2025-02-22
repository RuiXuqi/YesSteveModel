package com.elfmcys.yesstevemodel.client.animation.molang;

import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.TlmCompat;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.ContextBinding;

/**
 * 为了兼容女仆新增的 molang，即使女仆模组未安装，也应该存在，否则动画会报错
 */
public class TLMBinding extends ContextBinding {
    public static final TLMBinding INSTANCE = new TLMBinding();

    public TLMBinding() {
        TlmCompat.addBinding(this);
    }
}