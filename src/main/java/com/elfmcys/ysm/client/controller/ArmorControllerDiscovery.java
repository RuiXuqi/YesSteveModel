package com.elfmcys.ysm.client.controller;

import com.elfmcys.ysm.client.entity.CustomEntity;
import com.elfmcys.ysm.client.entity.IPreviewEntity;
import com.elfmcys.ysm.client.model.CommonAsset;
import com.elfmcys.ysm.client.model.PlayerModel;
import com.elfmcys.ysm.geckolib3.core.controller.IAnimationController;
import net.minecraft.world.entity.EquipmentSlot;
import org.apache.commons.lang3.function.TriFunction;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class ArmorControllerDiscovery<T extends CustomEntity<?>> implements ControllerDiscovery<T, PlayerModel> {
    private final String category;
    private final String name;
    private final ResourceAdapter<PlayerModel> resourceAdapter;
    private final TriFunction<String, T, EquipmentSlot, IAnimationController<T>> controllerFunc;

    public ArmorControllerDiscovery(String category, String name, ResourceAdapter<PlayerModel> resourceAdapter, TriFunction<String, T, EquipmentSlot, IAnimationController<T>> controllerFunc) {
        this.category = category;
        this.name = name;
        this.resourceAdapter = resourceAdapter;
        this.controllerFunc = controllerFunc;
    }

    @Override
    public ControllerFactory<T> process(PlayerModel playerModel, CommonAsset assets) {
        List<Pair<String, EquipmentSlot>> slots = new ArrayList<>();
        var condition = resourceAdapter.getArmorCondition(playerModel, assets);
        var controllers = resourceAdapter.getControllers(playerModel, assets);
        var animations = resourceAdapter.getAnimations(playerModel, assets);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            var controllerName = String.format("%s.%s_%s", category, name, slot.getName());
            if (controllers.containsKey(controllerName)) {
                slots.add(Pair.of(controllerName, slot));
                continue;
            }
            if (assets.eventHandlers().containsKey(String.format("%s_ctrl_%s_%s", category, name, slot.getName()))) {
                slots.add(Pair.of(controllerName, slot));
                continue;
            }
            if (slot.getType() == EquipmentSlot.Type.ARMOR && (condition.hasTest(slot) || animations.containsKey(slot.getName() + ":default"))) {
                slots.add(Pair.of(controllerName, slot));
                continue;
            }
        }
        return (animatable, consumer) -> {
            if (!(animatable instanceof IPreviewEntity)) {
                for (var slot : slots) {
                    consumer.accept(controllerFunc.apply(slot.getLeft(), animatable, slot.getRight()));
                }
            }
        };
    }
}
