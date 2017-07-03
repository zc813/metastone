package net.demilich.metastone.game.behaviour;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.cards.Card;

public class PlayRandomBehaviour extends Behaviour {

	private final static Logger logger = LoggerFactory.getLogger(PlayRandomBehaviour.class);

	private Random random = new Random();

	@Override
	public String getName() {
		return "Play Random";
	}

	@Override
	public List<Card> mulligan(GameContext context, Player player, List<Card> cards) {
		return new ArrayList<>();
	}

	@Override
	public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {
//		logger.info("player ID {}, num of valid actions {}.", player.getId(), validActions.size());

		if (validActions.size() == 1) {
			return validActions.get(0);
		}

		int randomIndex = random.nextInt(validActions.size());
		GameAction randomAction = validActions.get(randomIndex);
		return randomAction;
	}

}
