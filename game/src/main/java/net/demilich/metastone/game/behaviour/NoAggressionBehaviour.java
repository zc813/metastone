package net.demilich.metastone.game.behaviour;

import java.util.ArrayList;
import java.util.List;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.ActionType;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.cards.Card;

public class NoAggressionBehaviour extends Behaviour {

	@Override
	public String getName() {
		return "No Aggression";  // 这是一个完全不进攻的策略，一直召唤随从（如果可以的话），没有可召唤的就End Turn
	}

	@Override
	public List<Card> mulligan(GameContext context, Player player, List<Card> cards) {
		return new ArrayList<Card>();
	}

	@Override
	public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {
		if (validActions.size() == 1) {
			return validActions.get(0);
		}
		for (GameAction gameAction : validActions) {
			if (gameAction.getActionType() == ActionType.SUMMON) {  // 如果有可以召唤的随从就召唤
				return gameAction;
			}
		}
		return validActions.get(validActions.size() - 1);  // 没有随从可以召唤就返回最后一个valid action （好像是EndTurn，也就是结束出牌）
	}

}
