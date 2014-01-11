package io.github.uusdfg.mim.strategy;

import io.github.uusdfg.mim.action.Action;
import io.github.uusdfg.mim.action.GetYoshi;
import io.github.uusdfg.mim.action.Route;
import io.github.uusdfg.mim.action.TakeTransition;
import io.github.uusdfg.mim.action.Route.HistoryEntry;
import io.github.uusdfg.mim.data.Exit;
import io.github.uusdfg.mim.data.Floor;
import io.github.uusdfg.mim.data.Game;
import io.github.uusdfg.mim.data.Level;
import io.github.uusdfg.mim.data.Player;
import io.github.uusdfg.mim.data.Position;
import io.github.uusdfg.mim.data.Road;
import io.github.uusdfg.mim.data.Transition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

public class AStarRouteOptimizer implements RouteGenerator {

	private final ActionGenerator actionGenerator;

	private final AStarHeuristic heuristic;

	private final Queue<Route> overallRouteQueue = new PriorityQueue<>(1000,
			new OutsideLevelRouteTimeComparator());

	private final Queue<Route> levelRouteQueue = new PriorityQueue<>(1000,
			new InLevelRouteTimeComparator());

	private final Map<Integer, Collection<HistoryEntry>> levelSnapshotsByCode = new LinkedHashMap<>();

	private long totalLevelSolvingTime = 0;

	private long totalGetActionsTime = 0;

	private long totalCopyRouteTime = 0;

	private long totalPerformActionTime = 0;

	private long totalAddToQueueTime = 0;

	private long totalAddToQueueOpsTime = 0;

	private long totalLevelHeuristicTime = 0;

	private long solutionsComputed = 0;

	private long timesThatThingWorked = 0;

	public AStarRouteOptimizer() {
		this(new DefaultActionGenerator(), new SlightlyBetterHeuristic());
	}

	public AStarRouteOptimizer(final ActionGenerator actionGenerator,
			final AStarHeuristic heuristic) {
		this.actionGenerator = actionGenerator;
		this.heuristic = heuristic;
	}

	public List<Route> generate(final Game baseGame, final int numRoutes) {
		overallRouteQueue.clear();
		overallRouteQueue.add(new Route(baseGame));

		final List<Route> completeRoutes = new ArrayList<>(numRoutes);
		while ((completeRoutes.size() < numRoutes)
				&& !overallRouteQueue.isEmpty()) {
			final Route route = overallRouteQueue.remove();

			if (route.getFinalState().isCompleted()) {
				completeRoutes.add(route);
			} else if (justEnteredLevel(route)) {
				// TODO Make immediate level exits not completely awful
				// In the meantime, don't even try to simulate them. The big
				// problem right now is that if we do simulate them, we get
				// thousands/millions of routes that just go in and out of
				// various levels even though they're no faster than playing
				// through the game normally. We probably need to cap the number
				// of times an immediate exit can be done before the generator
				// assumes the route is too slow and gives up.
				//
				// addImmediateExitToQueue(route, overallRouteQueue);

				// Okay, now that my rant about immediate exits is over,
				// complete the level and add the resulting route back to the
				// queue.
				final Route levelRoute = completeLevel(route);
				addLevelSolutionToQueue(levelRoute, overallRouteQueue);

				whine();
			} else {
				expandOutsideLevel(route, overallRouteQueue);
			}
		}

		return completeRoutes;
	}

	protected boolean justEnteredLevel(final Route route) {
		final Action lastAction = route.getLastAction();
		if (!(lastAction instanceof TakeTransition)) {
			return false;
		}

		// XXX This is stupid and ugly. :/
		final TakeTransition take = (TakeTransition) lastAction;
		return (take.getRoadId() == Road.CASTLE_ROAD_ID)
				&& (take.getTransitionIndex() < 5);
	}

