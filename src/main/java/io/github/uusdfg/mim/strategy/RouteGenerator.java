package io.github.uusdfg.mim.strategy;

import io.github.uusdfg.mim.action.Route;
import io.github.uusdfg.mim.data.Game;

import java.util.List;

public interface RouteGenerator {

	List<Route> generate(Game baseGame, int numRoutes);

}
