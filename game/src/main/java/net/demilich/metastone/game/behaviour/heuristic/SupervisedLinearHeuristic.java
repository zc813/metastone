package net.demilich.metastone.game.behaviour.heuristic;

/**
 * Created by sjxn2423 on 2017/6/29.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.demilich.metastone.game.Attribute;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.cards.CardType;
import net.demilich.metastone.game.entities.minions.Minion;
import net.demilich.metastone.game.entities.minions.Summon;

public class SupervisedLinearHeuristic implements IGameStateHeuristic{

//    ################# Hunter #############################
//    正负1 binary 标签， 训练LogisticRegression模型得到权重
//    1. randomPlay_1000games数据，对称提取特征，胜率 49.5%
//    double[] coef = {0.099,-0.015,-0.104,0.093,0.127,0.049,0.052,0.063,0.072,0.028,0.026,0.005,0.000,-0.079,0.004,
//                    -0.099,0.015,0.104,-0.093,-0.127,-0.049,-0.052,-0.063,-0.072,-0.028,-0.026,-0.005,0.000,0.079,-0.004};

//    2. GameStateValue_300games数据，对称提取特征，胜率 41.1%
//    double[] coef = {0.135,0.028,-0.442,0.336,-0.529,0.431,-0.120,0.048,0.161,0.015,0.212,-0.013,0.000,-0.216,0.055,
//                    -0.135,-0.028,0.442,-0.336,0.529,-0.431,0.120,-0.048,-0.161,-0.015,-0.212,0.013,0.000,0.215,-0.055};

//    3. randomPlay_1000games数据，不对称提取特征，胜率 51.6%
//    double[] coef = {0.099,-0.004,-0.104,0.072,0.213,0.038,0.038,0.132,0.061,0.019,0.064,-0.010,0.000,-0.118,0.014,
//                    -0.098,0.025,0.110,-0.033,-0.038,-0.063,-0.063,0.009,-0.085,-0.037,0.017,-0.022,0.000,0.049,0.005};

//    4. GameStateValue_300games数据，不对称提取特征，胜率 43.5%
//    double[] coef ={0.133,-0.008,-0.429,0.344,-0.960,0.385,0.085,0.145,0.120,0.012,0.165,-0.022,0.000,-0.199,0.024,
//                    -0.138,-0.071,0.457,-0.075,0.186,-0.524,0.362,0.062,-0.203,-0.020,-0.228,-0.002,0.000,0.245,-0.083};

//    5. GreedyBestMove_1000games数据，不对称提取特征，胜率 54.2%
//    double[] coef = {0.116,0.142,-0.574,-0.088,-0.106,0.188,-0.053,-0.004,0.142,0.049,-0.181,-0.009,0.000,-0.581,0.059,
//                    -0.134,-0.157,0.467,0.640,-0.280,0.002,-0.034,-0.059,-0.136,-0.009,0.020,0.044,0.000,0.460,-0.056};

//    discounted real valued标签， 训练线性回归模型得到权重
//     1. randomPlay_1000games数据，对称提取特征，胜率 45.9%
//    double[] coef = {0.251,-0.012,-0.296,0.224,0.188,0.111,0.097,0.047,0.166,0.046,0.096,0.001,0.000,-0.066,0.012,
//                     -0.251,0.012,0.296,-0.224,-0.188,-0.111,-0.097,-0.047,-0.166,-0.046,-0.096,-0.001,-0.000,0.066,-0.012};

//    2. GameStateValue_300games数据，对称提取特征，胜率 %
//    double[] coef = {0.349,0.095,-1.232,0.261,-0.463,0.587,-0.223,0.119,0.390,0.014,0.410,-0.025,-0.000,-0.578,0.127,
//                      -0.349,-0.095,1.232,-0.261,0.463,-0.587,0.223,-0.119,-0.390,-0.014,-0.410,0.025,0.000,0.578,-0.127};

//    3. randomPlay_1000games数据，不对称提取特征，胜率 43.8%
//    double[] coef = {0.243,0.001,-0.304,0.371,0.328,0.085,0.082,0.137,0.152,0.023,0.119,-0.014,0.000,-0.116,0.019,
//                    -0.259,0.023,0.292,0.076,-0.018,-0.142,-0.110,0.052,-0.181,-0.071,-0.064,-0.016,0.000,0.019,-0.006};

//    4. GameStateValue_300games数据，不对称提取特征，胜率 50.9%
//    double[] coef = {0.339,0.078,-1.220,0.330,-1.133,0.571,0.005,0.233,0.305,0.034,0.262,-0.037,0.000,-0.562,0.074,
//                    -0.355,-0.122,1.219,0.709,-0.310,-0.595,0.515,0.023,-0.470,-0.004,-0.505,0.007,0.000,0.615,-0.173};

//    discounted real valued标签，尝试不同的gamma （之前偶然设置成0.8后效果极差，胜率0.5%，不确定是不是偶然，对于gamma这么敏感？）
//     GreedyBestMove_1000games数据，discounted real valued标签，不对称提取特征
//    1. gamma=0.8，胜率 0.4%
//    double[] coef = {0.272,0.422,-1.220,-0.107,0.293,0.081,0.025,-0.259,0.222,0.099,-0.088,-0.005,-0.000,-0.482,0.077,
//                    -0.305,-0.484,1.080,0.346,-0.447,-0.025,-0.093,-0.032,-0.194,-0.022,-0.189,0.050,0.000,0.313,-0.055};

//    2. gamma=0.85，胜率 4.8%
//    double[] coef = {0.286,0.410,-1.338,-0.105,0.190,0.143,-0.001,-0.228,0.253,0.104,-0.178,-0.006,-0.000,-0.666,0.090,
//                    -0.322,-0.472,1.171,0.554,-0.462,-0.024,-0.095,-0.078,-0.221,-0.019,-0.148,0.062,0.000,0.465,-0.069};

//    3. gamma=0.9，胜率 26.6%
//    double[] coef = {0.296,0.400,-1.438,-0.222,0.050,0.227,-0.040,-0.175,0.288,0.109,-0.306,-0.009,-0.000,-0.943,0.109,
//                    -0.336,-0.456,1.233,0.922,-0.507,-0.017,-0.091,-0.137,-0.252,-0.014,-0.085,0.079,0.000,0.693,-0.090};

//    4. gamma=0.95，胜率 51.5%
//    double[] coef = {0.298,0.391,-1.491,-0.508,-0.140,0.346,-0.096,-0.085,0.325,0.114,-0.487,-0.015,-0.000,-1.369,0.137,
//                    -0.344,-0.433,1.225,1.541,-0.604,0.002,-0.077,-0.216,-0.286,-0.006,0.016,0.105,0.000,1.043,-0.123};

//    5. gamma=0.99，胜率 55.1%
//    double[] coef = {0.290,0.386,-1.463,-0.898,-0.341,0.474,-0.162,0.026,0.356,0.115,-0.690,-0.022,-0.000,-1.878,0.170,
//                    -0.341,-0.407,1.123,2.309,-0.742,0.031,-0.056,-0.303,-0.313,0.005,0.136,0.133,0.000,1.456,-0.165};

//    # 尝试增加训练数据
//    1. randomPlay_4000games数据，不对称提取特征，正负1标签，胜率 48.9% (效果相比 randomPlay_1000games数据反而下降了)
//    double[] coef = {0.098,-0.027,-0.089,0.077,0.058,0.068,0.057,0.041,0.078,0.040,0.085,0.006,0.000,-0.081,0.016,
//                    -0.103,0.028,0.078,-0.018,-0.046,-0.076,-0.052,-0.038,-0.082,-0.033,-0.103,-0.003,0.000,0.095,-0.008};

//    2. GreedyBestMove_8000games数据，不对称提取特征，正负1标签，胜率 58% (这个效果有所提升)
//    double[] coef = {0.139,0.130,-0.566,-0.052,0.045,0.114,-0.024,0.034,0.133,0.032,0.002,-0.028,0.000,-0.489,0.066,
//                    -0.134,-0.124,0.580,0.727,-0.135,-0.115,0.047,-0.035,-0.133,-0.035,0.015,0.024,0.000,0.516,-0.066};

//    3. GreedyBestMove_8000games数据，不对称提取特征，discounted标签，gamma=0.99，胜率 51%
//    double[] coef = {0.353,0.354,-1.485,-0.155,-0.003,0.265,-0.021,0.187,0.338,0.053,-0.069,-0.082,0.000,-1.567,0.201,
//                    -0.340,-0.338,1.526,1.742,-0.223,-0.272,0.093,-0.194,-0.332,-0.063,0.123,0.070,0.000,1.667,-0.203};

//    ##################### Warrior #############################
//    1. GreedyBestMove_1000games数据，不对称提取特征，正负1标签，胜率 51.7%
//    double[] coef = {0.098,-0.027,-0.089,0.077,0.058,0.068,0.057,0.041,0.078,0.040,0.085,0.006,0.000,-0.081,0.016,
//                    -0.103,0.028,0.078,-0.018,-0.046,-0.076,-0.052,-0.038,-0.082,-0.033,-0.103,-0.003,0.000,0.095,-0.008};

//    #####################  多英雄数据混合 (似乎大部分英雄变的更好了，如hunter，有些英雄相对变差了) #############################
//    1. GreedyBestMove, warrior, hunter 各1000局数据，不对称提取特征，正负1标签，胜率 hunter vs hunter  63.3%    warrior vs warrior  47.6%
//    double[] coef = {0.107,0.030,-0.368,-0.028,-0.056,0.099,0.008,0.027,0.109,0.031,-0.071,-0.006,0.000,-0.428,0.032,
//                    -0.116,-0.042,0.321,0.004,-0.099,0.009,-0.073,-0.033,-0.134,0.001,-0.174,0.036,0.000,0.256,-0.050};

//    2. GreedyBestMove, warrior + hunter + rogue + druid 各1000局数据，不对称提取特征，正负1标签，胜率：
//      hunter vs hunter  61.9%
//      warrior vs warrior  41.5%
//      Druid vs Druid      39.3%
//      rogue vs rogue      40.4%
    double[] coef = {0.109,0.052,-0.224,-0.055,-0.145,0.091,0.023,0.008,0.113,0.020,-0.017,-0.011,0.000,-0.384,0.036,
                    -0.122,-0.065,0.177,0.039,0.010,-0.052,-0.012,-0.020,-0.119,-0.006,-0.068,0.024,0.000,0.336,-0.061};

//    3. 单独使用Druid数据训练，然后测试Druid，胜率 24.4% （出乎意料的低）
//    double[] coef = {0.098,0.086,-0.009,-0.111,-0.279,0.092,0.045,-0.050,0.107,0.024,0.006,-0.007,0.000,-0.336,0.050,
//                    -0.105,-0.080,-0.036,0.087,0.096,-0.111,0.079,0.061,-0.105,-0.019,0.078,0.003,0.000,0.374,-0.070};

//    4. 单独使用Rogue数据训练，然后测试Rogue，胜率 27.4%
//    double[] coef = {0.139,0.090,-0.218,-0.154,-0.318,0.070,0.132,0.043,0.139,-0.018,0.053,-0.026,0.000,-0.406,0.047,
//                    -0.162,-0.126,0.124,-0.904,0.364,-0.174,-0.005,-0.120,-0.086,-0.004,-0.031,0.032,0.000,0.464,-0.067};

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

        for (int i = 0; i < coef.length; i++){
            score += coef[i]*envState.get(i);
        }
        return score;
    }

    @Override
    public void onActionSelected(GameContext context, int playerId) {
    }


}
