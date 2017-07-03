package net.demilich.metastone.game.behaviour.threat;

import java.util.ArrayList;
import java.util.List;

import net.demilich.metastone.game.Attribute;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.behaviour.heuristic.IGameStateHeuristic;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.entities.heroes.Hero;
import net.demilich.metastone.game.entities.heroes.HeroClass;
import net.demilich.metastone.game.entities.minions.Minion;

public class ThreatBasedHeuristic implements IGameStateHeuristic {

	private static List<String> hardRemoval;

	static {
		hardRemoval = new ArrayList<String>();
		hardRemoval.add("spell_polymorph");
		hardRemoval.add("spell_execute");
		hardRemoval.add("spell_crush");
		hardRemoval.add("spell_assassinate");
		hardRemoval.add("spell_siphon_soul");
		hardRemoval.add("spell_shadow_word_death");
		hardRemoval.add("spell_naturalize");
		hardRemoval.add("spell_hex");
		hardRemoval.add("spell_humility");
		hardRemoval.add("spell_equality");
		hardRemoval.add("spell_deadly_shot");
		hardRemoval.add("spell_sap");
		hardRemoval.add("minion_doomsayer");
		hardRemoval.add("minion_big_game_hunter");
	}

	private static ThreatLevel calcuateThreatLevel(GameContext context, int playerId) {
		int damageOnBoard = 0;
		Player player = context.getPlayer(playerId);
		Player opponent = context.getOpponent(player);
		for (Minion minion : opponent.getMinions()) {
			damageOnBoard += minion.getAttack() * minion.getAttributeValue(Attribute.NUMBER_OF_ATTACKS);
		}
		damageOnBoard += getHeroDamage(opponent.getHero());  //对方随从 + 英雄的攻击力

		int remainingHp = player.getHero().getEffectiveHp() - damageOnBoard;  // 根据减去对方伤害后我方剩余血量来确定威胁等级：红、黄、绿
		if (remainingHp < 1) {
			return ThreatLevel.RED;
		} else if (remainingHp < 15) {
			return ThreatLevel.YELLOW;
		}

		return ThreatLevel.GREEN;
	}

	private static int getHeroDamage(Hero hero) {
		int heroDamage = 0;
		if (hero.getHeroClass() == HeroClass.MAGE) {
			heroDamage += 1;
		} else if (hero.getHeroClass() == HeroClass.HUNTER) {
			heroDamage += 2;
		} else if (hero.getHeroClass() == HeroClass.DRUID) {
			heroDamage += 1;
		} else if (hero.getHeroClass() == HeroClass.ROGUE) {
			heroDamage += 1;
		}
		if (hero.getWeapon() != null) {
			heroDamage += hero.getWeapon().getWeaponDamage();
		}
		return heroDamage;
	}

	private static boolean isHardRemoval(Card card) {
		return hardRemoval.contains(card.getCardId());
	}

	private final FeatureVector weights;

	public ThreatBasedHeuristic(FeatureVector vector) {
		this.weights = vector;
	}

	private double calculateMinionScore(Minion minion, ThreatLevel threatLevel) {
		if (minion.hasAttribute(Attribute.MARKED_FOR_DEATH)) {
			return 0;
		}
		double minionScore = weights.get(WeightedFeature.MINION_INTRINSIC_VALUE);  // 一个随从的基础得分
		minionScore += weights.get(WeightedFeature.MINION_ATTACK_FACTOR)  // 随从攻击力得分
				* (minion.getAttack() - minion.getAttributeValue(Attribute.TEMPORARY_ATTACK_BONUS));
		minionScore += weights.get(WeightedFeature.MINION_HP_FACTOR) * minion.getHp(); // 随从血量得分

		if (minion.hasAttribute(Attribute.TAUNT)) {   // 随从嘲讽技能得分 （根据对方整体威胁等级不同对应不同数值）
			switch (threatLevel) {
			case RED:
				minionScore += weights.get(WeightedFeature.MINION_RED_TAUNT_MODIFIER);
				break;
			case YELLOW:
				minionScore += weights.get(WeightedFeature.MINION_YELLOW_TAUNT_MODIFIER);
				break;
			default:
				minionScore += weights.get(WeightedFeature.MINION_DEFAULT_TAUNT_MODIFIER);
				break;
			}
		}
		// 随从的其他各种技能或效果得分（如风怒、圣盾、法术伤害、秘密行动、不可被攻击）
		if (minion.hasAttribute(Attribute.WINDFURY)) {
			minionScore += weights.get(WeightedFeature.MINION_WINDFURY_MODIFIER);
		} else if (minion.hasAttribute(Attribute.MEGA_WINDFURY)) {
			minionScore += 2 * weights.get(WeightedFeature.MINION_WINDFURY_MODIFIER);
		}

		if (minion.hasAttribute(Attribute.DIVINE_SHIELD)) {
			minionScore += weights.get(WeightedFeature.MINION_DIVINE_SHIELD_MODIFIER);
		}
		if (minion.hasAttribute(Attribute.SPELL_DAMAGE)) {
			minionScore += minion.getAttributeValue(Attribute.SPELL_DAMAGE) * weights.get(WeightedFeature.MINION_SPELL_POWER_MODIFIER);
		}

		if (minion.hasAttribute(Attribute.STEALTH)) {
			minionScore += weights.get(WeightedFeature.MINION_STEALTHED_MODIFIER);
		}
		if (minion.hasAttribute(Attribute.UNTARGETABLE_BY_SPELLS)) {
			minionScore += weights.get(WeightedFeature.MINION_UNTARGETABLE_BY_SPELLS_MODIFIER);
		}

		return minionScore;
	}

	@Override
	public double getScore(GameContext context, int playerId) {
		Player player = context.getPlayer(playerId);
		Player opponent = context.getOpponent(player);
		if (player.getHero().isDestroyed()) {
			return Float.NEGATIVE_INFINITY;
		}
		if (opponent.getHero().isDestroyed()) {
			return Float.POSITIVE_INFINITY;
		}
		double score = 0;

		ThreatLevel threatLevel = calcuateThreatLevel(context, playerId);  //计算对方威胁等级，从score中减去一定数值
		switch (threatLevel) {
		case RED:
			score += weights.get(WeightedFeature.RED_MODIFIER);
			break;
		case YELLOW:
			score += weights.get(WeightedFeature.YELLOW_MODIFIER);
			break;
		default:
			break;
		}
		score += player.getHero().getEffectiveHp() * weights.get(WeightedFeature.OWN_HP_FACTOR);  // 加上和减去自身和对方血量乘以一定系数
		score += opponent.getHero().getEffectiveHp() * weights.get(WeightedFeature.OPPONENT_HP_FACTOR);
		for (Card card : player.getHand()) {
			if (isHardRemoval(card)) {
				score += weights.get(WeightedFeature.HARD_REMOVAL_VALUE);   // 对于HARD_REMOVAL_VALUE性质手牌，加上一定数值
			}
		}

		score += player.getHand().getCount() * weights.get(WeightedFeature.OWN_CARD_COUNT);  // 加上和减去自身和对方手牌数乘以一定系数
		score += opponent.getHand().getCount() * weights.get(WeightedFeature.OPPONENT_CARD_COUNT);

		for (Minion minion : player.getMinions()) {     // 加上己方随从的score
			score += calculateMinionScore(minion, threatLevel);
		}

		for (Minion minion : opponent.getMinions()) {   // 减去对方随从的score
			score -= calculateMinionScore(minion, threatLevel);
		}

		return score;
	}

	@Override
	public void onActionSelected(GameContext context, int playerId) {

	}

}
