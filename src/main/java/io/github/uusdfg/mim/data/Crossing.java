package io.github.uusdfg.mim.data;

public final class Crossing extends Transition {

	public static final Range LEFT_ENTRY_RANGE = new Range(0, 14);

	public static final Range RIGHT_ENTRY_RANGE = new Range(-14, 0);

	// XXX This seems to swing between the 160s and 170s. I'm not sure why.
	// The given value seemed like a reasonable compromise.
	public static final long UP_NPC_TIME = 168;

	public static final long UP_TOTAL_TIME = UP_NPC_TIME + ROAD_LOAD_TIME;

	public static final long DOWN_NPC_TIME = 185;

	public static final long DOWN_TOTAL_TIME = DOWN_NPC_TIME + ROAD_LOAD_TIME;

	private final Range xRange;

	private final boolean left;

	private final boolean up;

	public Crossing(final Position srcPos, final Position destPos,
			final boolean left, final boolean up) {
		super(srcPos, destPos);

		this.left = left;
		this.up = up;

		if (left) {
			this.xRange = LEFT_ENTRY_RANGE.withOffset(srcPos.getX());
		} else {
			this.xRange = RIGHT_ENTRY_RANGE.withOffset(srcPos.getX());
		}
	}

	public boolean isLeft() {
		return left;
	}

	public boolean isUp() {
		return up;
	}

	@Override
	public boolean isActiveAt(final Position pos) {
		return getSrcPos().sharesRoadWith(pos) && xRange.includes(pos.getX());
	}

	@Override
	public Position getActivePosNearestTo(final Position pos) {
		if (!getSrcPos().sharesRoadWith(pos)) {
			return null;
		}

		final int x = pos.getX();
		if (xRange.isWhollyBelow(x)) {
			// Assume the player has to go left to get to a transition point to
			// the left...
			return new Position(true, pos.getRoadIndex(), xRange.getMax());
		} else if (xRange.isWhollyAbove(x)) {
			// Similar logic if the transition is to the right...
			return new Position(false, pos.getRoadIndex(), xRange.getMin());
		} else {
			return pos;
		}
	}

	@Override
	public long getTotalTime() {
		return (up) ? UP_TOTAL_TIME : DOWN_TOTAL_TIME;
	}

	@Override
	public long getNpcActivityTime() {
		return (up) ? UP_NPC_TIME : DOWN_NPC_TIME;
	}

	@Override
	public Position take(final Position pos) {
		if (isActiveAt(pos)) {
			// The game forces the direction in which the player is facing after
			// a crossing to match which side of the street the crossing is on.
			return getDestPos().withLeftFlag(left);
		} else {
			return null;
		}
	}

	@Override
	public String toString() {
		return String.format("%s %s crossing (%s -> %s)", (left) ? "Left"
				: "Right", (up) ? "up" : "down", getSrcPos(), getDestPos());
	}

}
