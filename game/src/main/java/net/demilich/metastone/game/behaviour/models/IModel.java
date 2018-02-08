package net.demilich.metastone.game.behaviour.models;

import net.demilich.metastone.game.GameContext;

public interface IModel {
    Object predict(GameContext context, int playerId);
    void load(String fileName);
}
