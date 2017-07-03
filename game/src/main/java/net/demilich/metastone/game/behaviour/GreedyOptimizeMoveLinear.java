package net.demilich.metastone.game.behaviour;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.heuristic.IGameStateHeuristic;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.logic.GameLogic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GreedyOptimizeMoveLinear extends Behaviour {

	private final static Logger logger = LoggerFactory.getLogger(GreedyOptimizeMoveLinear.class);

	private final IGameStateHeuristic heuristic;

	public GreedyOptimizeMoveLinear(IGameStateHeuristic heuristic) {
		this.heuristic = heuristic;
	}

	@Override
	public String getName() {
		return "Greedy Best Move Linear";
	}

	@Override
	public List<Card> mulligan(GameContext context, Player player, List<Card> cards) {
		List<Card> discardedCards = new ArrayList<Card>();
		for (Card card : cards) {
			if (card.getBaseManaCost() >= 4) {  //耗法值>=4的不要
				discardedCards.add(card);
			}
		}
		return discardedCards;
	}

	@Override
	public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {
		// Sjx, output info
		//logger.info("num of valid actions {}, they are {}.", validActions.size(), validActions);

		if (validActions.size() == 1) {  //只剩一个action一般是 END_TURN
			return validActions.get(0);
		}
		GameAction bestAction = validActions.get(0);
		double bestScore = Double.NEGATIVE_INFINITY;
		logger.debug("Current game state has a score of {}", bestScore, hashCode());
		for (GameAction gameAction : validActions) {
			GameContext simulationResult = simulateAction(context.clone(), player, gameAction);  //假设执行gameAction，得到之后的game context
			double gameStateScore = heuristic.getScore(simulationResult, player.getId());	     //heuristic评估执行gameAction之后的游戏局面的分数
			logger.debug("Action {} gains score of {}", gameAction, gameStateScore);
			if (gameStateScore > bestScore) {		// 记录得分最高的action
				bestScore = gameStateScore;
				bestAction = gameAction;
				logger.debug("BEST ACTION SO FAR id:{}", bestAction.hashCode());
			}
			simulationResult.dispose();  //GameContext环境每次仿真完销毁

		}
		logger.debug("Performing best action: {}", bestAction);

		return bestAction;
	}

	private GameContext simulateAction(GameContext simulation, Player player, GameAction action) {
		GameLogic.logger.debug("");
		GameLogic.logger.debug("********SIMULATION starts********** " + simulation.getLogic().hashCode());
		simulation.getLogic().performGameAction(player.getId(), action);   // 在simulation GameContext中执行action，似乎是获取logic模块来执行action的
		GameLogic.logger.debug("********SIMULATION ends**********");
		GameLogic.logger.debug("");
		return simulation;
	}

}
