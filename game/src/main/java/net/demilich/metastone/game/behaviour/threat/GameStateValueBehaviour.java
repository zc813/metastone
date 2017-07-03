package net.demilich.metastone.game.behaviour.threat;

import java.util.ArrayList;
import java.util.List;

import net.demilich.metastone.NotificationProxy;
import net.demilich.metastone.game.behaviour.heuristic.SupervisedLinearHeuristic;
import net.demilich.metastone.trainingmode.RequestTrainingDataNotification;
import net.demilich.metastone.trainingmode.TrainingData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.ActionType;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.Behaviour;
import net.demilich.metastone.game.behaviour.IBehaviour;
import net.demilich.metastone.game.behaviour.heuristic.IGameStateHeuristic;
import net.demilich.metastone.game.cards.Card;

public class GameStateValueBehaviour extends Behaviour {

	private final Logger logger = LoggerFactory.getLogger(GameStateValueBehaviour.class);

	private IGameStateHeuristic heuristic;
	private FeatureVector featureVector;
	private String nameSuffix = "";

	public GameStateValueBehaviour() {
//		this.heuristic = new SupervisedLinearHeuristic();  // test by sjx, 替换GameStateValue中使用的局面评估函数
	}

	public GameStateValueBehaviour(FeatureVector featureVector, String nameSuffix) {
		this.featureVector = featureVector;
		this.nameSuffix = nameSuffix;
		this.heuristic = new ThreatBasedHeuristic(featureVector);  // 使用的局面评估方法， 需要输入featureVector，也就是各种状态的权重
	}

	private double alphaBeta(GameContext context, int playerId, GameAction action, int depth) {
		GameContext simulation = context.clone();  // clone目前环境
		simulation.getLogic().performGameAction(playerId, action);  // 在拷贝环境中执行action
		if (depth == 0 || simulation.getActivePlayerId() != playerId || simulation.gameDecided()) {  // depth层递归结束、发生玩家切换（我方这轮打完了）或者比赛结果已定时，返回score
			return heuristic.getScore(simulation, playerId);
		}

		List<GameAction> validActions = simulation.getValidActions();  //执行完一个action之后，获取接下来可以执行的action

		double score = Float.NEGATIVE_INFINITY;

		for (GameAction gameAction : validActions) {
			score = Math.max(score, alphaBeta(simulation, playerId, gameAction, depth - 1));  // 递归调用alphaBeta，取评分较大的
			if (score >= 100000) {
				break;
			}
		}

		return score;
	}

	private void answerTrainingData(TrainingData trainingData) {
		featureVector = trainingData != null ? trainingData.getFeatureVector() : FeatureVector.getFittest();   //trainingData不为空的话用train的featureVector, 不然就用FeatureVector.getFittest()里设置好的
		heuristic = new ThreatBasedHeuristic(featureVector);
		nameSuffix = trainingData != null ? "(trained)" : "(untrained)";
	}

	@Override
	public IBehaviour clone() {
		if (featureVector != null) {
			return new GameStateValueBehaviour(featureVector.clone(), nameSuffix);
		}
		return new GameStateValueBehaviour();
	}

	@Override
	public String getName() {
		return "Game state value " + nameSuffix;
	}

	@Override
	public List<Card> mulligan(GameContext context, Player player, List<Card> cards) {
		requestTrainingData(player);
		List<Card> discardedCards = new ArrayList<Card>();
		for (Card card : cards) {
			if (card.getBaseManaCost() > 3) {  //耗法值>3的不要
				discardedCards.add(card);
			}
		}
		return discardedCards;
	}

	@Override
	public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {
		if (validActions.size() == 1) {
			return validActions.get(0);
		}

		int depth = 2;
		// when evaluating battlecry and discover actions, only optimize the immediate value （两种特殊的action）
		if (validActions.get(0).getActionType() == ActionType.BATTLECRY) {
			depth = 0;
		} else if (validActions.get(0).getActionType() == ActionType.DISCOVER) {  // battlecry and discover actions一定会在第一个么？
			return validActions.get(0);
		}

		GameAction bestAction = validActions.get(0);
		double bestScore = Double.NEGATIVE_INFINITY;

		for (GameAction gameAction : validActions) {
			double score = alphaBeta(context, player.getId(), gameAction, depth);  // 对每一个可能action，使用alphaBeta计算得分，原理？
			if (score > bestScore) {
				bestAction = gameAction;
				bestScore = score;
			}
		}

		logger.debug("Selecting best action {} with score {}", bestAction, bestScore);

		return bestAction;
	}

	private void requestTrainingData(Player player) {
		if (heuristic != null) {
			return;
		}

		RequestTrainingDataNotification request = new RequestTrainingDataNotification(player.getDeckName(), this::answerTrainingData);
		NotificationProxy.notifyObservers(request);
	}

}
