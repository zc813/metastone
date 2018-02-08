package net.demilich.metastone.game.behaviour.models;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.behaviour.features.FeatureStrategy;
import net.demilich.metastone.game.behaviour.features.V1FeaturesStrategy;

import java.util.ArrayList;
import java.util.List;

public class GamePlayData {
    private List<List> data = new ArrayList<>();
    private FeatureStrategy featureStrategy;

    public GamePlayData(){
        setFeatureStrategy(new V1FeaturesStrategy()); // 默认特征提取策略
    }

    public GamePlayData(FeatureStrategy featureStrategy){
        setFeatureStrategy(featureStrategy);
    }

    public void dump(String fileName){
        throw new UnsupportedOperationException("This class does not support dumping.");
    }

    public final List<List> getData() {
        return data;
    }

    public final void add(GameContext context, int playerId){
        data.add(featureStrategy.calculate(context, playerId)); // 添加一个局面
    }

    public final void merge(GamePlayData toBeMerged){
        data.addAll(toBeMerged.getData());
    }

    public final void clear(){ data.clear(); }

    public final void setFeatureStrategy(FeatureStrategy featureStrategy) {
        this.featureStrategy = featureStrategy;
    }
}
