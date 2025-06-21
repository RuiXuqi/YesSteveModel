package com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.animation.molang.CustomMolangParser;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.capability.YsmMaidCapabilityProvider;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.info.ModelProperties;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.molang.parser.ParseException;
import com.elfmcys.yesstevemodel.network.message.data.RoamingVarsChanges;
import com.elfmcys.yesstevemodel.util.FifoHashMap;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

/**
 * 这个类是客户端和服务端都可能用到的类
 */
public class TlmCommonCompatInner {
    static boolean isMaid(Entity entity) {
        return entity instanceof EntityMaid;
    }

    @OnlyIn(Dist.CLIENT)
    static void handleExecuteMolang(Entity entity, String molangExpression) {
        if (!(entity instanceof EntityMaid maid)) {
            return;
        }
        if (!maid.isYsmModel()) {
            return;
        }
        maid.getCapability(YsmMaidCapabilityProvider.CAP).ifPresent(cap -> {
            try {
                IValue value = CustomMolangParser.parseSingleExpressionUnsafe(molangExpression);
                cap.executeMolangExp(value, true, false, null);
            } catch (ParseException e) {
                YesSteveModel.LOGGER.error("Failed to execute molang " + molangExpression, e);
            }
        });
    }

    static void setRouletteAnima(Entity entity, String classifyId, int extraAnimIndex) {
        if (!(entity instanceof EntityMaid maid)) {
            return;
        }
        if (!maid.isYsmModel()) {
            return;
        }
        if (extraAnimIndex == -1) {
            maid.stopRouletteAnim();
            return;
        }
        String modelId = maid.getYsmModelId();
        ServerModelManager.getModel(modelId).ifPresent(model -> {
            ModelProperties properties = model.info().properties();
            var classifyMap = properties.extraAnimationClassifyMap();
            FifoHashMap<String, String> map;

            if (StringUtils.isNotBlank(classifyId) && classifyMap.containsKey(classifyId)) {
                map = classifyMap.get(classifyId);
            } else {
                map = properties.extraAnimationOrderMap();
            }

            if (map.size() > extraAnimIndex) {
                maid.playRouletteAnim(map.getKeyAt(extraAnimIndex));
            }
        });
    }

    static void handleVariableChanges(Entity entity, RoamingVarsChanges changes) {
        if (!(entity instanceof EntityMaid maid)) {
            return;
        }
        if (!maid.isYsmModel()) {
            return;
        }
        // TODO: 女仆 roaming 变量更新
    }
}
