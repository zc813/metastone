package net.demilich.metastone.game.behaviour.features;

import net.demilich.metastone.game.GameContext;

import java.util.List;

public interface FeatureStrategy {
    int getWidth(); // 特征维度，tensorflow 计算的时候要用
    List calculate(GameContext context, int player); // 根据局面和玩家视角生成特征
}