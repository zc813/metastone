package net.demilich.metastone.game.behaviour;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.ActionType;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.logic.GameLogic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

// 尝试直接使用CEM (Noisy Cross Entropy Method) 方法优化Linear value function中的参数, 但现在会往前多步后评估局面，而不是GreedyBest

public class GameTreeBatchCEM extends Behaviour {

	private final static Logger logger = LoggerFactory.getLogger(GameTreeBatchCEM.class);
	private Random random = new Random();
	private final static int feaNum = 30;
	private static double[] parMean = new double[feaNum];
	private static double[] parVar = new double[feaNum];
	private double[] parWeight = new double[feaNum];
	private static ArrayList<double[]> paraList = new ArrayList<>();
	private static SortedMap<Integer, List<Integer>> rewardMap = new TreeMap<>(Comparator.reverseOrder());
	private static int gameCount = 0;
	private static int batchCount = 0;
	private static int batchWinCnt = 0;
	private static int iterNum = 0;
	private final static int batchSize = 25;
	private final static int updateBatchSize = 20;
	private final static double topRatio = 0.25;

	// 初始化par均值
	double[] coef0 = {-0.037847876197846936, -0.5793008001169108, 0.1754385206469534, 0.1946006596220002, -0.18375281281268818, 0.07669337304029306, -0.23953339605248733, 0.1625375821028113, 0.9683292339344448, 0.44773851443668594, 0.47734256985078316, -0.21879857542089906, 0.39121568056816125, -0.6008596281153088, -0.05693259780075566, -0.2351841449032277, 0.5748925305480416, -0.3515842748443804, -1.2271003656222081, 0.13591035827512069, -1.161462138557064, 0.3489161472204509, -1.1059219462733827, -0.7285302999093869, -0.6625790473565222, -0.8504436668103094, -1.4268205997337957, 1.6023593638999283, -1.0209016644769786, 0.724077140200281};

	public GameTreeBatchCEM() {
		for(int i=0; i<feaNum; i++){
			parMean[i] = coef0[i]; //2*random.nextDouble() - 1;
			parVar[i] = 0.25;
		}
		updateParWeight();
	}

	@Override
	public String getName() {
		return "CEM-Batch-Tree";
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

		if (validActions.size() == 1) {  //只剩一个action一般是 END_TURN
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
			double score = alphaBeta(context, player.getId(), gameAction, depth);  // 对每一个可能action，使用alphaBeta递归计算得分
			if (score > bestScore) {
				bestAction = gameAction;
				bestScore = score;
			}
		}
		return bestAction;
	}

	private double alphaBeta(GameContext context, int playerId, GameAction action, int depth) {
		GameContext simulation = context.clone();  // clone目前环境
		simulation.getLogic().performGameAction(playerId, action);  // 在拷贝环境中执行action
		if (depth == 0 || simulation.getActivePlayerId() != playerId || simulation.gameDecided()) {  // depth层递归结束、发生玩家切换（我方这轮打完了）或者比赛结果已定时，返回score
			return evaluateContext(simulation, playerId);
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

	private double evaluateContext(GameContext context, int playerId) {
		Player player = context.getPlayer(playerId);
		Player opponent = context.getOpponent(player);
		if (player.getHero().isDestroyed()) {   // 己方被干掉，得分 负无穷
			return Float.NEGATIVE_INFINITY;  // 正负无穷会影响envState的解析，如果要加的话可以改成 +-100之类的
		}
		if (opponent.getHero().isDestroyed()) {  // 对方被干掉，得分 正无穷
			return Float.POSITIVE_INFINITY;
		}
		List<Integer> envState = player.getPlayerState();
		envState.addAll(opponent.getPlayerState());

		double score = 0;
		for (int i = 0; i < parWeight.length; i++){
			score += parWeight[i]*envState.get(i);
		}
		return score;
	}

	private void updateParWeight(){
		// 根据参数的均值和方差，按正态分布生成parWeight
		for(int i=0; i<parWeight.length; i++){
			parWeight[i] = parMean[i] + Math.sqrt(parVar[i])*random.nextGaussian();
		}
	}

	private double calcMean(double[] paras){
		double mean = 0;
		for(double para : paras){
			mean += para;
		}
		mean /= paras.length;
		return mean;
	}

	private double calcVar(double[] paras){
		double mean = calcMean(paras);
		double var = 0;
		for(double para : paras){
			var += (para - mean)*(para - mean);
		}
		var /= paras.length;
		return var;
	}

	private void updateMeanVar(){
		int k = 0;
		int topNum = (int)(updateBatchSize*topRatio);
		double[][] topPara = new double[feaNum][topNum];
		double[] bestPara = new double[feaNum];
		double meanTopReward = 0;
		// 取出reward最好的若干次的参数
		for(Integer reward: rewardMap.keySet()){
			for(Integer ind: rewardMap.get(reward)){
				double[] para = paraList.get(ind);
				for(int i=0; i<para.length; i++){
					topPara[i][k] = para[i];
				}
				k++;
				if(k == 1){
					bestPara = para.clone();
				}
				meanTopReward += reward;
				if(k >= topNum){
					meanTopReward /= topNum;
					logger.info("################# iterNum: {}, meanTopReward: {}, bestPara: {} ##################", iterNum, meanTopReward, bestPara);
					// 更新均值和方差
					for(int i=0; i<feaNum; i++){
						this.parMean[i] = calcMean(topPara[i]);
						this.parVar[i] = calcVar(topPara[i]) + Math.max(0.1 - 0.01*iterNum, 0);  // 添加逐渐减小的Noise， 可调整
					}
					logger.info("########## rewardMap: {}, parMean: {}, parVar: {}", rewardMap, parMean, parVar);
					// 清空这一个batch的数据
					paraList.clear();
					rewardMap.clear();
					return;
				}
			}
		}
	}

	@Override
	public void onGameOver(GameContext context, int playerId, int winningPlayerId) {
		// GameOver的时候会跳入这个函数
//		Player player = context.getPlayer(playerId);
//		Player opponent = context.getOpponent(player);
//		List<Integer> playerState = player.getPlayerState();
//		List<Integer> opponentState = opponent.getPlayerState();
//		int reward = player.getHero().getHp() - opponent.getHero().getHp();

		gameCount++;
		if(playerId == winningPlayerId){
			batchWinCnt += 1;
		}

		// 一个Batch结束
		if(gameCount == batchSize){
			logger.info("batchCount: {}, batchWinCnt: {}", batchCount, batchWinCnt);
			int reward = batchWinCnt;
			// 保存这一Batch的最终batchWinCnt和对应batchCount编号（从0开始编号）
			if(rewardMap.containsKey(reward)){
				rewardMap.get(reward).add(batchCount);
			}else{
				rewardMap.put(reward, new ArrayList<>(Arrays.asList(batchCount)));
			}
			// 保存这一Batch使用的模型参数
			paraList.add(parWeight.clone());
			batchCount++;
			gameCount = 0;
			batchWinCnt = 0;
			// 根据均值和方差，随机生成下一Batch使用的权重参数
			updateParWeight();
		}

		// 执行一个updateBatchSize之后, 更新参数均值和方差
		if(batchCount == updateBatchSize){
			iterNum++;
//			logger.info("rewardMap: {}, para: {}, parMean: {}, parVar: {}", rewardMap, parWeight, parMean, parVar);
			// 更新参数均值和方差, 并清空这一个batch的数据
			updateMeanVar();
			batchCount = 0;
//			logger.info("########## rewardMap: {}, para: {}, parMean: {}, parVar: {}", rewardMap, parWeight, parMean, parVar);
		}
	}
}