	protected Route completeLevel(final Route baseRoute) {
		final long levelSolvingStartTime = System.nanoTime();

		final Player player = baseRoute.getFinalState().getPlayer();
		final int floorId = player.getFloor();
		final int levelId = player.getLevel().getLevelId();

		levelRouteQueue.clear();
		levelSnapshotsByCode.clear();

		// Assume that we start the level by getting Yoshi because it's
		// extremely unlikely, if not impossible, for any other move to be
		// faster. Also, this dramatically shrinks the state space because it
		// prunes all the branches for getting Yoshi in other parts of the
		// level.
		final Route realBaseRoute = new Route(baseRoute);
		realBaseRoute.performAction(new GetYoshi());
		levelRouteQueue.add(realBaseRoute);

		System.err.printf("Trying to finish %d-%d%n", floorId, levelId);
		int numAttempts = 0;
		Route completeRoute = null;
		while ((completeRoute == null) && !levelRouteQueue.isEmpty()) {
			final Route route = levelRouteQueue.remove();

			if (isLevelCompleted(route, floorId, levelId)) {
				completeRoute = route;
			} else {
				expandInLevel(route, levelRouteQueue);
			}

			++numAttempts;
		}

		final long levelSolvingTime = System.nanoTime() - levelSolvingStartTime;
		totalLevelSolvingTime += levelSolvingTime;
		System.err
				.printf("After %.2f s and %d tries, complete route for %d-%d (%d done) takes %d%n",
						((double) levelSolvingTime) / 1e9,
						(numAttempts + levelRouteQueue.size()), floorId,
						levelId, completeRoute.getFinalState()
								.getNumLevelsCompleted(), completeRoute
								.getTotalTime());

		solutionsComputed++;
		return completeRoute;
	}

	protected boolean isLevelCompleted(final Route route, final int floorId,
			final int levelId) {
		final Level level = route.getFinalState().getLevel(floorId, levelId);
		return level.isCompleted();
	}

	protected void expandOutsideLevel(final Route route,
			final Queue<Route> outRouteQueue) {
		final Collection<Action> actions = actionGenerator
				.getPossibleActions(route.getFinalState());

		for (Action action : actions) {
			final Route routeWithAction = new Route(route);
			routeWithAction.performAction(action);
			outRouteQueue.add(routeWithAction);
		}
	}

	protected void expandInLevel(final Route route,
			final Queue<Route> outRouteQueue) {
		final Player player = route.getFinalState().getPlayer();
		final int floorId = player.getFloor();
		final int levelId = player.getLevel().getLevelId();

		final long getTimeStart = System.nanoTime();
		final Collection<Action> actions = actionGenerator
				.getPossibleActions(route.getFinalState());
		totalGetActionsTime += System.nanoTime() - getTimeStart;

		for (Action action : actions) {
			final long copyRouteTimeStart = System.nanoTime();
			final Route routeWithAction = new Route(route);
			totalCopyRouteTime += System.nanoTime() - copyRouteTimeStart;

			final long performTimeStart = System.nanoTime();
			routeWithAction.performAction(action);
			totalPerformActionTime += System.nanoTime() - performTimeStart;

			// Don't let the player exit the level immediately. We want to
			// actually complete the level.
			if (!routeWithAction.getFinalState().getPlayer().isInCastle()
					|| isLevelCompleted(routeWithAction, floorId, levelId)) {
				final long queueTimeStart = System.nanoTime();
				addLevelStepToQueue(routeWithAction, outRouteQueue);
				totalAddToQueueTime += System.nanoTime() - queueTimeStart;
			}
		}
	}

	protected void addLevelStepToQueue(final Route route,
			final Queue<Route> outRouteQueue) {
		// As a first check, see if this route completes the level. If it does,
		// we want it in the queue immediately so we can use the queue to find
		// the fastest solution for us. Additional, non-O(log n) pruning is a
		// waste of time in this case.
		final Game game = route.getFinalState();
		final Player player = game.getPlayer();
		if (player.isInCastle()) {
			outRouteQueue.add(route);
			return;
		}

		// Next, make sure it isn't completely stupid. If it is, don't bother
		// looking at it further.
		if (pruneIndividual(route)) {
			return;
		}

		// Now go through all of the existing routes and see if this one
		// duplicates any of them. If it does and it's slower, don't add it to
		// the queue. If it does and it's _faster_, take the old run out. As
		// long as this method is called consistently, there won't ever be more
		// than one duplicate run in the queue.
		//
		// To minimize the amount of time wasted doing this, use a collection of
		// routes that are pre-sorted by how far the player has progressed
		// through the level. That way, we'll only have to check the routes that
		// have already made the same progress.
		Route badExistingRoute = null;
		HistoryEntry badExistingHistory = null;
		boolean shouldAddRoute = true;
		final int completionCode = player.getLevel().getCompletionCode();
		Collection<HistoryEntry> historyForCode = levelSnapshotsByCode
				.get(completionCode);
		if (historyForCode == null) {
			historyForCode = new ArrayList<>();
			levelSnapshotsByCode.put(completionCode, historyForCode);
		} else {
			final HistoryEntry existingEntry = findEquivalentHistoryEntry(
					route, historyForCode);
			if (existingEntry != null) {
				shouldAddRoute = (route.getTotalTime() < existingEntry.cumulativeTime);
				if (shouldAddRoute) {
					badExistingHistory = existingEntry;
					badExistingRoute = existingEntry.backReference;
				}
			}
		}

		final long queueOpsTimeStart = System.nanoTime();
		if (badExistingRoute != null) {
			outRouteQueue.remove(badExistingRoute);

			// Don't forget to clean the bad entry out of our per-road map, or
			// we won't really be removing it.
			historyForCode.remove(badExistingHistory);
		}
		if (shouldAddRoute) {
			outRouteQueue.add(route);

			// Remember the history for this road for next time.
			historyForCode.add(route.getHistory().get(
					route.getHistory().size() - 1));
		}
		totalAddToQueueOpsTime += System.nanoTime() - queueOpsTimeStart;
	}

