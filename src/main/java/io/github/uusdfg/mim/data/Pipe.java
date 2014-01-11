package io.github.uusdfg.mim.data;

public final class Pipe extends Transition {

	public static final Range ENTRY_RANGE = new Range(-3, 3);

	public static final long NPC_TIME = 80;

	public static final long TOTAL_TIME = NPC_TIME + ROAD_LOAD_TIME;

	private final Range xRange;

	public Pipe(final Position srcPos, final Position destPos) {
		super(srcPos, destPos);

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
		// Pipes preserve the direction the player is facing.
		return getDestPos().withLeftFlag(pos.isFacingLeft());
	}

	@Override
	public String toString() {
		return String.format("Pipe (%s -> %s)", getSrcPos(), getDestPos());
	}

}
