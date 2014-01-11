package io.github.uusdfg.mim.action;

import io.github.uusdfg.mim.action.Route.HistoryEntry;
import io.github.uusdfg.mim.data.Game;

public interface Action {

	public boolean checkIfPossible(final Game game);

	public long getTimeRequired(final Game game, final Route pastActions);

	public long perform(final Game game, final Route pastActions);

	public String toDetailedString(final Game previousState,
			final HistoryEntry historyEntry);

}
