package net.demilich.metastone.gui.trainingmode;

import net.demilich.metastone.game.behaviour.heuristic.ModelHeuristic;
import net.demilich.metastone.game.behaviour.mcts.ModelMCTS;
import net.demilich.metastone.game.behaviour.models.*;
import net.demilich.metastone.game.behaviour.threat.GameStateValueBehaviour;
import net.demilich.metastone.game.behaviour.threat.ThreatBasedHeuristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.demilich.nittygrittymvc.SimpleCommand;
import net.demilich.nittygrittymvc.interfaces.INotification;
import net.demilich.metastone.GameNotification;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.gameconfig.GameConfig;
import net.demilich.metastone.game.gameconfig.PlayerConfig;
import net.demilich.metastone.utils.Tuple;

import java.io.File;

public class AlphaTrainingCommand extends SimpleCommand<GameNotification> {
    private static Logger logger = LoggerFactory.getLogger(AlphaTrainingCommand.class);
    private int gamesCompleted;

    private final static String DATA_PATH = AlphaTrainingCommand.class.getResource("/selfplay/").getPath() + "%x.data";
    private final static String MODELS_FOLDER = AlphaTrainingCommand.class.getResource("/models/").getPath();
    private final static int SELFPLAY_ROUNDS = 12;
    private final static int EVALUATION_ROUNDS = 10;

    private final static int INTERVAL_ROUNDS = 100;
    private final static int INTERVAL_EVAL_ROUNDS = 20;

    private int modelLatestVersion = 0, modelBestVersion = 0;

    private long lastLoaded = -1;

    public AlphaTrainingCommand(){
        logger.info(DATA_PATH);
        logger.info(MODELS_FOLDER);
        updateLatestModel();
        modelBestVersion = modelLatestVersion;
    }

    private void selfPlay(Player player, int rounds){
        GamePlayData selfPlayData = new JsonGamePlayData();
        for (int i = 0; i < rounds; i++) {
            SelfPlayGameTask selfPlayTask = new SelfPlayGameTask(player);

            selfPlayTask.call();

            logger.info("Self-play round {} finished.", gamesCompleted + i);
            selfPlayData.merge(selfPlayTask.getData()); // 每次打完一局都合并一下数据，但不写

            if ((i+1) % 4 == 0) { // 每 4 局会写一下数据
                String saveTo = String.format(DATA_PATH, System.currentTimeMillis());
                selfPlayData.dump(saveTo);
                selfPlayData.clear();
                logger.info("Saved to {}", saveTo);
            }
        }
    }

    /**
     * attacker 和 defeater 对战
     * @return (boolean) attacker 胜超过一半局
     */
    private boolean evaluate(Player defeater, Player attacker, int rounds){
        // attacker wins : true
        float player1Win = 0, player2Win = 0;
        for (int i = 0; i < rounds; i++) {
            EvaluateGameTask evaluateTask = new EvaluateGameTask(defeater, attacker);

            evaluateTask.call();

            Tuple<Float, Float> winningCount = evaluateTask.getResult();
            player1Win += winningCount.getFirst();
            player2Win += winningCount.getSecond();
            logger.info("Evaluation round {}/{} finished with number of winning games {} vs {}", i, rounds, player1Win, player2Win);

            if (player1Win * 2 >= rounds) return false; // end early (3/5, 2/4) and not update
            if (player2Win * 2 > rounds) return true; // end early (3/5, 3/4) and update
        }
        return player1Win < player2Win;
    }

    /**
     * attacker 和 defeater 对战
     * @return (double) attacker 胜率
     */
    private double evaluateResult(Player defeater, Player attacker, int rounds){
        float player1Win = 0, player2Win = 0;
        for (int i = 0; i < rounds; i++) {
            EvaluateGameTask evaluateTask = new EvaluateGameTask(defeater, attacker);

            evaluateTask.call();

            Tuple<Float, Float> winningCount = evaluateTask.getResult();
            player1Win += winningCount.getFirst();
            player2Win += winningCount.getSecond();
        }
        return player2Win / (player1Win + player2Win);
    }


    @Override
    public void execute(INotification<GameNotification> notification) {
        final GameConfig gameConfig = (GameConfig) notification.getBody();

        gamesCompleted = 0;

        PlayerConfig playerConfig = gameConfig.getPlayerConfig1();

        IModel bestModel = new TFValueModel(getModelBestPath());
        Player bestPlayer = new Player(playerConfig);
        bestPlayer.setBehaviour(new ModelMCTS(new ModelHeuristic(bestModel)));

        IModel latestModel = new TFValueModel(getModelLatestPath());
        Player latestPlayer = new Player(playerConfig);
        latestPlayer.setBehaviour(new ModelMCTS(new ModelHeuristic(latestModel)));

        Player gameStatePlayer = new Player(playerConfig);
        gameStatePlayer.setBehaviour(new GameStateValueBehaviour());

        boolean noEvaluateFlag = true; // 每次新模型来了之后是否进行 evaluate 后才更新
        boolean intervalEvalFlag = true; // 每次训了 INTERVAL_ROUNDS 后是否简短对战看下效果，不论是否有新模型

        while (gamesCompleted < gameConfig.getNumberOfGames()) {
            // 训练 SELFPLAY_ROUNDS 局
            selfPlay(bestPlayer, SELFPLAY_ROUNDS);
            gamesCompleted += SELFPLAY_ROUNDS;

            if (intervalEvalFlag && gamesCompleted % INTERVAL_ROUNDS < SELFPLAY_ROUNDS){
                // 和之前的 bestPlayer 对战
                logger.info("Evaluate against prev best player {} for {} rounds with win rate {}",
                        modelBestVersion,
                        INTERVAL_EVAL_ROUNDS,
                        evaluateResult(bestPlayer, latestPlayer, INTERVAL_EVAL_ROUNDS));

                // 和 gameState 对战
                logger.info("Evaluate against game state for {} rounds with win rate {}",
                        INTERVAL_EVAL_ROUNDS,
                        evaluateResult(gameStatePlayer, latestPlayer, INTERVAL_EVAL_ROUNDS));
            }

            // 检查是否有新模型
            if (updateLatestModel()) {
                logger.info("New model {} available. Evaluation starts against current best {}.",
                        modelLatestVersion, modelBestVersion);

                try {
                    Thread.sleep(300); // wait for model to be written
                } catch (InterruptedException e){

                }
                latestModel.load(getModelLatestPath());

                // 更新当前 best player，用于之后的 selfplay
                if (noEvaluateFlag || evaluate(bestPlayer, latestPlayer, EVALUATION_ROUNDS)) {
                    modelBestVersion = modelLatestVersion;
                    bestModel.load(getModelBestPath());
                    logger.info("New model wins. Updated to {}.", modelBestVersion);
                }
            } else
                // 没有新模型
                logger.info("Checking new model...No new model available. Latest: {}. Current best: {}.",
                        modelLatestVersion, modelBestVersion);
        }

        logger.info("Simulation finished");
    }

    private boolean updateLatestModel(){
        // check new model one by one till reaching the latest
        boolean latestAvailable = false;
        while (true){
            File f = new File(MODELS_FOLDER + String.valueOf(modelLatestVersion + 1));
            if (!f.exists())
                break;
            modelLatestVersion++;
            latestAvailable = true;
        }
        return latestAvailable;
    }

    private String getModelLatestPath(){
        return MODELS_FOLDER + String.valueOf(modelLatestVersion);
    }

    private String getModelBestPath(){
        return MODELS_FOLDER + String.valueOf(modelBestVersion);
    }
}
