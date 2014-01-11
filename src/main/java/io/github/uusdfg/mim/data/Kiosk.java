package io.github.uusdfg.mim.data;

public final class Kiosk {

	private static final Range LEFT_ACTIVATION_RANGE = new Range(-54, -7);

	private static final Range RIGHT_ACTIVATION_RANGE = new Range(-101, -55);

	private final int artifactId;

	private final Position basePos;

	private final Range leftRange;

	private final Range rightRange;

	private final Level level;

	private boolean completed = false;

	public Kiosk(final int artifactId, final Position basePos, final Level level) {
		this.artifactId = artifactId;
		this.basePos = basePos;
		this.leftRange = LEFT_ACTIVATION_RANGE.withOffset(basePos.getX());
		this.rightRange = RIGHT_ACTIVATION_RANGE.withOffset(basePos.getX());
		this.level = level;
	}

	public Kiosk(final Kiosk src, final Level level) {
		this.artifactId = src.artifactId;
		this.basePos = src.basePos;
		this.completed = src.completed;
		this.leftRange = src.leftRange;
		this.rightRange = src.rightRange;
		this.level = level;
	}

	public int getArtifactId() {
		return artifactId;
	}

	public Position getBasePos() {
		return basePos;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(final boolean completed) {
		this.completed = completed;

		level.setKioskCompletionFlag(artifactId, completed);
	}

	public boolean isActiveAt(final Position pos) {
		if (!pos.sharesRoadWith(basePos)) {
			return false;
		}

		if (pos.isFacingLeft()) {
			return leftRange.includes(pos.getX());
		} else {
			return rightRange.includes(pos.getX());
		}
	}

	public Position getActivePosNearestTo(final Position pos) {
		// Because there are two activation ranges, we have a lot of cases to
		// handle...
		if (!pos.sharesRoadWith(basePos)) {
			// If the position isn't even on the same road, this is impossible.
			return null;
		}

		final int roadIndex = pos.getRoadIndex();
		final int x = pos.getX();

		// If the player isn't in the area of the kiosk at all, the player
		// has to run to it.
		final Range totalRange = leftRange.unionWith(rightRange);
		if (totalRange.isWhollyBelow(x)) {
			return new Position(true, roadIndex, totalRange.getMax());
		} else if (totalRange.isWhollyAbove(x)) {
			return new Position(false, roadIndex, totalRange.getMin());
		}

		// If the player is near the kiosk, the player might already be at the
		// kiosk, or might need to turn around.
		// XXX This doesn't account for the fact turning moves you a pixel.
		if (pos.isFacingLeft()) {
			if (leftRange.includes(x)) {
				return pos;
			} else {
				// XXX This assumes there is no gap between the ranges.
				// Fortunately, there is no gap.
				return pos.withLeftFlag(false);
			}
		} else {
			if (rightRange.includes(x)) {
				return pos;
			} else {
				return pos.withLeftFlag(true);
			}
		}
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof Kiosk)) {
			return false;
		} else {
			final Kiosk kiosk = (Kiosk) obj;

			return (artifactId == kiosk.artifactId);
		}
	}

	@Override
	public int hashCode() {
		return artifactId;
	}

	@Override
	public String toString() {
		return String.format("Kiosk %d%s @ %s", artifactId,
				(completed) ? " (completed)" : "", basePos);
	}

}
