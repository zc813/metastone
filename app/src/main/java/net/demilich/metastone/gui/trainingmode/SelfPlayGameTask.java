package net.demilich.metastone.gui.trainingmode;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.PlayGameTask;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.behaviour.models.GamePlayData;
import net.demilich.metastone.game.behaviour.models.SerialGamePlayData;
import net.demilich.metastone.game.decks.DeckFormat;
import net.demilich.metastone.game.gameconfig.GameConfig;
import net.demilich.metastone.game.gameconfig.PlayerConfig;
import net.demilich.metastone.game.logic.GameLogic;

import java.util.List;

public class SelfPlayGameTask extends PlayGameTask {
    private GamePlayData gamePlayData1, gamePlayData2;
    private boolean gameEnded = false;

    public SelfPlayGameTask(Player player){
        super(player, player);
        gamePlayData1 = new GamePlayData();
        gamePlayData2 = new GamePlayData();
    }

    /**
     * 每次 perform 完一个 action 的时候分别根据 player 把特征计算后存到对应的数据里
     * 这是为了方便游戏结束后把输的玩家和赢的玩家分开
     */
    @Override
    protected void onActionEnd(GameContext context, int playerId) {
        if (playerId == GameContext.PLAYER_1)
            gamePlayData1.add(context, playerId);
        else
            gamePlayData2.add(context, playerId);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onGameEnd(GameContext context, int winner){
        for (List l : gamePlayData1.getData()){
            l.add(winner==GameContext.PLAYER_1?1.0:0.0); // 每个特征后面最后加一个输赢
        }
        for (List l : gamePlayData2.getData()){
            l.add(winner==GameContext.PLAYER_2?1.0:0.0); // 每个特征后面最后加一个输赢
        }
        gamePlayData1.merge(gamePlayData2);
        gameEnded = true;
    }

    public GamePlayData getData(){
        if (gameEnded)
            return gamePlayData1;
        else
            throw new RuntimeException("Game not completed!");
    }
}
