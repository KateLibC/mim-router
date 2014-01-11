package io.github.uusdfg.mim.rng;

public final class SlotRng {

	public static final int DEFAULT_LOW = 4506;

	public static final int DEFAULT_HIGH = 3716;

	private int low;

	private int high;

	public SlotRng() {
		this(DEFAULT_LOW, DEFAULT_HIGH);
	}

	public SlotRng(final int low, final int high) {
		this.low = low;
		this.high = high;
	}

	public SlotRng(final SlotRng src) {
		this.low = src.low;
		this.high = src.high;
	}

	public int getLow() {
		return low;
	}

	public int getHigh() {
		return high;
	}

	public SlotRng reseed(final int low, final int high) {
		this.low = low;
		this.high = high;
		return this;
	}

	public int getSlot(final int numNpcSlots) {
		final int mask = 0xffff >> (16 - next2exp(numNpcSlots));
		return (low & mask);
	}

	public SlotRng advance() {
		high = high << 1;
		final boolean highCarry = (high >= 0x10000);
		high = high & 0xffff;

		low = (low << 1) + ((highCarry) ? 1 : 0);
		final boolean lowCarry = (low >= 0x10000);
		low = low & 0xffff;

		if (lowCarry) {
			high = high ^ 0xb400;
		}

		return this;
	}

	public SlotRng advanceAbsolute(final int numIterations) {
		for (int i = 0; i < numIterations; i++) {
			advance();
		}
		return this;
	}

	public SlotRng advanceInLevel(final int numNpcSlots) {
		// Before choosing a slot, the game advances the RNG a number of times
		// that depends on the number of NPC slots in the level.
		final int numIterations = next2exp(numNpcSlots);

		// The game also randomly generates a slot number until it gets a valid
		// one. The algorithm for producing the slot number sometimes overshoots
		// and the game compensates by redoing it a lot.
		int slot;
		do {
			advanceAbsolute(numIterations);
			slot = getSlot(numNpcSlots);
		} while (slot >= numNpcSlots);

		return this;
	}

	@Override
	public int hashCode() {
		return (31 * low) + high;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof SlotRng)) {
			return false;
		} else {
			final SlotRng rng = (SlotRng) obj;

			return (low == rng.low) && (high == rng.high);
		}
	}

	@Override
	public String toString() {
		return String.format("RNG %d-%d", low, high);
	}

	protected int next2exp(final int val) {
		// TODO Less disgusting approach.
		return (int) (Math.log(val) / Math.log(2)) + 1;
	}

}
