package net.demilich.metastone.game.behaviour.mcts;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.PlayRandomBehaviour;
import net.demilich.metastone.game.behaviour.heuristic.IGameStateHeuristic;

public class ValueNode extends Node{
    private IGameStateHeuristic heuristic;
    public ValueNode(GameAction incomingAction, int player, IGameStateHeuristic heuristic) {
        super(incomingAction, player);
        this.heuristic = heuristic;
    }

    /**
     * 如果游戏胜负已分，返回确定值。
     * 否则，返回 heuristic 评估的分数。
     *
     * @return player 对应的 rollOut 结果（采用模型评估）
     */
    @Override
    public double rollOut(Node node){
        if (node.getState().gameDecided()) {
            GameContext state = node.getState();
            return state.getWinningPlayerId() == getPlayer() ? 1 : 0;
        }

        // 注意这里的 player 是父节点的 current player
        // 也就是子节点的 incomingAction 所对应的 player
        // 往往不是 node.getState().getActivePlayerId() 的 player
        // 这是因为这个返回值本来就需要代表该 player 在此种局面下的获胜情况
        // 实验也说明如果改成 getActivePlayerId() 胜率基本为 0
        return heuristic.getScore(node.getState(), getPlayer());
    }

    @Override
    protected Node createNode(GameAction incomingAction, int player) {
        return new ValueNode(incomingAction, player, heuristic);
    }
}
