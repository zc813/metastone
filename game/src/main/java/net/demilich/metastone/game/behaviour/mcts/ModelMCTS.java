package net.demilich.metastone.game.behaviour.mcts;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import net.demilich.metastone.game.actions.ActionType;
import net.demilich.metastone.game.behaviour.heuristic.IGameStateHeuristic;
import net.demilich.metastone.game.behaviour.heuristic.ModelHeuristic;
import net.demilich.metastone.game.behaviour.heuristic.ScalingHeuristicWrapper;
import net.demilich.metastone.game.behaviour.models.TFValueModel;
import net.demilich.metastone.game.behaviour.threat.FeatureVector;
import net.demilich.metastone.game.behaviour.threat.ThreatBasedHeuristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;

import static java.lang.Integer.max;

public class ModelMCTS extends MonteCarloTreeSearch{

    private static final String BEST_MODEL_PATH = ModelMCTS.class.getResource("/models/0/").getPath(); // 5, 13, 17, 21, 38, 41

    // use untrained game tree heuristcs: 38.0% Game Tree (100rounds)

    private final static Logger logger = LoggerFactory.getLogger(ModelMCTS.class);

    private static final int ITERATIONS = 500;

    public IGameStateHeuristic getNodeHeuristic() {
        return nodeHeuristic;
    }

    public void setNodeHeuristic(IGameStateHeuristic nodeHeuristic) {
        this.nodeHeuristic = nodeHeuristic;
    }

    private IGameStateHeuristic nodeHeuristic;

    public ModelMCTS(){
        File parent = new File(BEST_MODEL_PATH).getParentFile();
        Integer version = 0;
        for (File f : parent.listFiles()){
            if (f.getName().matches("[0-9]{1,}"))
                version = max(version, Integer.valueOf(f.getName()));
        }
        logger.info("Using model {}", version);
        nodeHeuristic = new ModelHeuristic(new TFValueModel(new File(parent, String.valueOf(version)).getPath()));
    }

    public ModelMCTS(IGameStateHeuristic heuristic){
        nodeHeuristic = heuristic;
    }

    @Override
    public String getName() {  // 这个似乎还没有完整实现，没法跑， Node.process() 报NullPointer Exception
        return "ModelMCTS";
    }

    @Override
    public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {
        if (nodeHeuristic == null)
            nodeHeuristic = new ModelHeuristic(new TFValueModel(chooseModel()));

        if (validActions.get(0).getActionType() == ActionType.BATTLECRY || validActions.get(0).getActionType() == ActionType.DISCOVER){
            return validActions.get(ThreadLocalRandom.current().nextInt(validActions.size()));
        }

        if (validActions.size() == 1) {
//            logger.info("ModelMCTS selected the only action {}", validActions.get(0));
            return validActions.get(0);
        }

        Node root = new ValueNode(null, player.getId(), nodeHeuristic);
        root.initState(context, validActions);
        GameContext state = root.getState();
        for (Player otherPlayer : state.getPlayers())
            if (otherPlayer != state.getActivePlayer())
                otherPlayer.setBehaviour(this);  // 避免在 MCTS 过程中请求对手 behavior

        UctPolicy treePolicy = new UctPolicy();
        for (int i = 0; i < ITERATIONS; i++) {
//            logger.info(String.valueOf(i)); //
            root.process(treePolicy);
        }

        GameAction bestAction = root.getBestAction();

//        logger.info("ModelMCTS selected best action {}", bestAction);
        return bestAction;
    }

    private String chooseModel(){
        File modelfile = new DirectoryChooser().showDialog(null);
        String path;
        if (modelfile == null || !modelfile.isDirectory()) {
            new Alert(Alert.AlertType.WARNING, "Not valid model path, using `" + BEST_MODEL_PATH +"` instead!", ButtonType.OK).showAndWait();
            path = BEST_MODEL_PATH;
        }else
            path = modelfile.getPath();
        return path;
    }
}
