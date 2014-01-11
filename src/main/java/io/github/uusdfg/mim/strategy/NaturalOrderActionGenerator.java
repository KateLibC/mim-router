package io.github.uusdfg.mim.strategy;

import io.github.uusdfg.mim.action.Action;
import io.github.uusdfg.mim.action.TakeTransition;
import io.github.uusdfg.mim.data.Entrance;
import io.github.uusdfg.mim.data.Floor;
import io.github.uusdfg.mim.data.Game;
import io.github.uusdfg.mim.data.Level;
import io.github.uusdfg.mim.data.Road;
import io.github.uusdfg.mim.data.Transition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// XXX lol encapsulation
public class NaturalOrderActionGenerator extends DefaultActionGenerator {

	@Override
	protected Collection<Action> getPossibleActionsOutsideLevel(final Game game) {
		final Floor floor = game.getFloor(game.getPlayer().getFloor());
		final Map<Integer, Action> entranceMap = getLevelEntrances(floor);
		final int numLevels = floor.getLevels().size();

		boolean allDun = true;
		final List<Action> actions = new ArrayList<>();
		if (isLevelCompleted(floor, 1)) {
			for (int i = 2; i <= numLevels; i++) {
				if (!isLevelCompleted(floor, i)) {
					allDun = false;
					actions.add(entranceMap.get(i));
					break;
				}
			}
		} else if (isLevelCompleted(floor, numLevels)) {
			for (int i = numLevels - 1; i >= 1; i--) {
				if (!isLevelCompleted(floor, i)) {
					allDun = false;
					actions.add(entranceMap.get(i));
					break;
				}
			}
		} else {
			allDun = false;
			actions.add(entranceMap.get(1));
			actions.add(entranceMap.get(numLevels));
		}

		if (allDun) {
			actions.add(getFloorExit(floor));
		}

		return actions;
	}

	protected Map<Integer, Action> getLevelEntrances(final Floor floor) {
		final Map<Integer, Action> entranceMap = new HashMap<>();
		final Road castleRoad = floor.getCastleRoad();
		final List<Transition> transitions = castleRoad.getTransitions();
		for (Level level : floor.getLevels()) {
			final int levelId = level.getLevelId();
			for (int i = 0; i < transitions.size(); i++) {
				final Transition transition = transitions.get(i);
				if (transition instanceof Entrance) {
					final Entrance entrance = (Entrance) transition;
					if (entrance.getLevelId() == levelId) {
						entranceMap.put(levelId,
								new TakeTransition(castleRoad.getRoadId(), i));
					}
				}
			}
		}
		return entranceMap;
	}

	protected Action getFloorExit(final Floor floor) {
		int floorExitTransitionIndex = 0;
		int floorExitRoadIndex = 0;
		final List<Transition> transitions = floor.getCastleRoad()
				.getTransitions();
		for (int i = 0; i < transitions.size(); i++) {
			final Transition transition = transitions.get(i);
			if (transition instanceof Entrance) {
				final Entrance entrance = (Entrance) transition;
				if (entrance.getLevelId() == Entrance.FLOOR_EXIT_LEVEL_ID) {
					floorExitTransitionIndex = i;
					floorExitRoadIndex = entrance.getDestPos().getRoadIndex();
					break;
				}
			}
		}
		return new TakeTransition(floorExitRoadIndex, floorExitTransitionIndex);
	}

	protected boolean isLevelCompleted(final Floor floor, final int index) {
		return floor.getLevel(index).isCompleted();
	}
}
