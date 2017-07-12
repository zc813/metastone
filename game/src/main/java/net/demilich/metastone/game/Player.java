package net.demilich.metastone.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import net.demilich.metastone.game.behaviour.IBehaviour;
import net.demilich.metastone.game.behaviour.human.HumanBehaviour;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.cards.CardCollection;
import net.demilich.metastone.game.cards.CardType;
import net.demilich.metastone.game.decks.Deck;
import net.demilich.metastone.game.entities.Actor;
import net.demilich.metastone.game.entities.Entity;
import net.demilich.metastone.game.entities.EntityType;
import net.demilich.metastone.game.entities.heroes.Hero;
import net.demilich.metastone.game.entities.minions.Minion;
import net.demilich.metastone.game.entities.minions.Summon;
import net.demilich.metastone.game.statistics.GameStatistics;
import net.demilich.metastone.game.gameconfig.PlayerConfig;

public class Player extends Entity {

	private Hero hero;
	private final String deckName;

	private final CardCollection deck;
	private final CardCollection hand = new CardCollection();
	private final List<Entity> setAsideZone = new ArrayList<>();
	private final List<Entity> graveyard = new ArrayList<>();
	private final List<Summon> summons = new ArrayList<>();
	private final HashSet<String> secrets = new HashSet<>();
	private final HashSet<String> quests = new HashSet<>();

	private final GameStatistics statistics = new GameStatistics();

	private int mana;
	private int maxMana;
	private int lockedMana;

	private boolean hideCards;

	private IBehaviour behaviour;

	private Player(Player otherPlayer) {
		this.setName(otherPlayer.getName());
		this.deckName = otherPlayer.getDeckName();
		this.setHero(otherPlayer.getHero().clone());
		this.deck = otherPlayer.getDeck().clone();
		this.attributes.putAll(otherPlayer.getAttributes());
		this.hand.addAll(otherPlayer.getHand().clone());
		this.summons.addAll(otherPlayer.getSummons().stream().map(Summon::clone).collect(Collectors.toList()));
		this.graveyard.addAll(otherPlayer.getGraveyard().stream().map(Entity::clone).collect(Collectors.toList()));
		this.setAsideZone.addAll(otherPlayer.getSetAsideZone().stream().map(Entity::clone).collect(Collectors.toList()));
		this.secrets.addAll(otherPlayer.secrets);
		this.quests.addAll(otherPlayer.quests);
		this.setId(otherPlayer.getId());
		this.mana = otherPlayer.mana;
		this.maxMana = otherPlayer.maxMana;
		this.lockedMana = otherPlayer.lockedMana;
		this.behaviour = otherPlayer.behaviour;
		this.getStatistics().merge(otherPlayer.getStatistics());
	}

	public Player(PlayerConfig config) {
		config.build();
		Deck selectedDeck = config.getDeckForPlay();
		this.deck = selectedDeck.getCardsCopy();
		this.setHero(config.getHeroForPlay().createHero());
		this.setName(config.getName() + " - " + hero.getName());
		this.deckName = selectedDeck.getName();
		setBehaviour(config.getBehaviour().clone());
		setHideCards(config.hideCards());
	}

	@Override
	public Player clone() {
		return new Player(this);
	}

	public IBehaviour getBehaviour() {
		return behaviour;
	}

	public List<Actor> getCharacters() {
		List<Actor> characters = new ArrayList<Actor>();
		characters.add(getHero());
		characters.addAll(getMinions());
		return characters;
	}

	public CardCollection getDeck() {
		return deck;
	}

	public String getDeckName() {
		return deckName;
	}

	@Override
	public EntityType getEntityType() {
		return EntityType.PLAYER;
	}

	public List<Entity> getGraveyard() {
		return graveyard;
	}

	public CardCollection getHand() {
		return hand;
	}

	public Hero getHero() {
		return hero;
	}

	public int getLockedMana() {
		return lockedMana;
	}

	public int getMana() {
		return mana;
	}

	public int getMaxMana() {
		return maxMana;
	}

	public List<Minion> getMinions() {
		List<Minion> minions = new ArrayList<Minion>();
		for (Summon summon : getSummons()) {
			if (summon instanceof Minion) {
				minions.add((Minion) summon);
			}
		}
		return minions;
	}

	public HashSet<String> getQuests() {
		return quests;
	}

	public List<Summon> getSummons() {
		return summons;
	}

	public HashSet<String> getSecrets() {
		return secrets;
	}
	
	public List<Entity> getSetAsideZone() {
		return setAsideZone;
	}

	public GameStatistics getStatistics() {
		return statistics;
	}

	public List<Integer> getPlayerState(){
		List<Integer> playerState = new ArrayList<Integer>();

		playerState.add(this.getHero().getHp());  // 血量
		playerState.add(this.getMana());  // 当前法力值
		playerState.add(this.getMaxMana());   // 当前最大法力值
		playerState.add(this.getHero().getArmor()); // 护甲

		// 场上的随从相关数据
		int summonCount = 0;   // minions on board that can still attack (直观来说，一回合结束时，自己场上应该不会再有能攻击的随从还没用的情况)
		int summonAttack = 0;
		int summonHp = 0;
		int summonCountNot = 0; // minions on board that can not attack
		int summonAttackNot = 0;
		int summonHpNot = 0;
		for (Summon summon : this.getSummons()) {   // 场上的随从信息, 暂时只考虑攻击力和血量，跑通流程，各种特殊效果后面补充
			if (summon.canAttackThisTurn()) {
				summonCount += 1;
				summonAttack += summon.getAttack();
				summonHp += summon.getHp();
			} else {
				summonCountNot += 1;
				summonAttackNot += summon.getAttack();
				summonHpNot += summon.getHp();
			}
		}
		playerState.addAll(Arrays.asList(summonCount, summonAttack, summonHp, summonCountNot, summonAttackNot, summonHpNot));

		// 手牌相关信息
		int cardMinionCount = 0;
		int cardMinionMana = 0;
		int cardMinionBattleCry = 0;
		int cardSpellCount = 0;
		int cardSpellMana = 0;
		for (Card card : this.getHand()) {
			if (card.getCardType() == CardType.MINION) {
				cardMinionCount += 1;
				cardMinionMana += card.getBaseManaCost();
				if (card.hasBattlecry()) {
					cardMinionBattleCry += 1;
				}
			} else {  // 除了Spell法术牌以外，其实还有 CHOOSE_ONE 等其他手牌类型，但目前暂时不考虑
				cardSpellCount += 1;
				cardSpellMana += card.getBaseManaCost();
			}
		}
		playerState.addAll(Arrays.asList(cardMinionCount, cardMinionMana, cardMinionBattleCry, cardSpellCount, cardSpellMana));
		return playerState;
	}

	public boolean hideCards() {
		return hideCards && !(behaviour instanceof HumanBehaviour);
	}

	public void setBehaviour(IBehaviour behaviour) {
		this.behaviour = behaviour;
	}

	public void setHero(Hero hero) {
		this.hero = hero;
	}

	public void setHideCards(boolean hideCards) {
		this.hideCards = hideCards;
	}

	public void setLockedMana(int lockedMana) {
		this.lockedMana = lockedMana;
	}

	public void setMana(int mana) {
		this.mana = mana;
	}

	public void setMaxMana(int maxMana) {
		this.maxMana = maxMana;
	}

	@Override
	public String toString() {
		return "[PLAYER " + "id: " + getId() + ", name: " + getName() + ", hero: " + getHero() + "]";
	}

}
