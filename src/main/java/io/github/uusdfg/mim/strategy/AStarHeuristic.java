package io.github.uusdfg.mim.strategy;

import io.github.uusdfg.mim.data.Game;
import io.github.uusdfg.mim.data.Level;
import io.github.uusdfg.mim.data.Player;

public interface AStarHeuristic {

	long estimateRemainingLevelsTime(Game game);

	long estimateLevelTime(Level level, Player player);

}
