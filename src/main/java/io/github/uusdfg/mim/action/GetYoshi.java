package io.github.uusdfg.mim.action;

import io.github.uusdfg.mim.action.Route.HistoryEntry;
import io.github.uusdfg.mim.data.Game;
import io.github.uusdfg.mim.data.Level;
import io.github.uusdfg.mim.data.Player;
import io.github.uusdfg.mim.data.Road;

public class GetYoshi implements Action {

	// TODO Real, per-stage estimates.
	public static final long GLOBULATOR_TIME = 600;

	public boolean checkIfPossible(final Game game) {
		final Player player = game.getPlayer();
		final Level level = player.getLevel();
		final Road road;
		if (level == null) {
			road = game.getFloor(player.getFloor()).getCastleRoad();
		} else {
			road = level.getRoad(player.getPos().getRoadIndex());
		}

		// It's only possible to get Yoshi if Yoshi is allowed on the current
		// road and the player doesn't already have him.
		return (road.isYoshiAvailable() && !player.hasYoshi());
	}

	public long getTimeRequired(final Game game, final Route pastActions) {
		return GLOBULATOR_TIME;
	}

	public long perform(final Game game, final Route pastActions) {
		game.getPlayer().setYoshi(true);
		return getTimeRequired(game, pastActions);
	}

	@Override
	public String toString() {
		return "Get Yoshi";
	}

	public String toDetailedString(final Game previousState,
			final HistoryEntry historyEntry) {
		return String.format("Get Yoshi at %s", previousState.getPlayer()
				.getPos());
	}

}
