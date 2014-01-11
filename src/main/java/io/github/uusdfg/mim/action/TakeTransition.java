package io.github.uusdfg.mim.action;

import io.github.uusdfg.mim.action.Route.HistoryEntry;
import io.github.uusdfg.mim.data.Entrance;
import io.github.uusdfg.mim.data.Exit;
import io.github.uusdfg.mim.data.Floor;
import io.github.uusdfg.mim.data.Game;
import io.github.uusdfg.mim.data.Level;
import io.github.uusdfg.mim.data.Player;
import io.github.uusdfg.mim.data.Position;
import io.github.uusdfg.mim.data.Road;
import io.github.uusdfg.mim.data.Transition;

import java.util.List;

public class TakeTransition implements Action {

	private final int roadId;

	private final int transitionIndex;

	public TakeTransition(final int roadId, final int transitionIndex) {
		this.roadId = roadId;
		this.transitionIndex = transitionIndex;
	}

	public final int getRoadId() {
		return roadId;
	}

	public final int getTransitionIndex() {
		return transitionIndex;
	}

	public boolean checkIfPossible(final Game game) {
		// As an early sanity check, it's completely impossible if the
		// transition doesn't exist in the current level or if the player's not
		// in a level at all.
		final Transition transition = getTransition(game);
		if (transition == null) {
			return false;
		}

		// It's only possible for the player to take the transition if the
		// player is on the same road. Otherwise, the player needs to take some
		// other transition first.
		final Player player = game.getPlayer();
		final Position playerPos = player.getPos();
		boolean possible = (transition.getSrcPos().getRoadIndex() == playerPos
				.getRoadIndex());

		if (transition instanceof Exit) {
			// Additionally, the player can only exit a level if the player has
			// Yoshi or if the player just entered and is still on the pipe.
			// TODO Add something to actually check when Pokey comes up instead
			// of assuming that the player won't ever be on the pipe without
			// Yoshi.
			possible = possible
					&& (player.hasYoshi() || transition.isActiveAt(playerPos));
		} else if (transition instanceof Entrance) {
			// Also, the player can only enter a level that hasn't already been
			// completed, and the player can only exit the floor when all of the
			// levels have been completed.
			final Entrance entrance = (Entrance) transition;
			final Floor floor = game.getFloor(player.getFloor());
			if (entrance.isFloorExit()) {
				possible = possible && floor.isCompleted();
			} else {
				possible = possible
						&& !floor.getLevel(entrance.getLevelId()).isCompleted();
			}
		}
		return possible;
	}

	public long getTimeRequired(final Game game, final Route pastActions) {
		// The normal time to take a transition is the time spent traveling to
		// the transition plus however much the game makes us wait on
		// animations/loading. If we recently stomped a Koopa, however, we might
		// have to wait for the stomp animation to finish before we start the
		// transition animation.
		final long minWalkTime = game.getPlayer().getTransitionDelayEnd()
				- pastActions.getTotalTime();
		final long walkTime = Math.max(minWalkTime, getTimeToReach(game));

		final Transition transition = getTransition(game);
		return transition.getTotalTime() + walkTime;
	}

	public long getTimeToReach(final Game game) {
		final Transition transition = getTransition(game);
		final Player player = game.getPlayer();
		final Position playerPos = player.getPos();
		final Position tPos = transition.getActivePosNearestTo(playerPos);
		return player.getTimeToReach(tPos);
	}

	public long perform(final Game game, final Route pastActions) {
		final Transition transition = getTransition(game);
		final long timeToReach = getTimeToReach(game);
		final long minPreTransitionTime = game.getPlayer()
				.getTransitionDelayEnd() - pastActions.getTotalTime();
		final long preTransitionTime = Math.max(timeToReach,
				minPreTransitionTime);
		final long totalTime = preTransitionTime + transition.getTotalTime();

		final Player player = game.getPlayer();
		final Position playerPos = player.getPos();
		final Position srcPos = transition.getSrcPos();
		final Position destPos = transition.getDestPos();

		Position newPos;
		if (transition instanceof Entrance) {
			final Entrance entrance = (Entrance) transition;
			if (entrance.isFloorExit()) {
				// If the player's leaving the floor, the position in the
				// transition is probably junk. In addition, we need to
				// increment the player's floor counter.
				final int nextFloorId = player.getFloor() + 1;
				player.setFloor(nextFloorId);
				player.setLevel(null);
				newPos = game.getFloor(nextFloorId).getStartPos();
			} else {
				// If the player is actually entering a level, we need to get
				// the player set up to be at the start of that level.
				final Position postRunPos = player.run(
						srcPos.isLeftOf(playerPos), timeToReach);
				final Level level = game.getLevel(player.getFloor(),
						entrance.getLevelId());
				player.setLevel(level);
				newPos = transition.take(postRunPos);

				// We also need to get the level set up. Everything resets when
				// the player enters a level, even if the player had done most
				// of the level the last time in.
				level.respawn(game.getSlotRng());

				// Also, the NPCs get a little time to advance before the player
				// can do anything. The time the player takes to walk to the
				// entrance obviously doesn't count, though.
				level.advanceNpcs(transition.getNpcActivityTime());
			}
		} else if (destPos.getRoadIndex() == Road.CASTLE_ROAD_ID) {
			// In this case, the player is exiting the level, so the level gets
			// nulled and Yoshi goes away. The transition should have the exit
			// spot in the castle road.
			final Position postRunPos = player.run(srcPos.isLeftOf(playerPos),
					timeToReach);
			newPos = transition.take(postRunPos);

			final Level level = player.getLevel();
			player.setLevel(null);
			player.setYoshi(false);

			// If the player collected everything in the level before leaving,
			// we can mark it as completed.
			level.setCompleted(level.areAllArtifactsReturned());
		} else {
			// The normal case: the player goes from one place within a level to
			// another. In this case, the player's position changes...
			final Position postRunPos = player.run(srcPos.isLeftOf(playerPos),
					timeToReach);
			newPos = transition.take(postRunPos);

			// ...and the NPCs all advance.
			player.getLevel().advanceNpcs(
					preTransitionTime + transition.getNpcActivityTime());
		}

		player.setPos(newPos);
		return totalTime;
	}

	@Override
	public String toString() {
		return String.format("Take transition %d in road %d", transitionIndex,
				roadId);
	}

	public String toDetailedString(final Game previousState,
			final HistoryEntry historyEntry) {
		final Transition transition = getTransition(previousState);
		final long typicalTime = transition.getTotalTime()
				+ getTimeToReach(previousState);
		final long extraTime = historyEntry.time - typicalTime;

		if (extraTime > 0) {
			return String.format("Wait %d and take %s", extraTime, transition);
		} else {
			return String.format("Take %s", transition);
		}
	}

	public final Transition getTransition(final Game game) {
		final Player player = game.getPlayer();

		final Level level = player.getLevel();
		final Road road;
		if (level == null) {
			final Floor floor = game.getFloor(player.getFloor());
			if (floor == null) {
				road = null;
			} else {
				road = floor.getCastleRoad();
			}
		} else {
			road = level.getRoad(roadId);
		}

		if (road == null) {
			return null;
		}

		final List<Transition> transitions = road.getTransitions();
		if (transitionIndex >= transitions.size()) {
			return null;
		}

		return transitions.get(transitionIndex);
	}
}
