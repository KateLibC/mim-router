package io.github.uusdfg.mim.strategy;

import io.github.uusdfg.mim.action.Action;
import io.github.uusdfg.mim.data.Game;

import java.util.Collection;

public interface ActionGenerator {

	Collection<Action> getPossibleActions(Game game);

}
