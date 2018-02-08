package net.demilich.metastone.game.behaviour.mcts;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.TurnState;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.PlayRandomBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Node {

	private static final Logger logger = LoggerFactory.getLogger(Node.class);
	private GameContext state;
	private List<GameAction> validTransitions;
	private final List<Node> children = new LinkedList<>();
	private final GameAction incomingAction;
	private int visits;
	private double score;
	private final int player;

	/**
	 * 父节点 player 完成某个 action 后来到该节点
	 *
	 * @param incomingAction 该节点从父节点经过哪个动作而来
	 *                          （该节点状态是已经 perform 完这个 incomingAction 了）
	 * @param player 父节点的 currentPlayer
	 *                  （也就是做了 incomingAction 的那个 player）
	 *               	（不一定是当前节点的 currentPlayer！）
	 */
	public Node(GameAction incomingAction, int player) {
		this.incomingAction = incomingAction;
		this.player = player;
	}

	private boolean canFurtherExpanded() {
		return !validTransitions.isEmpty();
	}

	private Node expand() {
		GameAction action = validTransitions.remove(0);
		GameContext newState = state.clone();
		int incomingPlayer = newState.getActivePlayer().getId();

		try {
			newState.getLogic().performGameAction(incomingPlayer, action);

			// 我方结束后换 turn
			if (newState.getTurnState() == TurnState.TURN_ENDED)
				newState.startTurn(newState.getActivePlayer().getId());
		} catch (Exception e) {
			System.err.println("Exception on action: " + action + " state decided: " + state.gameDecided());
			e.printStackTrace();
			throw e;
		}

		// 在 ModelMCTS 中所使用的 ValueNode 和此类的构造方法不同，
		// 在拓展时使用 createNode 方法
		//
		// 注意：incomingPlayer 是 perform 前的 player，与 action 是对应的
		Node child = createNode(action, incomingPlayer);

		child.initState(newState, newState.getValidActions());
		children.add(child);
		return child;
	}

	public GameAction getBestAction() {
		GameAction best = null;
		double bestScore = Double.MIN_VALUE;
		for (Node node : children) {
			if (node.getScore() > bestScore) {
				best = node.incomingAction;
				bestScore = node.getScore(); // 因为次数常常相同，所以用 score
			}
		}
		return best;
	}

	public List<Node> getChildren() {
		return children;
	}

	public int getPlayer() {
		return player;
	}

	public double getScore() {
		return score;
	}

	public GameContext getState() {
		return state;
	}

	public int getVisits() {
		return visits;
	}

	public void initState(GameContext state, List<GameAction> validActions) {
		this.state = state.clone();
		this.validTransitions = new ArrayList<GameAction>(validActions);
	}

	public boolean isExpandable() {
		if (validTransitions.isEmpty()) {
			return false;
		}
		if (state.gameDecided()) {
			return false;
		}
		return getChildren().size() < validTransitions.size();
	}

	public boolean isLeaf() {
		return children == null || children.isEmpty();
	}

	private boolean isTerminal() {
		return state.gameDecided();
	}

	public void process(ITreePolicy treePolicy) {
		List<Node> visited = new LinkedList<Node>();  // back propagation 路径
		Node current = this;
		visited.add(this);
		while (!current.isTerminal()) { // 如果游戏还没结束
			if (current.canFurtherExpanded()) { // 还有动作没拓展时优先都拓展完
				current = current.expand();
				visited.add(current);
				break;
			} else { // 已经拓展完的话选一个访问
				current = treePolicy.select(current);
				visited.add(current);
			}
		}

		double value = rollOut(current);
		for (Node node : visited) {
			if (node.getPlayer() == getPlayer())
				node.updateStats(value);
			else
				// 对手，反转值（0~1）
				// 如果你用的是 -1~1 的话需要改成 -value
				node.updateStats(1-value);
		}
	}

	public double rollOut(Node node) {
		if (node.getState().gameDecided()) {
			GameContext state = node.getState();
			return state.getWinningPlayerId() == getPlayer() ? 1 : 0; // 理论上在 ucb1 的公式下用 0~1 和 -1~1 没区别
		}

		GameContext simulation = node.getState().clone();
		for (Player player : simulation.getPlayers()) {
			player.setBehaviour(new PlayRandomBehaviour());
		}

		//simulation.playTurn();
		simulation.playFromState(); // 两个随机玩家一直对战到游戏结束

		return simulation.getWinningPlayerId() == getPlayer() ? 1 : 0;
	}

	private void updateStats(double value) {
		visits++;
		score += value;
	}

	protected Node createNode(GameAction incomingAction, int player){
		return new Node(incomingAction, player);
	}

}
