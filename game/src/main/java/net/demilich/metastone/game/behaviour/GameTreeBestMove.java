package net.demilich.metastone.game.behaviour;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.ActionType;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.heuristic.IGameStateHeuristic;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.logic.GameLogic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GameTreeBestMove extends Behaviour {

	// 使用Batch CEM, batchSize = 25; updateBatchSize = 20; topRatio = 0.25; 总局数 10000， 随机初始化N(0,0.25)， 逐渐减小的Noise: Math.max(0.1 - 0.01*iterNum, 0);
	//	############### 搜索深度 depth=2, 针对 70.4%的Greedy Best Move Linear进行优化，耗时接近5个小时, 10000局训练整体胜率 63.06%
	// 下面涉及GameTree搜索的都是100局胜率评估，可能不是很准确
	// iterNum=10时的bestPara，胜率24/25， VS Greedy Best Move 胜率：53%   VS 70.4%的Greedy Best Move Linear 胜率：46% （看来训练前期出现的24/25纯属偶然？）
//	double[] parWeight = {0.05732746865866716, -0.18140247230331047, 0.49975087073441615, 0.6473297638123358, -0.3414953192020501, 0.24758057657231658, -0.9864847025723429, 0.2025830857591307, 1.866557562088892, 0.28427872933054543, 0.28948916166271144, -0.22973753810461392, 0.21812337670778814, -1.1181972463287209, -0.26649525786320616, 0.7110708179033914, 0.6045896465264924, -0.16497316503369025, -0.5899631090184629, -0.27251239686828366, -1.029097112797488, -0.9911979194986152, -0.7430061296137774, -1.0677017071084236, -1.1183810574140896, -2.253374039215773, -1.9083246948553652, 1.8136208540273824, -0.9595916060427558, 0.8496532406887332};
	// （Good，经李宁测试）iterNum=17时的bestPara，胜率24/25， VS Greedy Best Move 胜率：71%，  VS 70.4%的Greedy Best Move Linear 胜率：73%，  VS 默认GameStateValue 胜率 58% (还是后期的结果更加靠谱,看来不能前期随便停止)
//	double[] parWeight = {-0.037847876197846936, -0.5793008001169108, 0.1754385206469534, 0.1946006596220002, -0.18375281281268818, 0.07669337304029306, -0.23953339605248733, 0.1625375821028113, 0.9683292339344448, 0.44773851443668594, 0.47734256985078316, -0.21879857542089906, 0.39121568056816125, -0.6008596281153088, -0.05693259780075566, -0.2351841449032277, 0.5748925305480416, -0.3515842748443804, -1.2271003656222081, 0.13591035827512069, -1.161462138557064, 0.3489161472204509, -1.1059219462733827, -0.7285302999093869, -0.6625790473565222, -0.8504436668103094, -1.4268205997337957, 1.6023593638999283, -1.0209016644769786, 0.724077140200281};
	// iterNum=20时的paraMean，胜率24/25， VS Greedy Best Move 胜率：76%，  VS 70.4%的Greedy Best Move Linear 胜率：67% （不稳定，出现过46%）， VS 默认GameStateValue 胜率 57%
//	double[] parWeight = {-0.01894108346479285, -0.6498351778764011, 0.2536971763176777, 0.33389224352734104, -0.42517333447394323, 0.07130314047914921, -0.17081333968834544, 0.12590660310554186, 1.5373426654938487, 0.5300651514439202, 0.4571980814872247, -0.2488889858109417, 0.3780320074483211, -0.44492566022161545, -0.11257660963744213, -0.077281489903719, 0.5254987663583898, -0.39287559962305496, -1.2106529117519496, -0.10809080305564531, -1.1197048368776241, 0.3586802266233625, -1.1308642739906707, -0.8548343728001531, -0.78415772506409, 0.6326704845661801, -1.4211089028231494, 1.1706341725914826, -0.9648905065005593, 0.8828072821175915};

	//############## 搜索深度 depth=2, 针对 默认GameStateValue进行优化， 20000局训练整体胜率 60.1%， 耗时7小时 (训练后期似乎没有提升了，另外因为训练对手的问题，看来对Greedy Best Move Linear的效果不好)
	// 发现搜索深度2训练出来的Good权重参数，如果在深度=0的情况下使用，效果很差 （VS Greedy Best Move 胜率：41%） 在深度=1的情况下使用，效果就蛮好了 （VS Greedy Best Move 胜率：73.6%  VS 默认GameStateValue 胜率 62%） 到深度=3时就很慢了，而且效果不一定好，VS 默认GameStateValue 胜率 58%
	// iterNum=8时的paraMean， VS Greedy Best Move 胜率：76%，  VS 70.4%的Greedy Best Move Linear 胜率：48% (这个有点低)， VS 默认GameStateValue 胜率 62%
//	double[] parWeight = {-1.226919005119356, -1.3227033979794256, 0.17763259677863705, -0.08237331929316685, -0.457575124812566, 1.0927324952374549, 0.35327714542210875, 0.22832997386358586, 1.8928013135630875, 1.0346298094797615, 1.6232944944396273, -0.46135852155964896, 0.5891938490070523, 0.17416572738255837, -0.3853550503769568, -0.2478716674268691, -0.14910526039726751, -0.9496741917424874, -1.386914090717463, 0.7264749572085489, -0.9283393857491135, 1.0797466731215932, -1.615008830787571, -1.315612050534872, -0.9684252134277124, -1.715006877825872, -1.5468996682627372, 2.125640263354073, -1.853508543161519, 0.6546139732540672};
	// iterNum=17时的bestPara，胜率23/25， VS Greedy Best Move 胜率：78%，  VS 70.4%的Greedy Best Move Linear 胜率：49%，  VS 默认GameStateValue 胜率 67%
//	double[] parWeight = {-0.48469589039385513, -1.4684344617708704, 0.886127246379373, 0.084134615594126, -2.9644129861724164, 1.0513313555679613, 0.08836642429330685, 0.15039803188925688, 2.2465998409753105, 1.219211422282237, 1.6201865898349233, -1.1384751140169822, 0.7608525325504604, -0.46791253208051314, -0.46592274880871076, -0.11825161732636086, 0.10443713883125631, -1.5279211971327136, -1.521842549508122, 1.1116409003914722, -1.916601171918681, 4.80137229649976, -1.5485629480590233, -1.3178461619772845, -0.7165173865020682, -2.111629065394186, -1.5505750604956616, 1.1966735209761712, -2.1063005623882307, 0.6555461603903561};
	// （Good）iterNum=22时的paraMean， VS Greedy Best Move 胜率：76%，  VS 70.4%的Greedy Best Move Linear 胜率：56% ， VS 默认GameStateValue 胜率 70%  （综合来看这个不错，选择的时候看来要选rewardMap整体分布好的，最低reward大的）
	double[] parWeight = {-0.4026557850528921, -1.4777779620148956, 0.4807519990112861, 0.17256270698849252, -3.0048219754608025, 1.2471351008867102, 0.0984930660725887, 0.10428128567415333, 2.305958663224414, 1.1694585122714134, 1.6161794705279806, -1.2488627310061495, 0.7849320841015818, -0.44585583054203787, -0.5739923937489321, -0.15309219515362008, 0.10443397417923186, -1.6386810620082035, -1.5720069467333566, 1.142923451103324, -1.8273642125940401, 3.9607177425623874, -1.5590593357541482, -1.8584667661464103, -0.7176115097989136, -1.9578732692028225, -1.555219474560269, 1.260854506957219, -2.110174979376178, 0.6617734109303619};
	// iterNum=40时的bestPara，胜率22/25， VS Greedy Best Move 胜率：69%，  VS 70.4%的Greedy Best Move Linear 胜率：49%，  VS 默认GameStateValue 胜率 68%
//	double[] parWeight = {-0.3911919433803114, -1.4764118363727834, 0.39562263892652444, 0.17872383016311882, -3.028344659422104, 1.2784718432230882, 0.09464033220057197, 0.09754508671550316, 2.2806440882368633, 1.217281242319537, 1.6172127669471419, -1.273450593722082, 0.7768778476982692, -0.49534794757244266, -0.5727578785175892, -0.1409913552161238, 0.1047117028305755, -1.6763465139003637, -1.5725574835718972, 1.1426922818699154, -1.8296914059828693, 4.71578277486832, -1.5557188194644007, -1.7630146582129207, -0.7100529487019973, -1.865291420930733, -1.5383752324035105, 1.1361230085536875, -2.0290841407760647, 0.6808540184893374};

	private final static Logger logger = LoggerFactory.getLogger(GameTreeBestMove.class);
	private final IGameStateHeuristic heuristic;

	public GameTreeBestMove(IGameStateHeuristic heuristic) {
		this.heuristic = heuristic;
	}

	@Override
	public String getName() {
		return "Game Tree Best Move";
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

}
