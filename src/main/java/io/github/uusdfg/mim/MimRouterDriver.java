package io.github.uusdfg.mim;

import io.github.uusdfg.mim.action.Route;
import io.github.uusdfg.mim.data.Game;
import io.github.uusdfg.mim.data.loader.SaveStateLoader;
import io.github.uusdfg.mim.strategy.AStarRouteOptimizer;
import io.github.uusdfg.mim.strategy.DefaultActionGenerator;
import io.github.uusdfg.mim.strategy.RouteGenerator;
import io.github.uusdfg.mim.strategy.SlightlyBetterHeuristic;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MimRouterDriver {

	private RouteGenerator routeGenerator;

	private File baseDir;

	private Game baseGame;

	private List<Route> routes;

	public MimRouterDriver() {
	}

	public MimRouterDriver(final RouteGenerator routeGenerator) {
		setRouteGenerator(routeGenerator);
	}

	public final RouteGenerator getRouteGenerator() {
		return routeGenerator;
	}

	public final Game getBaseGame() {
		return baseGame;
	}

	public final List<Route> getRoutes() {
		return routes;
	}

	public final File getBaseDir() {
		return baseDir;
	}

	public final MimRouterDriver setRouteGenerator(
			final RouteGenerator routeGenerator) {
		this.routeGenerator = routeGenerator;
		return this;
	}

	public final MimRouterDriver setBaseGame(final Game baseGame) {
		this.baseGame = baseGame;
		return this;
	}

	public void loadSaveStates(final File baseDir) throws IOException {
		clear();

		this.baseGame = new SaveStateLoader().loadGameFromDir(baseDir);
		this.baseDir = baseDir;
	}

	public List<Route> generateRoute() {
		routes = routeGenerator.generate(baseGame, 10);
		return routes;
	}

	public static void main(final String[] args) throws IOException {
		final MimRouterDriver router = new MimRouterDriver(
				new AStarRouteOptimizer(new DefaultActionGenerator(),
						new SlightlyBetterHeuristic()));
		router.loadSaveStates(getBaseDir(args));

		final List<Route> routes = router.generateRoute();
		if (routes.isEmpty()) {
			System.out.println("Oh, man, this game is impossible!");
		}

		for (int i = 0; i < routes.size(); i++) {
			final Route route = routes.get(i);

			System.out.printf("Route #%d:%n", i);
			System.out.printf("%s%n%n", route);
		}
	}

	protected void clear() {
		baseDir = null;
		baseGame = null;
		routes = null;
	}

	protected static File getBaseDir(final String[] args) {
		if (args.length < 1) {
			throw new IllegalArgumentException(
					"Missing command line argument with savestate directory path");
		}
		final String path = args[0];

		final File baseDir = new File(path);
		if (!baseDir.isDirectory()) {
			throw new IllegalArgumentException("Base directory " + path
					+ " does not exist.");
		}
		return baseDir;
	}

}
