package net.demilich.metastone.game.behaviour.models;

import net.demilich.metastone.game.behaviour.features.FeatureStrategy;
import net.demilich.metastone.game.behaviour.mcts.MonteCarloTreeSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

public class SerialGamePlayData extends GamePlayData{
    private final static Logger logger = LoggerFactory.getLogger(SerialGamePlayData.class);

    @Override
    public void dump(String fileName){
        try {
            FileOutputStream fos = new FileOutputStream(fileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(getData());
            oos.close();
        } catch (Exception e){
            logger.error(e.getMessage());
        }
    }
}