	protected boolean pruneIndividual(final Route route) {
		// Don't record the route if it has doubled back on itself. The player
		// runs faster than the Koopas and we will never gain anything by
		// waiting for them to run around.
		return (findEquivalentHistoryEntry(route, route.getHistory()) != null);
	}

	protected HistoryEntry findEquivalentHistoryEntry(final Route route,
			final Collection<HistoryEntry> history) {
		// We consider states equivalent if:
		// 1. The RNG is the same. We don't want to prune a route with an RNG
		// that could save time later.
		// 2. The various flags within each level (kiosks, etc.) are the same.
		// Moving the player in a loop is fine as long as stuff gets done.
		// 3. The player's positions are close enough that if you consider the
		// time elapsed between the given history state and the current one, the
		// player could have walked to the current position and gotten there
		// faster.
		final long routeTime = route.getTotalTime();
		final Game game = route.getFinalState();
		final Player player = game.getPlayer();
		for (HistoryEntry entry : history) {
			final Game oldGame = entry.state;
			final long timeSince = routeTime - entry.cumulativeTime;
			if ((game != oldGame)
					&& essentiallySamePosition(player, oldGame.getPlayer(),
							Math.abs(timeSince))
					&& sameProgressInLevel(game, oldGame, routeTime,
							entry.cumulativeTime)
					&& game.getSlotRng().equals(oldGame.getSlotRng())) {
				return entry;
			}
		}

		return null;
	}

	protected boolean essentiallySamePosition(final Player newPlayer,
			final Player oldPlayer, final long timeDelta) {
		// Have to be on the same road. Not comparable otherwise. Also watch out
		// for players leaving the level.
		final Position newPos = newPlayer.getPos();
		final Position oldPos = oldPlayer.getPos();
		if ((newPlayer.isInCastle() != oldPlayer.isInCastle())
				|| (newPos.getRoadIndex() != oldPos.getRoadIndex())) {
			return false;
		}

		// See if we've gone farther than we could have run in the given amount
		// of time. If we haven't, unless we tripped a flag to advance the
		// level, we've essentially wasted that time.
		final long distance = newPos.getXdistance(oldPos);
		final long maxPossibleDistance = oldPlayer.getRunSpeed() * timeDelta;
		return (distance <= maxPossibleDistance);
	}

	protected boolean sameProgressInLevel(final Game game1, final Game game2,
			final long time1, final long time2) {
		// XXX Assume we'll only be called if both games are in the same
		// level-solving loop, and don't bother comparing levels/floors.
		final Player player1 = game1.getPlayer();
		final Player player2 = game2.getPlayer();

		// Okay, let's see if the player's taken care of any new level
		// objectives. Yoshi counts.
		if (player1.hasYoshi() != player2.hasYoshi()) {
			return false;
		}

		final Level level1 = player1.getLevel();
		final Level level2 = player2.getLevel();
		if (level1.getCompletionCode() != level2.getCompletionCode()) {
			return false;
		}

		// Also, we have to check the transition delay end time because it takes
		// up too many bits to go into the completion code. If two routes are
		// basically the same, but one prevents the player from crossing the
		// street and the other doesn't, we want the one that lets the player
		// cross the street. This only matters if either route is actually in a
		// state where the player can't cross the street, though...
		final long remainingTransitionDelay1 = player1.getTransitionDelayEnd()
				- time1;
		final long remainingTransitionDelay2 = player2.getTransitionDelayEnd()
				- time2;
		return ((remainingTransitionDelay1 == remainingTransitionDelay2) || (remainingTransitionDelay1 <= 0));
	}

