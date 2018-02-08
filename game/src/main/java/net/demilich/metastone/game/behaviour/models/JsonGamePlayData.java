package net.demilich.metastone.game.behaviour.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.Writer;

public class JsonGamePlayData extends GamePlayData {
    private final static Logger logger = LoggerFactory.getLogger(JsonGamePlayData.class);

    @Override
    public void dump(String fileName) {
        try (Writer writer = new FileWriter(fileName)){
            Gson gson = new GsonBuilder().create();
            gson.toJson(getData(), writer);
        } catch (Exception e){
            logger.error(e.toString());
        }
    }
}
