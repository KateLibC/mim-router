package io.github.uusdfg.mim.data;

public final class Range {

	private final int min;

	private final int max;

	public Range(final int min, final int max) {
		if (min > max) {
			throw new IllegalArgumentException(String.format(
					"Range minimum %d exceeds maximum %d", min, max));
		}

		this.min = min;
		this.max = max;
	}

	public int getMin() {
		return min;
	}

	public int getMax() {
		return max;
	}

	public boolean includes(final int val) {
		return (val >= min) && (val <= max);
	}

	public boolean isWhollyBelow(final int val) {
		return (val > max);
	}

	public boolean isWhollyAbove(final int val) {
		return (val < min);
	}

	public Range withMin(final int min) {
		return new Range(min, this.max);
	}

	public Range withMax(final int max) {
		return new Range(this.min, max);
	}

	public Range withOffset(final int offset) {
		return new Range(min + offset, max + offset);
	}

	public Range unionWith(final Range range) {
		return new Range(Math.min(min, range.min), Math.max(max, range.max));
	}

	public Range intersectionWith(final Range range) {
		final int newMin = Math.max(min, range.min);
		final int newMax = Math.min(max, range.max);
		if (newMin > newMax) {
			return null;
		} else {
			return new Range(newMin, newMax);
		}
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof Range)) {
			return false;
		} else {
			final Range range = (Range) obj;

			return (min == range.min) && (max == range.max);
		}
	}

	@Override
	public int hashCode() {
		return (31 * min) + max;
	}

	@Override
	public String toString() {
		return String.format("[%d, %d]", min, max);
	}

}
