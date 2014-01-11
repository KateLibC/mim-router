package io.github.uusdfg.mim.strategy;

import io.github.uusdfg.mim.action.GetYoshi;
import io.github.uusdfg.mim.action.ReturnArtifact;
import io.github.uusdfg.mim.action.StompKoopa;
import io.github.uusdfg.mim.data.Crossing;
import io.github.uusdfg.mim.data.Entrance;
import io.github.uusdfg.mim.data.Exit;
import io.github.uusdfg.mim.data.Floor;
import io.github.uusdfg.mim.data.Game;
import io.github.uusdfg.mim.data.Kiosk;
import io.github.uusdfg.mim.data.Koopa;
import io.github.uusdfg.mim.data.Level;
import io.github.uusdfg.mim.data.Pipe;
import io.github.uusdfg.mim.data.Player;
import io.github.uusdfg.mim.data.Position;
import io.github.uusdfg.mim.data.Road;
import io.github.uusdfg.mim.data.Transition;
import io.github.uusdfg.mim.data.Koopa.Status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

public class SlightlyBetterHeuristic implements AStarHeuristic {

	private static final long TRANSITION_ESTIMATE = min(Pipe.TOTAL_TIME,
			Crossing.DOWN_TOTAL_TIME, Crossing.UP_TOTAL_TIME);

	private static final long RUN_SPEED_ESTIMATE = 4;

	private static final long STOMP_PENALTY = StompKoopa.POST_STOMP_TIME;

	private static final long YOSHI_PENALTY = GetYoshi.GLOBULATOR_TIME;

	private final Map<LevelKey, Set<Integer>> fixedSignificantRoads = new HashMap<>();

	// TODO Crazy path/caching thing.

	public long estimateRemainingLevelsTime(final Game game) {
		// TODO Cache the penalties per-level?
		long penalty = 0;
		for (Floor floor : game.getFloors()) {
			final Map<Integer, Entrance> entrances = new HashMap<>();
			for (Transition transition : floor.getCastleRoad().getTransitions()) {
				if (transition instanceof Entrance) {
					final Entrance entrance = (Entrance) transition;
					if (!entrance.isFloorExit()) {
						entrances.put(entrance.getLevelId(), entrance);
					}
				}
			}

			for (Level level : floor.getLevels()) {
				final long minArtifactPenalty = ReturnArtifact
						.getArtifactReturningTime(floor.getFloorId());
				if (!level.isCompleted()) {
					penalty += level.getKiosks().size() * minArtifactPenalty;
					penalty += level.getArtifactHolders().size()
							* STOMP_PENALTY;
					penalty += Exit.TOTAL_TIME;
					penalty += entrances.get(level.getLevelId()).getTotalTime();
					// XXX This is technically wrong if all the kiosks and the
					// exit are on the same road (should be 0 in that case).
					// Fortunately, I don't think that ever happens.
					penalty += getFixedSignificantRoads(level,
							floor.getFloorId()).size()
							* TRANSITION_ESTIMATE;
				}
			}
		}
		return penalty;
	}

	public long estimateLevelTime(final Level level, final Player player) {
		// Do an early sanity check because this sometimes gets called after the
		// player has moved out of the level in question, which makes our other
		// tests really hard to administer.
		if (level.isCompleted() || player.isInCastle()) {
			return 0;
		}

		// Skip the position penalty because it seems to make the optimizer
		// slower. I guess it doesn't prune enough actions to make up for how
		// long it takes to calculate.
		return getStompPenalty(level, player) + getKioskPenalty(level, player)
				+ getYoshiPenalty(player);
	}

	public long getStompPenalty(final Level level, final Player player) {
		int penalty = 0;
		for (Koopa koopa : level.getArtifactHolders()) {
			if (koopa.hasArtifact() && (koopa.getStatus() == Status.ACTIVE)) {
				penalty += STOMP_PENALTY;
			}
		}
		return penalty;
	}

	public long getKioskPenalty(final Level level, final Player player) {
		final long perArtifactPenalty = ReturnArtifact
				.getArtifactReturningTime(player);

		long penalty = 0;
		for (Kiosk kiosk : level.getKiosks()) {
			if (!kiosk.isCompleted()) {
				penalty += perArtifactPenalty;
			}
		}
		return penalty;
	}

