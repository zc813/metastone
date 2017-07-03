package net.demilich.metastone.game.behaviour;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.ActionType;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.heuristic.IGameStateHeuristic;
import net.demilich.metastone.game.cards.Card;

public class GreedyOptimizeTurn extends Behaviour {

	private final Logger logger = LoggerFactory.getLogger(GreedyOptimizeTurn.class);

	private final IGameStateHeuristic heuristic;

	private int assignedGC;
	private final HashMap<ActionType, Integer> evaluatedActions = new HashMap<ActionType, Integer>();
	private final TranspositionTable table = new TranspositionTable();

	public GreedyOptimizeTurn(IGameStateHeuristic heuristic) {
		this.heuristic = heuristic;
	}

	private double alphaBeta(GameContext context, int playerId, GameAction action, int depth) {
		GameContext simulation = context.clone();  // clone当前环境
		simulation.getLogic().performGameAction(playerId, action); // 在clone的环境中执行action
		if (!evaluatedActions.containsKey(action.getActionType())) { //evaluatedActions中似乎记录的是每种actionType的action的执行次数
			evaluatedActions.put(action.getActionType(), 0); // 如果当前执行的action的actionType还没出现过，增加一个key，value设置为0
		}
		evaluatedActions.put(action.getActionType(), evaluatedActions.get(action.getActionType()) + 1); //在evaluatedActions中将刚执行的actionType的对应value加1
		if (depth == 0 || simulation.getActivePlayerId() != playerId || simulation.gameDecided()) {
			return heuristic.getScore(simulation, playerId);  // depth层递归结束、发生玩家切换（我方这轮打完了）或者比赛结果已定时，使用heuristic方法评估当前局面，返回score
		}

		List<GameAction> validActions = simulation.getValidActions();  //执行完一个action之后，获取接下来可以执行的action

		double score = Float.NEGATIVE_INFINITY;
		if (table.known(simulation)) {
			return table.getScore(simulation); // 只是为了避免重复计算？
			// logger.info("GameState is known, has score of {}", score);
		} else {
			for (GameAction gameAction : validActions) {
				score = Math.max(score, alphaBeta(simulation, playerId, gameAction, depth - 1));  // 进一步遍历validactions，递归调用alphaBeta，取评分较大的
				if (score >= 10000) {
					break;
				}
			}
			table.save(simulation, score);  // 保存一个局面和它的得分（从这个局面开始执行depth-1次action后能达到的最高局面评分）
		}

		return score;
	}

	@Override
	public IBehaviour clone() {
		try {
			return new GreedyOptimizeTurn(heuristic.getClass().newInstance());
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String getName() {
		return "Min-Max Turn";
	}

	@Override
	public List<Card> mulligan(GameContext context, Player player, List<Card> cards) {
		List<Card> discardedCards = new ArrayList<Card>();
		for (Card card : cards) {
			if (card.getBaseManaCost() >= 4) {
				discardedCards.add(card);
			}
		}
		return discardedCards;
	}

	@Override
	public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {
		if (validActions.size() == 1) {
			heuristic.onActionSelected(context, player.getId());
			return validActions.get(0);
		}

		// for now, do now evaluate battecry actions
		if (validActions.get(0).getActionType() == ActionType.BATTLECRY) {   // 不清楚为什么战吼需要特殊处理？
			return validActions.get(context.getLogic().random(validActions.size()));
		}

		if (assignedGC != 0 && assignedGC != context.hashCode()) {   // 意味着什么？
			logger.warn("AI behaviour was used in another context!");
		}

		assignedGC = context.hashCode();
		evaluatedActions.clear();
		table.clear();

		GameAction bestAction = validActions.get(0);
		double bestScore = Double.NEGATIVE_INFINITY;

		for (GameAction gameAction : validActions) {
			logger.debug("********************* SIMULATION STARTS *********************");
			double score = alphaBeta(context, player.getId(), gameAction, 3);  // 遍历所有validaction，使用alphaBeta计算评分
			if (score > bestScore) {
				bestAction = gameAction;
				bestScore = score;
			}
			logger.debug("********************* SIMULATION ENDS, Action {} achieves score {}", gameAction, score);
		}

		int totalActionCount = 0;
		for (ActionType actionType : evaluatedActions.keySet()) {
			int count = evaluatedActions.get(actionType);
			logger.debug("{} actions of type {} have been evaluated this turn", count, actionType);  // 只是为了观察一些信息么？
			totalActionCount += count;
		}
		logger.debug("{} actions in total have been evaluated this turn", totalActionCount); // evaluate过的action总数，有什么用呢？
		logger.debug("Selecting best action {} with score {}", bestAction, bestScore);
		heuristic.onActionSelected(context, player.getId());

		return bestAction;
	}

	/*private double simulateAction(GameContext context, int playerId, GameAction action) {
		GameContext simulation = context.clone();
		simulation.getLogic().performGameAction(playerId, action);
		if (!evaluatedActions.containsKey(action.getActionType())) {
			evaluatedActions.put(action.getActionType(), 0);
		}
		evaluatedActions.put(action.getActionType(), evaluatedActions.get(action.getActionType()) + 1);
		if (simulation.getActivePlayerId() != playerId || simulation.gameDecided()) {
			return heuristic.getScore(simulation, playerId);
		}
		List<GameAction> validActions = simulation.getValidActions();
		if (validActions.size() == 0) {
			throw new RuntimeException("No more possible moves, last action was: " + action);
		}
		double bestScore = Integer.MIN_VALUE;
		for (GameAction gameAction : validActions) {
			bestScore = Math.max(bestScore, simulateAction(simulation, playerId, gameAction));
		}
		return bestScore;
	}*/

}
