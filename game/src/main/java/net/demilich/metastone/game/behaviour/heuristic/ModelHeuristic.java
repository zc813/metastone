package net.demilich.metastone.game.behaviour.heuristic;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.behaviour.models.IModel;

public class ModelHeuristic implements IGameStateHeuristic {
    private IModel model;
    private double min = 0., max = 1.;

    public ModelHeuristic(IModel model){
        this.model = model;
    }
    public ModelHeuristic(IModel model, double min, double max){
        this.model = model;
        this.min = min;
        this.max = max;
    }

    public double getScore(GameContext context, int playerId){
        Player player = context.getPlayer(playerId);
        Player opponent = context.getOpponent(player);
        if (player.getHero().isDestroyed()) {   // 己方被干掉，得分最小
            return min;
        }
        if (opponent.getHero().isDestroyed()) {  // 对方被干掉，得分最大
            return max;
        }
        double score = (double)model.predict(context, playerId);
        return Double.min(Double.max(score,min),max);
    }

    public void onActionSelected(GameContext context, int playerId){

    }

    public void setModel(IModel model) {
        this.model = model;
    }

    public IModel getModel() {
        return model;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public void setMin(double min) {
        this.min = min;
    }
}