	public long getPositionPenalty(final Level level, final Player player) {
		final Position playerPos = player.getPos();
		final int playerRoad = playerPos.getRoadIndex();

		// Figure out the following:
		// 1. Which roads the player must still travel to, at a minimum, in
		// order to complete the level.
		// 2. Which _fixed_ targets the player must reach on those other roads,
		// where, "fixed," basically means, "not an active Koopa."
		// 3. Which fixed targets the player must reach on the current road.
		final Set<Integer> roadsToReach = new LinkedHashSet<>();
		final Multimap<Integer, Object> targetsOnRoads = LinkedHashMultimap
				.create();
		final List<Object> targetsOnCurrentRoad = new ArrayList<>();

		for (Koopa koopa : level.getArtifactHolders()) {
			final int koopaRoad = koopa.getCurrentPos().getRoadIndex();
			if ((koopa.getStatus() != Status.CARRIED)
					&& (koopaRoad != playerRoad)) {
				roadsToReach.add(koopaRoad);
			}
			if (koopa.getStatus() == Status.STOMPED) {
				if (koopaRoad == playerRoad) {
					targetsOnCurrentRoad.add(koopa);
				} else {
					targetsOnRoads.put(koopaRoad, koopa);
				}
			}
		}
		for (Kiosk kiosk : level.getKiosks()) {
			if (kiosk.isCompleted()) {
				continue;
			}

			final int kioskRoad = kiosk.getBasePos().getRoadIndex();
			if (kioskRoad == playerRoad) {
				targetsOnCurrentRoad.add(kiosk);
			} else {
				roadsToReach.add(kioskRoad);
				targetsOnRoads.put(kioskRoad, kiosk);
			}
		}

		// Having done that, we can add the following penalties:
		//
		// 1. The player needs to cross at least as many transitions as there
		// are roads to reach. Also, if the player has nowhere else to go on the
		// current road, the player has to get to a transition to leave. If the
		// player does have somewhere to go, we'll ignore the second part in
		// case that other thing is on the way to a transition.
		final long roadLeavingPenalty = (targetsOnCurrentRoad.isEmpty()) ? (getDistanceTo(
				getNearestTransition(level, playerPos), playerPos) / RUN_SPEED_ESTIMATE)
				: 0;
		final long transitionPenalty = (TRANSITION_ESTIMATE * roadsToReach
				.size()) + roadLeavingPenalty;

		// 2. The player needs to run to whatever kiosks/etc. haven't yet been
		// dealt with on each road. We'll underestimate this a lot to be safe,
		// but we can still do a little with it.
		final long remoteTargetPenalty = getRemoteTargetPenalty(targetsOnRoads,
				level);
		return transitionPenalty + remoteTargetPenalty;
	}

	public long getYoshiPenalty(final Player player) {
		return (player.hasYoshi()) ? 0 : YOSHI_PENALTY;
	}

	protected Transition getNearestTransition(final Level level,
			final Position playerPos) {
		final Road road = level.getRoad(playerPos.getRoadIndex());
		Transition best = null;
		for (Transition transition : road.getTransitions()) {
			if ((best == null)
					|| (getDistanceTo(transition, playerPos) < getDistanceTo(
							best, playerPos))) {
				best = transition;
			}
		}
		return best;
	}

	protected long getMinimumDistanceToReach(final Kiosk kiosk,
			final Level level) {
		final Road road = level.getRoad(kiosk.getBasePos().getRoadIndex());
		long min = Long.MAX_VALUE;
		for (Transition transition : road.getTransitions()) {
			long distance = getDistanceTo(kiosk, transition.getDestPos());
			min = Math.min(min, distance);
		}
		return min;
	}

