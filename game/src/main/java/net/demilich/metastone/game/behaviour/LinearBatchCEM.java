package net.demilich.metastone.game.behaviour;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.logic.GameLogic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

// 尝试直接使用CEM (Noisy Cross Entropy Method) 方法优化Linear value function中的参数
// 相对于Linear CEM中优化一局结束时的HpDiff， Linear Batch CEM中以优化一个batch的对局的胜率为目标
// 也就是说一组参数下跑batch局，以这组参数和这batch局的胜率作为一个样本

public class LinearBatchCEM extends Behaviour {

	private final static Logger logger = LoggerFactory.getLogger(LinearBatchCEM.class);
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
	private final static int batchSize = 50;
	private final static int updateBatchSize = 20;
	private final static double topRatio = 0.25;

	// 初始化par均值
//	double[] coef0 = {-1.0908019161141327, 0.6010612694539739, -2.5866502811370067, 2.22006504329619, 1.5848131785872193, -1.5805967137590908, 0.2941482345080195, 2.438458701747288, 1.6271195269493988, 0.7199948277761504, 0.506913009963485, -1.1279103773463786, 2.047988618707141, -1.57363758325508, -2.1533199260998135, -1.5920098329344945, 0.8910700740567333, 0.10653911094993662, -0.31931599483632694, 1.1156702570688315, 0.7425087545093341, 0.7080486811044453, 0.26080540244703476, -2.4334761063404704, 2.4431256407143143, 0.6575185973320499, 1.625164010147523, -0.45913914628255625, -0.23061990409221683, 0.08987186060534504};

	public LinearBatchCEM() {
		for(int i=0; i<feaNum; i++){
			parMean[i] = 0; //coef0[i]; //2*random.nextDouble() - 1;
			parVar[i] = 0.25;
		}
		updateParWeight();
	}

	@Override
	public String getName() {
		return "CEM-Batch-Linear";
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

		// get best action at the current state and the corresponding Q-score
		GameAction bestAction = validActions.get(0);
		double bestScore = Double.NEGATIVE_INFINITY;
		for (GameAction gameAction : validActions) {
			GameContext simulationResult = simulateAction(context.clone(), player, gameAction);  //假设执行gameAction，得到之后的game context
			double gameStateScore = evaluateContext(simulationResult, player.getId());  //heuristic.getScore(simulationResult, player.getId());	     //heuristic评估执行gameAction之后的游戏局面的分数
			if (gameStateScore > bestScore) {		// 记录得分最高的action
				bestScore = gameStateScore;
				bestAction = gameAction;
			}
			simulationResult.dispose();  //GameContext环境每次仿真完销毁
		}

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
