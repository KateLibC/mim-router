package io.github.uusdfg.mim.data;

public final class Position {

	private boolean facingLeft;

	private int x;

	private int roadIndex;

	public Position(final boolean facingLeft, final int roadIndex, final int x) {
		this.facingLeft = facingLeft;
		this.roadIndex = roadIndex;
		this.x = x;
	}

	public Position(final Position src) {
		this.facingLeft = src.facingLeft;
		this.x = src.x;
		this.roadIndex = src.roadIndex;
	}

	public boolean isFacingLeft() {
		return facingLeft;
	}

	public int getX() {
		return x;
	}

	public int getRoadIndex() {
		return roadIndex;
	}

	public Position withLeftFlag(final boolean leftFlag) {
		return new Position(leftFlag, roadIndex, x);
	}

	public Position withX(final int x) {
		return new Position(this.facingLeft, this.roadIndex, x);
	}

	public Position plusXOffset(final int offset) {
		return new Position(this.facingLeft, this.roadIndex, x + offset);
	}

	public boolean sharesRoadWith(final Position pos) {
		return (roadIndex == pos.roadIndex);
	}

	public boolean isLeftOf(final Position pos) {
		return (this.x < pos.x);
	}

	public int getXdistance(final Position pos) {
		return Math.abs(this.x - pos.x);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof Position)) {
			return false;
		} else {
			final Position pos = (Position) obj;

			return (facingLeft == pos.facingLeft) && (x == pos.x)
					&& (roadIndex == pos.roadIndex);
		}
	}

	@Override
	public int hashCode() {
		return 31 * ((31 * x) + roadIndex) + ((facingLeft) ? 31 : 0);
	}

	@Override
	public String toString() {
		return String.format("(#%d, %d (%s))", roadIndex, x,
				(facingLeft) ? "left" : "right");
	}
}
