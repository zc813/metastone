package net.demilich.metastone.game.behaviour.models;

import com.google.protobuf.InvalidProtocolBufferException;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.behaviour.features.FeatureStrategy;
import net.demilich.metastone.game.behaviour.features.V1FeaturesStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.SignatureDef;

import java.util.List;


public class TFValueModel implements IModel {
    private Session sess;
    private String inputName;
    private String outputName;
    private FeatureStrategy featureStrategy;
    private SavedModelBundle savedModel;

    private static final Logger logger = LoggerFactory.getLogger(TFValueModel.class);

    public TFValueModel(String fileName){
        setFeatureStrategy(new V1FeaturesStrategy());
        load(fileName);
    }

    public void setFeatureStrategy(FeatureStrategy featureStrategy) {
        this.featureStrategy = featureStrategy;
    }

    /**
     * load 或初始化后，对某一个 Context 从特定玩家视角输出模型打分
     *
     * @return (double) 模型打分
     */
    public Object predict(GameContext context, int playerId){
        List<Double> features = featureStrategy.calculate(context, playerId);

        int featureSize = featureStrategy.getWidth();
        float matrix[][] = new float[1][featureSize];
        for (int i=0; i<featureSize; ++i)
            matrix[0][i] = features.get(i).floatValue();

        double score;
        try(Tensor t = Tensor.create(matrix);
            Tensor result = sess.runner().feed(inputName, t).fetch(outputName).run().get(0)) {
            float[][] value = new float[1][1];
            result.copyTo(value);
            score = value[0][0]; // 最后输出值只有一个数也要这么写
        }

        return score;
    }

    /**
     * 加载通过 tensorflow 的 SavedModelBundle 保存好的模型和 weights
     * 它会以文件夹的形式储存序列化后的模型的计算图（saved_model.pb）和 weights（variables.*）
     *
     * 保存代码在 python 中。需要注意如果每次保存后不清除 Saver 的话，每次计算图会越来越大，读取会越来越慢
     *
     * @param fileName 文件夹地址
     */
    public void load(String fileName) {
        if (savedModel != null)
            close();

        try {
            savedModel = SavedModelBundle.load(fileName, "model");

            Session newSess = savedModel.session();
            SignatureDef signature = MetaGraphDef.parseFrom(savedModel.metaGraphDef())
                    .getSignatureDefOrThrow("signature"); // 如果图无效会 throw InvalidProtocolBufferException
            inputName = signature.getInputsOrThrow("input").getName();
            outputName = signature.getOutputsOrThrow("output").getName();

            sess = newSess; // 读取都成功再更新 session，否则仍然用之前的

        } catch (Exception e) { // 可能会有 python 写到一半这边就读取的情况
            e.printStackTrace();
            logger.error("Due to the error above, model was not updated to {}!", fileName);
        }
    }

    public void close(){
        savedModel.close();
    }
}
