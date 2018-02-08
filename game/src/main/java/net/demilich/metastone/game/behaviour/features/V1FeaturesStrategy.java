package net.demilich.metastone.game.behaviour.features;

import net.demilich.metastone.game.Attribute;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.cards.CardType;
import net.demilich.metastone.game.entities.heroes.HeroClass;
import net.demilich.metastone.game.entities.minions.Minion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class V1FeaturesStrategy implements FeatureStrategy {
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

    @Override
    public int getWidth() {
        return 86;
    }

    @Override
    public List<Double> calculate(GameContext context, int player){
        List<Double> activeState = calculatePlayerState(context.getPlayer(player));
        List<Double> opponentState = calculatePlayerState(context.getOpponent(context.getPlayer(player)));
        activeState.addAll(opponentState);
        return activeState;
    }

    private List<Double> calculatePlayerState(Player player){
        List<Double> playerState = new ArrayList<>();

        // 英雄相关信息
        playerState.add(player.getHero().getHp() / 1.0);  // 0. 血量
        playerState.add(player.getHero().getArmor() / 1.0); // 1. 护甲
        playerState.add(player.getMana() / 1.0);  // 2. 当前法力值
        int weaponDamage = 0;
        int weaponDurability = 0;
        if (player.getHero().getWeapon() != null) {
            weaponDamage = player.getHero().getWeapon().getWeaponDamage();  //3. 武器伤害
            weaponDurability = player.getHero().getWeapon().getDurability(); //4. 武器耐久
        }
        playerState.add(weaponDamage / 1.0);
        playerState.add(weaponDurability / 1.0);
        // 英雄技能, 暂时按照英雄类型将技能分为1、2、3三档，暂时不考虑一些非基础的英雄技能的影响
        int heroPower = 3; // 默认为3
        HeroClass heroPowerClass = player.getHero().getHeroPower().getHeroClass();
        if(heroPowerClass == HeroClass.HUNTER || heroPowerClass == HeroClass.MAGE || heroPowerClass == HeroClass.WARLOCK){
            heroPower = 3;
        }else if(heroPowerClass == HeroClass.DRUID || heroPowerClass == HeroClass.PALADIN || heroPowerClass == HeroClass.SHAMAN){
            heroPower = 2;
        }else if(heroPowerClass == HeroClass.PRIEST || heroPowerClass == HeroClass.ROGUE || heroPowerClass == HeroClass.WARRIOR){
            heroPower = 1;
        }
        playerState.add(heroPower / 1.0); // 5. 英雄技能

        // 场上的随从相关数据
        int minionCount = 0;   // 6. 随从可攻击数目 (直观来说，一回合结束时，自己场上应该不会再有能攻击的随从还没用的情况)
        int minionAttack = 0;  // 7. 随从攻击力
        int minionHp = 0;  // 8. 随从血量
        int minionCountNot = 0; // 9. 随从不可攻击数目
        int minionAttackNot = 0; // 10. 不可攻击随从攻击力
        int minionHpNot = 0; // 11. 不可攻击随从血量
        int minionCountTaunt = 0;  // 12. 带嘲讽的随从数目
        int minionAttackTaunt= 0;  // 13.
        int minionHpTaunt = 0;  // 14
        int minionCountFrozen = 0;  // 15 冻结的随从
        int minionAttackFrozen= 0;  // 16
        int minionHpFrozen = 0; // 17
        int minionCountStealth = 0;  // 18 潜行的随从
        int minionAttackStealth= 0;  // 19
        int minionHpStealth = 0;  // 20
        int minionCountShield = 0;  // 21 带圣盾的随从
        int minionAttackShield= 0;  // 22
        int minionHpShield = 0;  // 23
        int minionCountEnrage = 0;  // 24 带激怒的随从
        int minionAttackEnrage= 0;  // 25
        int minionHpEnrage = 0;  // 26
        int minionCountUntarget = 0;  // 27 不可被法术攻击的随从
        int minionAttackUntarget= 0;  // 28
        int minionHpUntarget = 0;  // 29
        int minionCountWindfury = 0; // 30 带风怒效果的随从
        int minionAttackWindfury = 0; // 31
        int minionHpWindfury = 0;  // 32
        int minionCountSpell = 0;  // 33 带法术伤害的随从
        int minionSpellDamage = 0;  // 34

        for (Minion minion : player.getMinions()) {  // 场上的随从信息
            if (minion.canAttackThisTurn()) {
                minionCount += 1;
                minionAttack += minion.getAttack();
                minionHp += minion.getHp();
            } else {
                minionCountNot += 1;
                minionAttackNot += minion.getAttack();
                minionHpNot += minion.getHp();
            }
            if(minion.hasAttribute(Attribute.TAUNT)){
                minionCountTaunt += 1;
                minionAttackTaunt += minion.getAttack();
                minionHpTaunt += minion.getHp();
            }
            if (minion.hasAttribute(Attribute.FROZEN)) {  // 冻结的随从
                minionCountFrozen += 1;
                minionAttackFrozen += minion.getAttack();
                minionHpFrozen += minion.getHp();
            }
            if (minion.hasAttribute(Attribute.STEALTH)) {  // 潜行
                minionCountStealth += 1;
                minionAttackStealth += minion.getAttack();
                minionHpStealth += minion.getHp();
            }
            if (minion.hasAttribute(Attribute.DIVINE_SHIELD)) {  //圣盾
                minionCountShield += 1;
                minionAttackShield += minion.getAttack();
                minionHpShield += minion.getHp();
            }
            if (minion.hasAttribute(Attribute.ENRAGED)) {  // 激怒
                minionCountEnrage += 1;
                minionAttackEnrage += minion.getAttack();
                minionHpEnrage += minion.getHp();
            }
            if (minion.hasAttribute(Attribute.UNTARGETABLE_BY_SPELLS)) {  // 不能被法术指定
                minionCountUntarget += 1;
                minionAttackUntarget += minion.getAttack();
                minionHpUntarget += minion.getHp();
            }
            if (minion.hasAttribute(Attribute.WINDFURY) || minion.hasAttribute(Attribute.MEGA_WINDFURY)) {  // 风怒或超级风怒
                minionCountWindfury += 1;
                minionHpWindfury += minion.getHp();
                if (minion.hasAttribute(Attribute.MEGA_WINDFURY)){  // 风怒或超级风怒带来的额外的攻击力
                    minionAttackWindfury += 3*minion.getAttack();
                }else{
                    minionAttackWindfury += minion.getAttack();
                }
            }
            if (minion.hasAttribute(Attribute.SPELL_DAMAGE)) {  // 法术伤害
                minionCountSpell += 1;
                minionSpellDamage += minion.getAttributeValue(Attribute.SPELL_DAMAGE);
            }
        }
        playerState.addAll(Arrays.asList(minionCount / 1.0, minionAttack / 1.0, minionHp / 1.0, minionCountNot / 1.0, minionAttackNot / 1.0, minionHpNot / 1.0,
                minionCountTaunt / 1.0, minionAttackTaunt / 1.0, minionHpTaunt / 1.0,
                minionCountFrozen / 1.0, minionAttackFrozen / 1.0, minionHpFrozen / 1.0,
                minionCountStealth / 1.0, minionAttackStealth / 1.0, minionHpStealth / 1.0,
                minionCountShield / 1.0, minionAttackShield / 1.0, minionHpShield / 1.0,
                minionCountEnrage / 1.0, minionAttackEnrage / 1.0, minionHpEnrage / 1.0,
                minionCountUntarget / 1.0, minionAttackUntarget / 1.0, minionHpUntarget / 1.0,
                minionCountWindfury / 1.0, minionAttackWindfury / 1.0, minionHpWindfury / 1.0,
                minionCountSpell / 1.0, minionSpellDamage / 1.0));

        // 手牌相关信息
        int cardMinionCount = 0; // 35
        int cardMinionMana = 0;  // 36
        int cardMinionBattleCry = 0;  // 37
        int cardWeaponCount = 0;  // 38
        int cardWeaponMana = 0;  // 39
        int cardSpellCount = 0;  // 40
        int cardSpellMana = 0;  // 41
        int cardHardRemoval = 0;  // 42
        for (Card card : player.getHand()) {
            if (card.getCardType() == CardType.MINION) {  // 随从牌
                cardMinionCount += 1;
                cardMinionMana += card.getBaseManaCost();
                if (card.hasBattlecry()) {
                    cardMinionBattleCry += 1;  // 这个似乎一直是0，可能没用
                }
            } else if(card.getCardType() == CardType.WEAPON){  // 武器牌
                cardWeaponCount += 1;
                cardWeaponMana += card.getBaseManaCost();
            } else{  // 剩下的应该就是Spell法术牌了，但貌似也有另外几个其他的, 不区分
                cardSpellCount += 1;
                cardSpellMana += card.getBaseManaCost();
            }

            if (isHardRemoval(card)) {
                cardHardRemoval += 1;
            }
        }
        playerState.addAll(Arrays.asList(cardMinionCount / 1.0, cardMinionMana / 1.0, cardMinionBattleCry / 1.0, cardWeaponCount / 1.0, cardWeaponMana / 1.0, cardSpellCount / 1.0, cardSpellMana / 1.0, cardHardRemoval / 1.0));
        return playerState;
    }

    private static boolean isHardRemoval(Card card) {
        return hardRemoval.contains(card.getCardId());
    }
}