	protected long getRemoteTargetPenalty(
			final Multimap<Integer, Object> targetsByRoad, final Level level) {
		long penalty = 0;
		for (Map.Entry<Integer, Collection<Object>> entries : targetsByRoad
				.asMap().entrySet()) {
			// Assume the player can attack this road starting from any
			// transition on it. We don't know what route the player is going to
			// take through the level and we can't overestimate.
			final Collection<Object> targets = entries.getValue();
			final Road road = level.getRoad(entries.getKey());

			long minRoadPenalty = (road.getReverseTransitions().isEmpty()) ? 0
					: Long.MAX_VALUE;
			for (Transition transition : road.getReverseTransitions()) {
				// After taking the transition, the player will be exactly at
				// the transition's destination position (at worst, facing the
				// wrong direction). Therefore, we can do this calculation as if
				// the player is at that spot locally.
				final Position tPos = transition.getDestPos();
				minRoadPenalty = Math.min(minRoadPenalty,
						getLocalTargetPenalty(targets, tPos));
			}

			// We know the player needs to hit all of these roads, so it's safe
			// to add up the minimum penalty on each one.
			penalty += minRoadPenalty;
		}
		return penalty;
	}

	protected long getLocalTargetPenalty(final Collection<Object> targets,
			final Position playerPos) {
		// Figure out the minimum distance the player needs to run to do
		// something. Don't try to figure out the minimum distance the player
		// needs to run to do _everything_ because the hit boxes make those
		// calculations weird and I'm lazy.
		long minDistance = Long.MAX_VALUE;
		for (Object target : targets) {
			minDistance = Math.min(minDistance,
					getDistanceToTarget(target, playerPos));
		}
		return (minDistance / RUN_SPEED_ESTIMATE);
	}

	protected long getDistanceToTarget(final Object target, final Position pos) {
		if (target instanceof Kiosk) {
			final Kiosk kiosk = (Kiosk) target;
			return kiosk.getActivePosNearestTo(pos).getXdistance(pos);
		} else {
			final Koopa koopa = (Koopa) target;
			return koopa.getTimeToHitboxEntry(pos, 1);
		}
	}

	protected int getDistanceTo(final Transition transition,
			final Position playerPos) {
		return transition.getActivePosNearestTo(playerPos).getXdistance(
				playerPos);
	}

	protected int getDistanceTo(final Kiosk kiosk, final Position playerPos) {
		return kiosk.getActivePosNearestTo(playerPos).getXdistance(playerPos);
	}

	// TODO Make a graph of all of the transition start/end positions in each
	// level.

	// TODO Cache the shortest paths between those positions, per level, or at
	// least which roads you cross between them.

	// TODO Figure out which roads have the exit + kiosks + artifact holders.

	protected Set<Integer> getFixedSignificantRoads(final Level level,
			final int floorId) {
		final LevelKey key = new LevelKey(level.getLevelId(), floorId);
		Set<Integer> stuff = fixedSignificantRoads.get(key);
		if (stuff != null) {
			return stuff;
		}

		stuff = new LinkedHashSet<>(7);
		stuff.add(level.getStartPos().getRoadIndex());
		for (Kiosk kiosk : level.getKiosks()) {
			stuff.add(kiosk.getBasePos().getRoadIndex());
		}
		fixedSignificantRoads.put(key, stuff);
		return stuff;
	}

	protected Set<Integer> getPerSpawnSignificantRoads(final Level level,
			final int floorId) {
		final Set<Integer> stuff = getFixedSignificantRoads(level, floorId);
		for (Koopa koopa : level.getArtifactHolders()) {
			stuff.add(koopa.getStartPos().getRoadIndex());
		}
		return stuff;
	}

	// TODO On a per-level basis, figure out which roads you might have to cross
	// to get all of the significant roads.

	private static long min(long... values) {
		long best = Long.MAX_VALUE;
		for (long value : values) {
			best = Math.min(best, value);
		}
		return best;
	}

	protected static final class LevelKey {
		public final int floorId;
		public final int levelId;

		public LevelKey(final int floorId, final int levelId) {
			this.floorId = floorId;
			this.levelId = levelId;
		}

		@Override
		public int hashCode() {
			return (31 * floorId) + levelId;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			} else if (!(obj instanceof LevelKey)) {
				return false;
			} else {
				final LevelKey key = (LevelKey) obj;
				return (floorId == key.floorId) && (levelId == key.levelId);
			}
		}
	}

}
