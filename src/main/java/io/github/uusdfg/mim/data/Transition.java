package io.github.uusdfg.mim.data;

public abstract class Transition {

	// XXX Technically, this should be set per-road, but this seems like a
	// reasonable overall estimate based on what I've seen on console.
	public static final int ROAD_LOAD_TIME = 34;

	private final Position srcPos;

	private final Position destPos;

	public final Position getSrcPos() {
		return srcPos;
	}

	public final Position getDestPos() {
		return destPos;
	}

	public abstract boolean isActiveAt(final Position pos);

	public abstract Position getActivePosNearestTo(final Position pos);

	public abstract long getTotalTime();

	public abstract long getNpcActivityTime();

	public abstract Position take(final Position pos);

	protected Transition(final Position srcPos, final Position destPos) {
		this.srcPos = srcPos;
		this.destPos = destPos;
	}

}
