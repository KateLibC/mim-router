package io.github.uusdfg.mim.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Road {

	public static final int CASTLE_ROAD_ID = 1234;

	private final Level containingLevel;

	private final int roadId;

	private final int length;

	private final List<Transition> transitions = new ArrayList<>();

	private final List<Transition> reverseTransitions = new ArrayList<>();

	public Road(final Level containingLevel, final int roadId, final int length) {
		this.containingLevel = containingLevel;
		this.roadId = roadId;
		this.length = length;
	}

	public Level getContainingLevel() {
		return containingLevel;
	}

	public int getRoadId() {
		return roadId;
	}

	public long getLength() {
		return length;
	}

	public boolean isCastle() {
		return (roadId == CASTLE_ROAD_ID);
	}

	public boolean isYoshiAvailable() {
		return !isCastle();
	}

	public List<Transition> getTransitions() {
		return Collections.unmodifiableList(transitions);
	}

	public List<Transition> getReverseTransitions() {
		return Collections.unmodifiableList(reverseTransitions);
	}

	// TODO Add stuff to Level to double-check that the reverse transitions are
	// complete.
	public void addTransition(final Transition transition) {
		transitions.add(transition);

		if (containingLevel != null) {
			final Road destRoad = containingLevel.getRoad(transition
					.getDestPos().getRoadIndex());
			if (destRoad != null) {
				destRoad.reverseTransitions.add(transition);
			}
		}
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof Road)) {
			return false;
		} else {
			final Road road = (Road) obj;

			return (roadId == road.roadId);
		}
	}

	@Override
	public int hashCode() {
		return roadId;
	}

	@Override
	public String toString() {
		return String.format("Road %d", roadId);
	}

}
