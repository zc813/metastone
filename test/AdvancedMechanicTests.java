import java.util.List;

import net.pferdimanzug.hearthstone.analyzer.game.GameContext;
import net.pferdimanzug.hearthstone.analyzer.game.GameTag;
import net.pferdimanzug.hearthstone.analyzer.game.Player;
import net.pferdimanzug.hearthstone.analyzer.game.actions.GameAction;
import net.pferdimanzug.hearthstone.analyzer.game.actions.PhysicalAttackAction;
import net.pferdimanzug.hearthstone.analyzer.game.behaviour.IBehaviour;
import net.pferdimanzug.hearthstone.analyzer.game.cards.MinionCard;
import net.pferdimanzug.hearthstone.analyzer.game.cards.concrete.neutral.AbusiveSergeant;
import net.pferdimanzug.hearthstone.analyzer.game.cards.concrete.neutral.AmaniBerserker;
import net.pferdimanzug.hearthstone.analyzer.game.cards.concrete.neutral.StonetuskBoar;
import net.pferdimanzug.hearthstone.analyzer.game.entities.Entity;
import net.pferdimanzug.hearthstone.analyzer.game.entities.heroes.Anduin;
import net.pferdimanzug.hearthstone.analyzer.game.entities.heroes.Garrosh;
import net.pferdimanzug.hearthstone.analyzer.game.entities.heroes.Jaina;

import org.testng.Assert;
import org.testng.annotations.Test;


public class AdvancedMechanicTests extends BasicTests {
	
	@Test
	public void testEnrage() {
		GameContext context = createContext(new Jaina(), new Anduin());
		Player mage = context.getPlayer1();
		mage.setMana(10);
		Player priest = context.getPlayer2();
		priest.setMana(10);

		MinionCard amaniBerserkerCard = new AmaniBerserker();
		priest.getHand().add(amaniBerserkerCard);
		context.getLogic().performGameAction(priest.getId(), amaniBerserkerCard.play());
		
		MinionCard stonetuskBoarCard = new StonetuskBoar();
		mage.getHand().add(stonetuskBoarCard);
		context.getLogic().performGameAction(mage.getId(), stonetuskBoarCard.play());
		
		Entity attacker = getSingleMinion(mage.getMinions());
		Entity defender = getSingleMinion(priest.getMinions());
		
		Assert.assertEquals(defender.getAttack(), AmaniBerserker.BASE_ATTACK);
		Assert.assertEquals(defender.hasTag(GameTag.ENRAGED), false);
		
		// attack once, should apply the enrage attack bonus
		GameAction attackAction = new PhysicalAttackAction(attacker.getReference());
		attackAction.setTarget(defender);
		context.getLogic().performGameAction(mage.getId(), attackAction);
		Assert.assertEquals(defender.getAttack(), AmaniBerserker.BASE_ATTACK + AmaniBerserker.ENRAGE_ATTACK_BONUS);
		Assert.assertEquals(defender.hasTag(GameTag.ENRAGED), true);
		// attack second time, enrage bonus should not increase
		context.getLogic().performGameAction(mage.getId(), attackAction);
		Assert.assertEquals(defender.getAttack(), AmaniBerserker.BASE_ATTACK + AmaniBerserker.ENRAGE_ATTACK_BONUS);
		
		// heal - enrage attack bonus should be gone
		GameAction healAction = priest.getHero().getHeroPower().play();
		healAction.setTarget(defender);
		context.getLogic().performGameAction(priest.getId(), healAction);
		Assert.assertEquals(defender.getAttack(), AmaniBerserker.BASE_ATTACK);
		Assert.assertEquals(defender.hasTag(GameTag.ENRAGED), false);
	}
	
	@Test
	public void testShorttermBuffs() {
		GameContext context = createContext(new Jaina(), new Garrosh());
		Player mage = context.getPlayer1();
		mage.setMana(10);
		Player warrior = context.getPlayer2();
		warrior.setMana(10);
		
		int baseAttack = 1;
		MinionCard devMonster = new DevMonster(baseAttack, 1);
		mage.getHand().add(devMonster);
		MinionCard abusiveSergeant = new AbusiveSergeant();
		mage.getHand().add(abusiveSergeant);
		
		context.getLogic().performGameAction(mage.getId(), devMonster.play());
		mage.setBehaviour(new IBehaviour() {
			
			@Override
			public Entity provideTargetFor(Player player, GameAction action, List<Entity> validTargets) {
				return validTargets.get(0);
			}

			@Override
			public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {
				return null;
			}
		});
		Entity testSubject = getSingleMinion(mage.getMinions());
		Assert.assertEquals(testSubject.getAttack(), baseAttack);
		context.getLogic().performGameAction(mage.getId(), abusiveSergeant.play());
		Assert.assertEquals(testSubject.getAttack(), baseAttack + AbusiveSergeant.ATTACK_BONUS);
		context.getLogic().endTurn(mage.getId());
		Assert.assertEquals(testSubject.getAttack(), baseAttack);
	}
	
	@Test
	public void testDivineShield() {
		GameContext context = createContext(new Jaina(), new Garrosh());
		Player mage = context.getPlayer1();
		mage.setMana(10);
		Player warrior = context.getPlayer2();
		warrior.setMana(10);

		MinionCard minionCard1 = new DevMonster(2, 2, GameTag.DIVINE_SHIELD);
		mage.getHand().add(minionCard1);
		context.getLogic().performGameAction(mage.getId(), minionCard1.play());
		
		MinionCard minionCard2 = new DevMonster(5, 5);
		mage.getHand().add(minionCard2);
		context.getLogic().performGameAction(warrior.getId(), minionCard2.play());
		
		Entity attacker = getSingleMinion(mage.getMinions());
		Entity defender = getSingleMinion(warrior.getMinions());
		
		GameAction attackAction = new PhysicalAttackAction(attacker.getReference());
		attackAction.setTarget(defender);
		
		context.getLogic().performGameAction(mage.getId(), attackAction);
		Assert.assertEquals(attacker.getHp(), attacker.getMaxHp());
		Assert.assertEquals(defender.getHp(), defender.getMaxHp() - attacker.getAttack());
		Assert.assertEquals(attacker.isDead(), false);
		
		context.getLogic().performGameAction(mage.getId(), attackAction);
		Assert.assertEquals(attacker.getHp(), attacker.getMaxHp() - defender.getAttack());
		Assert.assertEquals(defender.getHp(), defender.getMaxHp() - attacker.getAttack() * 2);
		Assert.assertEquals(attacker.isDead(), true);
	}

}