	protected void addLevelSolutionToQueue(final Route newRoute,
			final Queue<Route> routeQueue) {
		boolean shouldAdd = true;
		Route routeToRemove = null;

		final Game game = newRoute.getFinalState();
		for (Route existingRoute : routeQueue) {
			// 1. The routes must have the same RNG. We can't compare routes
			// with different RNGs because we don't know what effect those RNGs
			// will have later on.
			final Game existingGame = existingRoute.getFinalState();
			boolean match = game.getSlotRng().equals(existingGame.getSlotRng());

			// 2. The routes must have completed exactly the same levels.
			for (final Iterator<Floor> fit = game.getFloors().iterator(); match
					&& fit.hasNext();) {
				final Floor floor = fit.next();
				for (final Iterator<Level> lit = floor.getLevels().iterator(); match
						&& lit.hasNext();) {
					final Level level = lit.next();
					match = level.isCompleted() == existingGame.getLevel(
							floor.getFloorId(), level.getLevelId())
							.isCompleted();
				}
			}

			if (match) {
				// XXX See if this is worth it...
				timesThatThingWorked++;

				// If the routes match, plan to keep whichever one is faster.
				if (newRoute.getTotalTime() < existingRoute.getTotalTime()) {
					routeToRemove = existingRoute;
				} else {
					shouldAdd = false;
				}

				// As long as we always use this method to add routes, we'll
				// never have duplicates, so we can stop now.
				break;
			}
		}

		if (routeToRemove != null) {
			routeQueue.remove(routeToRemove);
		}
		if (shouldAdd) {
			routeQueue.add(newRoute);
		}
	}

	protected void addImmediateExitToQueue(final Route route,
			final Queue<Route> outRouteQueue) {
		final Route exitRoute = new Route(route);
		final Player player = exitRoute.getFinalState().getPlayer();
		final int roadIndex = player.getPos().getRoadIndex();
		final Road road = player.getLevel().getRoad(roadIndex);

		int tIndex = Integer.MAX_VALUE;
		final List<Transition> transitions = road.getTransitions();
		for (int i = 0; i < transitions.size(); i++) {
			final Transition transition = transitions.get(i);
			if (transition instanceof Exit) {
				tIndex = i;
				break;
			}
		}
		if (tIndex == Integer.MAX_VALUE) {
			System.err.println("wtfh.");
			System.exit(25);
		}

		exitRoute.performAction(new TakeTransition(roadIndex, tIndex));
		addLevelSolutionToQueue(exitRoute, outRouteQueue);
	}

	protected class OutsideLevelRouteTimeComparator implements
			Comparator<Route> {
		public int compare(final Route o1, final Route o2) {
			return Long.signum(getTime(o1) - getTime(o2));
		}

		private long getTime(final Route route) {
			final Game game = route.getFinalState();
			return heuristic.estimateRemainingLevelsTime(game)
					+ route.getTotalTime();
		}
	}

	protected class InLevelRouteTimeComparator implements Comparator<Route> {
		public int compare(final Route o1, final Route o2) {
			return Long.signum(getTime(o1) - getTime(o2));
		}

		private long getTime(final Route route) {
			final Game game = route.getFinalState();
			final Player player = game.getPlayer();

			long time = route.getTotalTime();
			if (!player.isInCastle()) {
				final long heuristicTimeStart = System.nanoTime();
				time += heuristic.estimateLevelTime(player.getLevel(), player);
				totalLevelHeuristicTime += System.nanoTime()
						- heuristicTimeStart;
			}
			return time;
		}
	}

	private void whine() {
		System.err.printf("Total level time = %.2f%n",
				totalLevelSolvingTime / 1e9);
		System.err
				.printf("get=%.2f, copy=%.2f, perf=%.2f, add=%.2f, add-ops=%.2f, guess=%.2f%n",
						totalGetActionsTime / 1e9, totalCopyRouteTime / 1e9,
						totalPerformActionTime / 1e9,
						totalAddToQueueTime / 1e9,
						totalAddToQueueOpsTime / 1e9,
						totalLevelHeuristicTime / 1e9);
		System.err.printf(
				"Computed %d level solutions; that thing worked %d times.%n",
				solutionsComputed, timesThatThingWorked);
	}

}
