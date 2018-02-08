package net.demilich.metastone.game.behaviour.heuristic;

import net.demilich.metastone.game.GameContext;

public class ScalingHeuristicWrapper implements IGameStateHeuristic {
    public IGameStateHeuristic component;
    private double coeff, bias, max = Double.POSITIVE_INFINITY, min = Double.NEGATIVE_INFINITY;
    public ScalingHeuristicWrapper(IGameStateHeuristic component){
        this.component = component;
    }

    @Override
    public double getScore(GameContext context, int playerId){
        double score = component.getScore(context, playerId) * coeff + bias;
        return Double.min(Double.max(min, score), max);
    }

    @Override
    public void onActionSelected(GameContext context, int playerId){
        component.onActionSelected(context, playerId);
    }

    public void setValue(Double coeff, Double bias, Double max, Double min){
        if (bias != null) this.bias = bias;
        if (coeff != null) this.coeff = coeff;
        if (max != null) this.max = max;
        if (min != null) this.min = min;
    }
}
