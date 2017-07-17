package net.demilich.metastone.game.behaviour.heuristic;

/**
 * Created by sjxn2423 on 2017/6/29.
 */

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.cards.CardType;
import net.demilich.metastone.game.entities.minions.Summon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SupervisedThreeStageLinearHeuristic implements IGameStateHeuristic{
//     分3个阶段训练3个局面评估函数
//    ################# Hunter #############################
//    正负1 binary 标签， 训练LogisticRegression模型得到权重
//    randomPlay_1000games数据，不对称提取特征
//    对 GreedyBestMove 胜率 54.8%  对 GreedyBestMoveLinear （1阶段） 胜率 50.3%
    double[] coef1 = {-0.018,-0.043,-0.012,0.221,0.343,-0.328,0.285,0.240,-0.005,0.044,0.112,-0.032,0.000,-0.188,
                    0.015,-0.001,0.039,-0.111,0.000,0.474,-0.229,-0.117,-0.021,-0.044,-0.007,0.198,-0.015,0.000,0.139,0.042};
    double[] coef2 = {0.003,-0.002,-0.106,0.354,0.065,-0.032,0.157,0.033,0.079,0.083,0.017,-0.015,0.000,-0.286,
                    0.028,-0.030,-0.010,0.077,0.000,-0.190,-0.066,-0.004,0.060,-0.114,-0.042,0.113,-0.016,0.000,0.168,0.023};
    double[] coef3 = {0.111,-0.007,-0.269,0.026,0.245,0.046,0.027,0.120,0.070,0.013,0.043,0.002,0.000,0.045,-0.007,
                    -0.110,0.029,0.273,-0.055,0.005,-0.063,-0.076,0.016,-0.084,-0.043,-0.004,-0.027,0.000,-0.025,0.001};

    int turn1 = 5;
    int turn2 = 10;

    // 可以考虑把这个method放到Player类中，公用，这样只用维护一份
    public List<Integer> getPlayerState(Player player){
        List<Integer> playerState = new ArrayList<Integer>();

        playerState.add(player.getHero().getHp());  // 血量
        playerState.add(player.getMana());  // 当前法力值
        playerState.add(player.getMaxMana());   // 当前最大法力值
        playerState.add(player.getHero().getArmor()); // 护甲

        // 场上的随从相关数据
        int summonCount = 0;   // minions on board that can still attack (直观来说，一回合结束时，自己场上应该不会再有能攻击的随从还没用的情况)
        int summonAttack = 0;
        int summonHp = 0;
        int summonCountNot = 0; // minions on board that can not attack
        int summonAttackNot = 0;
        int summonHpNot = 0;
        for (Summon summon : player.getSummons()) {   // 场上的随从信息, 暂时只考虑攻击力和血量，跑通流程，各种特殊效果后面补充
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
        for (Card card : player.getHand()) {
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

    @Override
    public double getScore(GameContext context, int playerId) {
        float score = 0;
        Player player = context.getPlayer(playerId);
        Player opponent = context.getOpponent(player);
        if (player.getHero().isDestroyed()) {   // 己方被干掉，得分 负无穷
            return Float.NEGATIVE_INFINITY;
        }
        if (opponent.getHero().isDestroyed()) {  // 对方被干掉，得分 正无穷
            return Float.POSITIVE_INFINITY;
        }

        List<Integer> envState = getPlayerState(player);
        envState.addAll(getPlayerState(opponent));

        // 3阶段不同的评估
        if(context.getTurn() <= turn1){
            for (int i = 0; i < coef1.length; i++){
                score += coef1[i]*envState.get(i);
            }
        }else if(context.getTurn() > turn2){
            for (int i = 0; i < coef3.length; i++){
                score += coef3[i]*envState.get(i);
            }
        }else{
            for (int i = 0; i < coef2.length; i++){
                score += coef2[i]*envState.get(i);
            }
        }

        return score;
    }

    @Override
    public void onActionSelected(GameContext context, int playerId) {
    }


}
