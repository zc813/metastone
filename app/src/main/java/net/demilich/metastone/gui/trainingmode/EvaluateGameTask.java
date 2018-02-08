package net.demilich.metastone.gui.trainingmode;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.PlayGameTask;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.gameconfig.GameConfig;
import net.demilich.metastone.utils.Tuple;

public class EvaluateGameTask extends PlayGameTask {
    private float win1=0, win2=0;

    public EvaluateGameTask(Player player1, Player player2){
        super(player1, player2);
    }

    public Tuple<Float, Float> getResult(){
        return new Tuple<>(win1, win2);
    }

    @Override
    protected void onTurnStart(GameContext context, int playerId){ }

    @Override
    protected void onTurnEnd(GameContext context, int playerId){ }

    @Override
    protected void onGameEnd(GameContext context, int winner){
        if (winner == GameContext.PLAYER_1)
            win1++;
        else if (winner == GameContext.PLAYER_2)
            win2++;
        else{
            win1 += 0.5;
            win2 += 0.5;
        }

    }
}
