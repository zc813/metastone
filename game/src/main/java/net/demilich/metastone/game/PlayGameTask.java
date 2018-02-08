package net.demilich.metastone.game;

import net.demilich.metastone.game.cards.CardSet;
import net.demilich.metastone.game.decks.DeckFormat;
import net.demilich.metastone.game.gameconfig.GameConfig;
import net.demilich.metastone.game.gameconfig.PlayerConfig;
import net.demilich.metastone.game.logic.GameLogic;

import java.util.concurrent.Callable;

public abstract class PlayGameTask implements Callable<Void> {
    protected Player player1, player2;
    protected DeckFormat deckFormat;

    public PlayGameTask(Player player1, Player player2){
        DeckFormat deckFormat = new DeckFormat();
        for (CardSet set : CardSet.values()) {
            deckFormat.addSet(set);
        }
        this.player1 = player1.clone();
        this.player2 = player2.clone();
        this.deckFormat = deckFormat;
    }

    public PlayGameTask(Player player1, Player player2, DeckFormat deckFormat){
        this.player1 = player1.clone();
        this.player2 = player2.clone();
        this.deckFormat = deckFormat;
    }

    public PlayGameTask(GameConfig config){
        PlayerConfig playerConfig1 = config.getPlayerConfig1();
        PlayerConfig playerConfig2 = config.getPlayerConfig2();

        Player player1 = new Player(playerConfig1);
        Player player2 = new Player(playerConfig2);

        DeckFormat deckFormat = config.getDeckFormat();
    }

    public final Void call(){
        // each player is randomly played in evaluation, since init() is called each time
        GameContext newGame = new GameContext(player1, player2, new GameLogic(), deckFormat);
        newGame.playTask(this);
        newGame.dispose();
        return null;
    }

    protected void onTurnStart(GameContext context, int playerId){}

    protected void onActionStart(GameContext context, int playerId){}

    protected void onActionEnd(GameContext context, int playerId){}

    protected void onTurnEnd(GameContext context, int playerId){}

    protected void onGameEnd(GameContext context, int winner){}
}
