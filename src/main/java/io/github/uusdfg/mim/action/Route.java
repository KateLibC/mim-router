package io.github.uusdfg.mim.action;

import io.github.uusdfg.mim.data.Game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Route {

	private final Game initialState;

	private Game finalState;

	private List<HistoryEntry> history;

	private long totalTime;

	// TODO Add a counter for the number of levels that have been exited
	// immediately after entry. That will allow us to limit how often we do
	// that, which should hopefully make it possible to try that without a huge
	// explosion in the number of solutions we try.

	public Route(final Game initialState) {
		this.initialState = initialState;
		this.finalState = initialState;
		this.totalTime = 0;
		this.history = new ArrayList<>();
	}

	public Route(final Route src) {
		this.initialState = src.initialState;
		this.finalState = new Game(src.finalState);
		this.totalTime = src.totalTime;
		this.history = new ArrayList<>(src.history);
	}

	public Game getInitialState() {
		return initialState;
	}

	public Game getFinalState() {
		return finalState;
	}

	public List<HistoryEntry> getHistory() {
		return Collections.unmodifiableList(history);
	}

	public Action getLastAction() {
		if (history.isEmpty()) {
			return null;
		}
		return history.get(history.size() - 1).action;
	}

	public final long getTotalTime() {
		return totalTime;
	}

	public void performAction(final Action action) {
		final long actionTime = action.perform(finalState, this);
		totalTime += actionTime;
		history.add(new HistoryEntry(action, actionTime, totalTime, finalState,
				this));
	}

	@Override
	public String toString() {
		final StringBuilder str = new StringBuilder();
		str.append("Route (").append(totalTime).append("):\n");

		Game previousState = initialState;
		for (int i = 0; i < history.size(); i++) {
			str.append("  ").append(i).append(":  ");

			final HistoryEntry entry = history.get(i);
			str.append(entry.toDetailedString(previousState)).append("\n");

			previousState = entry.state;
		}
		return str.toString();
	}

	public static final class HistoryEntry {
		public Action action;
		public long time;
		public long cumulativeTime;
		public Game state;
		public Route backReference;

		public HistoryEntry() {
		}

		public HistoryEntry(final Action action, final long time,
				final long cumulativeTime, final Game state,
				final Route backReference) {
			this.action = action;
			this.time = time;
			this.cumulativeTime = cumulativeTime;
			this.state = state;
			this.backReference = backReference;
		}

		public HistoryEntry(final HistoryEntry src) {
			this.action = src.action;
			this.time = src.time;
			this.cumulativeTime = src.cumulativeTime;
			this.state = src.state;
			this.backReference = src.backReference;
		}

		@Override
		public String toString() {
			return String.format("%s (%d - %d)", action, time, cumulativeTime);
		}

		public String toDetailedString(final Game previousState) {
			return String.format("%s (%d - %d)",
					action.toDetailedString(previousState, this), time,
					cumulativeTime);
		}
	}

}
