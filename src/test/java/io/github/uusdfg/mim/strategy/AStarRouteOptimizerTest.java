package io.github.uusdfg.mim.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import io.github.uusdfg.mim.action.Action;
import io.github.uusdfg.mim.action.Route;
import io.github.uusdfg.mim.action.TakeTransition;
import io.github.uusdfg.mim.data.Crossing;
import io.github.uusdfg.mim.data.Entrance;
import io.github.uusdfg.mim.data.Exit;
import io.github.uusdfg.mim.data.Floor;
import io.github.uusdfg.mim.data.Game;
import io.github.uusdfg.mim.data.Kiosk;
import io.github.uusdfg.mim.data.Level;
import io.github.uusdfg.mim.data.Player;
import io.github.uusdfg.mim.data.Position;
import io.github.uusdfg.mim.data.Road;
import io.github.uusdfg.mim.data.Transition;
import io.github.uusdfg.mim.data.loader.SaveStateLoader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit tests for cases where {@link AStarRouteOptimizer} used to behave
 * suboptimally.
 * 
 * TODO Add some more cases where you know what the correct behavior should be
 * so you can test proactively rather than waiting until you notice another
 * problem with the generator's output.
 */
public class AStarRouteOptimizerTest {

	private final SaveStateLoader loader = new SaveStateLoader();

	/**
	 * Verifies that the optimizer does not choose a suboptimal route in a
	 * certain case in Beijing (1-4) that it used to fail. In the route in
	 * question, artifact 0 was returned last. At that point, Luigi can run to
	 * either of the street crossing on the Kiosk's road and go from there to
	 * the exit. The optimizer used to pick the crossing on the left even though
	 * the exit was to the <em>right</em> of the far end of both crossings. This
	 * confirms that the optimizer doesn't prune the "good" crossings (the ones
	 * on the right), and also that it prioritizes exit routes that use the good
	 * crossings over ones that run left and then back to the right.
	 */
	@Test
	@Ignore("Disabled because I don't want to pack in the savestate")
	public void testBeijingExitRoute() throws IOException {
		final Level level = loadLevelFromResource(4, "1-4.nnn");
		final Game game = makeMiniGame(1, level);

		// Put the player on the kiosk at the bottom of the level. Also, mark
		// all of the kiosks as completed so our heuristic won't be baffled when
		// we try to exit the level later.
		for (Kiosk kiosk : level.getKiosks()) {
			kiosk.setCompleted(true);
		}
		final Kiosk weirdKiosk = level.getKiosk(0);

		final Player player = game.getPlayer();
		final Position kioskPos = weirdKiosk.getBasePos();
		player.setPos(weirdKiosk.getBasePos());
		player.setYoshi(true);

		// See how the optimizer feels about the possible ways to get back to
		// the "main" road from that bottom road. It _should_ distinguish the
		// two crossings because it's impossible to run to one, cross the
		// street, and then run to the other in the time it takes to just take
		// one of them. Also, it should distinguish the two sides of each
		// crossing because it's possible to shift a few pixels by going up at
		// the very edge of the transition. So, we should get back all four
		// transitions.
		final Route baseRoute = new Route(game);
		final int roadId = kioskPos.getRoadIndex();
		final Road road = level.getRoad(kioskPos.getRoadIndex());

		final AStarRouteOptimizer ringo = new AStarRouteOptimizer();
		final Queue<Route> queue = getField(ringo, "levelRouteQueue");
		for (int i = 0; i < road.getTransitions().size(); i++) {
			final Transition transition = road.getTransitions().get(i);
			if (transition instanceof Crossing) {
				final Route route = new Route(baseRoute);
				route.performAction(new TakeTransition(roadId, i));
				ringo.addLevelStepToQueue(route, queue);
			}
		}

		final int numCrossingRoutes = queue.size();
		assertEquals("Wrong number of possible crossing routes kept in queue: "
				+ queue, 4, queue.size());

		// Now try getting to the exit from each of those places. The optimizer
		// shouldn't have a problem with doing that.
		final Action exitAction = getExitAction(level);
		for (int i = 0; i < numCrossingRoutes; i++) {
			final Route crossingRoute = queue.remove();
			final Route exitRoute = new Route(crossingRoute);
			exitRoute.performAction(exitAction);

			System.err.printf("Added exit to %s%n", crossingRoute);
			ringo.addLevelStepToQueue(exitRoute, queue);
		}
		assertEquals("Wrong number of possible exit routes kept in queue: "
				+ queue, 4, queue.size());

		// Finally, make sure that the exit routes come out of the queue in the
		// right order. Since the exit is to the right of both crossings, the
		// right crossing should obviously be faster. Also, the right side of
		// the right crossing should be the best due to how the crossing
		// detection boxes work.
		final List<Integer> exitCrossingSrcXs = new ArrayList<>();
		while (!queue.isEmpty()) {
			final Route route = queue.remove();
			final TakeTransition crossingAction = (TakeTransition) route
					.getHistory().get(0).action;
			exitCrossingSrcXs.add(crossingAction.getTransition(game)
					.getSrcPos().getX());
		}

		for (int i = 0; i < exitCrossingSrcXs.size() - 1; i++) {
			final int betterX = exitCrossingSrcXs.get(i);
			final int worseX = exitCrossingSrcXs.get(i + 1);
			assertTrue("Crossing Xs out of order: " + exitCrossingSrcXs,
					betterX > worseX);
		}
	}

	protected Level loadLevelFromResource(final int id, final String name)
			throws IOException {
		final BufferedInputStream levelStream = new BufferedInputStream(
				new GZIPInputStream(Thread.currentThread()
						.getContextClassLoader().getResourceAsStream(name)));
		final Level level;
		try {
			level = loader.loadLevelFromStream(id, levelStream);
		} finally {
			IOUtils.closeQuietly(levelStream);
		}
		return level;
	}

	protected Game makeMiniGame(final int floorId, final Level level) {
		final Game game = new Game();

		final Floor floor = new Floor(floorId, new Position(false,
				Road.CASTLE_ROAD_ID, 1));
		game.addFloor(floor);
		floor.addLevel(level);

		final Road castleRoad = new Road(null, Road.CASTLE_ROAD_ID,
				Floor.DEFAULT_CASTLE_ROAD_WIDTH);
		castleRoad.addTransition(new Entrance(new Position(false,
				Road.CASTLE_ROAD_ID, 2), new Position(level.getStartPos()),
				level.getLevelId(), 200));
		floor.setCastleRoad(castleRoad);

		final Player player = game.getPlayer();
		player.setFloor(floorId);
		player.setLevel(level);
		player.setPos(level.getStartPos());

		return game;
	}

	protected Action getExitAction(final Level level) {
		final int exitRoadId = level.getStartPos().getRoadIndex();
		final Road startRoad = level.getRoad(exitRoadId);
		for (int i = 0; i < startRoad.getTransitions().size(); i++) {
			final Transition transition = startRoad.getTransitions().get(i);
			if (transition instanceof Exit) {
				return new TakeTransition(exitRoadId, i);
			}
		}
		// :(
		return null;
	}

	@SuppressWarnings("unchecked")
	protected <T> T getField(final Object object, final String fieldName) {
		final Class<?> clazz = object.getClass();
		try {
			final Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);
			return (T) field.get(object);
		} catch (final Exception e) {
			throw new RuntimeException("Error accessing field " + fieldName, e);
		}
	}
}
