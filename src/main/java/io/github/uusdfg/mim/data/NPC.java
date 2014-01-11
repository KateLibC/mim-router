package io.github.uusdfg.mim.data;

public abstract class NPC {

	private final int id;

	private Road road;

	private Position startPos;

	private Position currentPos;

	public final int getId() {
		return id;
	}

	public final Road getRoad() {
		return road;
	}

	public final Position getStartPos() {
		return startPos;
	}

	public final Position getCurrentPos() {
		return currentPos;
	}

	public final void setRoad(final Road road) {
		this.road = road;
	}

	public final void setStartPos(final Position startPos) {
		this.startPos = startPos;
	}

	public final void setCurrentPos(final Position currentPos) {
		this.currentPos = currentPos;
	}

	public abstract boolean isInHitbox(final Position pos);

	public abstract void advance(final long time);

	protected NPC(final int id) {
		this(id, new Position(false, 0, 0));
	}

	protected NPC(final int id, final Position startPos) {
		this.id = id;
		this.startPos = startPos;
		this.currentPos = startPos;
	}

	protected NPC(final NPC npc) {
		this.id = npc.id;
		this.startPos = npc.startPos;
		this.currentPos = npc.currentPos;
		this.road = npc.road;
	}

}
