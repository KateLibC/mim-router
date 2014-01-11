package io.github.uusdfg.mim.data;

public final class Entrance extends Transition {

	public static final int FLOOR_EXIT_LEVEL_ID = 5555;

	private static final Range ENTRY_RANGE = new Range(-3, 3);

	private static final long NPC_TIME = 80;

	private final int levelId;

	private final long loadTime;

	private final Range xRange;

	public Entrance(final Position srcPos, final Position destPos,
			final int levelId, final long loadTime) {
		super(srcPos, destPos);
		this.levelId = levelId;
		this.loadTime = loadTime;

		xRange = ENTRY_RANGE.withOffset(srcPos.getX());
	}

	public int getLevelId() {
		return levelId;
	}

	public boolean isFloorExit() {
		return (levelId == FLOOR_EXIT_LEVEL_ID);
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
		return (NPC_TIME + loadTime);
	}

	@Override
	public long getNpcActivityTime() {
		return NPC_TIME;
	}

	@Override
	public Position take(final Position pos) {
		return getDestPos().withLeftFlag(false);
	}

	@Override
	public String toString() {
		return String.format("Entrance (%s --> %s)", getSrcPos(),
				(isFloorExit()) ? "Next Floor" : levelId);
	}
}
