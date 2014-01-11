package io.github.uusdfg.mim.data;

public final class Exit extends Transition {

	public static final long NPC_TIME = 0;

	public static final long TOTAL_TIME = 370;

	private static final Range ENTRY_RANGE = new Range(-3, 3);

	private final Range xRange;

	public Exit(final Position srcPos, final Position posInCastleRoad) {
		super(srcPos, posInCastleRoad);

		xRange = ENTRY_RANGE.withOffset(srcPos.getX());
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
			return new Position(true, pos.getRoadIndex(), xRange.getMax());
		} else if (xRange.isWhollyAbove(x)) {
			return new Position(false, pos.getRoadIndex(), xRange.getMin());
		} else {
			return pos;
		}
	}

	@Override
	public long getTotalTime() {
		return TOTAL_TIME;
	}

	@Override
	public long getNpcActivityTime() {
		return NPC_TIME;
	}

	@Override
	public Position take(final Position pos) {
		// Exits, like regular pipes, preserve the direction the player is
		// facing.
		return getDestPos().withLeftFlag(pos.isFacingLeft());
	}

	@Override
	public String toString() {
		return String.format("Exit (%s)", getSrcPos());
	}

}
