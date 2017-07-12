package net.demilich.metastone.game.behaviour.heuristic;

/**
 * Created by sjxn2423 on 2017/7/11.
 */

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.cards.CardType;
import net.demilich.metastone.game.entities.minions.Summon;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.*;
import org.jpmml.model.PMMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

public class SupervisedModelHeuristic implements IGameStateHeuristic{

    private final static Logger logger = LoggerFactory.getLogger(SupervisedModelHeuristic.class);
    private Random random = new Random();
//    private final static String pmmlFile = "E:\\MetaStone\\game\\src\\main\\java\\net\\demilich\\metastone\\game\\behaviour\\heuristic\\random1000_binary_discounted_mlp.pmml";
    private final static String pmmlFile = "E:\\MetaStone\\app\\mlp.pmml";
    private static Evaluator modelEvaluator;

    public SupervisedModelHeuristic() {
        File file = new File(pmmlFile);
        try {
            if (file.exists()){
                //如果PMML文件存在，直接加载已有模型
                loadPMMLModel(pmmlFile);
            }else{
                this.modelEvaluator = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadPMMLModel(String file) throws Exception {
        try(InputStream model = new FileInputStream(new File(file))){
            PMML pmml = PMMLUtil.unmarshal(model);
            ModelEvaluatorFactory modelEvaluatorFactory = ModelEvaluatorFactory.newInstance();
            this.modelEvaluator = modelEvaluatorFactory.newModelEvaluator(pmml);
        } catch(Exception e) {
            logger.error(e.toString());
            throw e;
        }
    }

    private double evaluateContext(GameContext context, Player player) {
        if (this.modelEvaluator==null){
            logger.info("PMML evaluator is null");
            return random.nextDouble();
        }
        Player opponent = context.getOpponent(player);
        List<Integer> envState = player.getPlayerState();
        envState.addAll(opponent.getPlayerState());
//        logger.info("envState2:{}", envState);
        // 准备模型输入数据
        Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();
        List<InputField> inputFields = this.modelEvaluator.getInputFields();
        int i = 0;
        for(InputField inputField : inputFields){
            FieldName inputFieldName = inputField.getName();
            Object rawValue = envState.get(i);
//            Object rawValue = null;
//            if (envState.get(i) >= 0)
//                rawValue = Math.log(envState.get(i) + 1.0);  // 尝试对输入进行log变换, 效果变差
//            else
//                rawValue = 0;
            FieldValue inputFieldValue = inputField.prepare(rawValue);
            arguments.put(inputFieldName, inputFieldValue);
            i++;
        }
        //	logger.info("PMML inputs {}", arguments);
        Map<FieldName, ?> results = this.modelEvaluator.evaluate(arguments);
        List<TargetField> targetFields = this.modelEvaluator.getTargetFields();
        //	logger.info("PMML results {}", results);
        //	logger.info("PMML targetFields {}", targetFields);
        Object targetFieldValue = results.get(targetFields.get(0).getName());
//        logger.info("PMML targetFieldValue {}", targetFieldValue);
        return (double)targetFieldValue;
    }

    @Override
    public double getScore(GameContext context, int playerId) {
        Player player = context.getPlayer(playerId);
        Player opponent = context.getOpponent(player);
        if (player.getHero().isDestroyed()) {   // 己方被干掉，得分 负无穷
            return Float.NEGATIVE_INFINITY;
        }
        if (opponent.getHero().isDestroyed()) {  // 对方被干掉，得分 正无穷
            return Float.POSITIVE_INFINITY;
        }

        double score = evaluateContext(context, player);
        return score;
    }

    @Override
    public void onActionSelected(GameContext context, int playerId) {
    }


}
