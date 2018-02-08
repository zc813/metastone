package net.demilich.metastone.game.behaviour.mcts;

import java.util.ArrayList;
import java.util.List;
 import java.util.concurrent.ThreadLocalRandom;

import net.demilich.metastone.game.actions.ActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.Behaviour;
import net.demilich.metastone.game.cards.Card;

public class MonteCarloTreeSearch extends Behaviour {

	private final static Logger logger = LoggerFactory.getLogger(MonteCarloTreeSearch.class);

	private int iterations = 1000;

	@Override
	public String getName() {  // 这个似乎还没有完整实现，没法跑， Node.process() 报NullPointer Exception
		return "MCTS";
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

    /**
     * 进行 Monte Carlo Tree Search，并且返回最佳动作
     * 遇到战吼或者发现类型时，上一个动作还没 perform 完，这时随机选择一个。
     * （尝试过把这两种的 requestAction 也加入模拟，成本较高，收益不太大）
     *
     * @return 最佳动作
     */
	@Override
	public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {
		if (validActions.get(0).getActionType() == ActionType.BATTLECRY || validActions.get(0).getActionType() == ActionType.DISCOVER){
			return validActions.get(ThreadLocalRandom.current().nextInt(validActions.size()));
		}

		if (validActions.size() == 1) {
			logger.info("MCTS selected the only action {}", validActions.get(0));
			return validActions.get(0);
		}
		Node root = new Node(null, player.getId());
		root.initState(context, validActions); // 这里会把玩家都复制
		GameContext state = root.getState();
		for (Player otherPlayer : state.getPlayers())
			if (otherPlayer != state.getActivePlayer())
				otherPlayer.setBehaviour(this);  // 避免在 MCTS 过程中请求对手 behavior

		UctPolicy treePolicy = new UctPolicy();
		for (int i = 0; i < iterations; i++) {
//			logger.info(String.valueOf(i)); // 看看是不是有按正常执行
			root.process(treePolicy);
		}
		GameAction bestAction = root.getBestAction();
		logger.info("MCTS selected best action {}", bestAction);
		return bestAction;
	}

	public void setIterations(int iterations) {
		this.iterations = iterations;
	}
}
