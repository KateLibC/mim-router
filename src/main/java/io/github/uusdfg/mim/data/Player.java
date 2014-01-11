package io.github.uusdfg.mim.data;

public class Player {

	private static final int LEVEL_RUN_SPEED = 3;

	private static final int CASTLE_RUN_SPEED = 2;

	private static final int WALK_SPEED = 1;

	private static final int YOSHI_SPEED_BONUS = 1;

	private static final int LUIGI_X_MARGIN = 18;

	private int floor;

	private Position pos;

	private Level level;

	private boolean yoshi;

	private int currentArtifact;

	private long transitionDelayEnd;

	public Player() {
		floor = 0;
		pos = new Position(false, 0, 0);
		level = null;
		yoshi = false;
	}

	public Player(final Player player, final Game cloneGame) {
		this.floor = player.floor;
		this.pos = player.pos;
		this.yoshi = player.yoshi;
		this.currentArtifact = player.currentArtifact;
		this.transitionDelayEnd = player.transitionDelayEnd;

		final Level srcLevel = player.getLevel();
		if (srcLevel == null) {
			this.level = null;
		} else {
			this.level = cloneGame.getLevel(player.getFloor(),
					srcLevel.getLevelId());
		}
	}

	public final int getFloor() {
		return floor;
	}

	public final Position getPos() {
		return pos;
	}

	public final boolean isInCastle() {
		return (level == null);
	}

	public final Level getLevel() {
		return level;
	}

	// XXX I can't do anything with the castle road because I don't track a
	// reference to the actual floor. Stupid. Oh, well.
	public final Road getRoad() {
		Road road = null;
		if (level != null) {
			road = level.getRoad(pos.getRoadIndex());
		}
		return road;
	}

	public final boolean hasYoshi() {
		return yoshi;
	}

	public final int getCurrentArtifact() {
		return currentArtifact;
	}

	public final long getTransitionDelayEnd() {
		return transitionDelayEnd;
	}

	public final void setFloor(final int floor) {
		this.floor = floor;
	}

	public final void setPos(final Position pos) {
		if (pos == null) {
			throw new IllegalArgumentException(
					"Tried to give player null position");
		}

		this.pos = pos;
		final int roadIndex = pos.getRoadIndex();
		if ((this.level != null) && (roadIndex != Road.CASTLE_ROAD_ID)) {
			this.level.setPlayerRoadFlag(roadIndex);
		}
	}

	public final void setLevel(final Level level) {
		this.level = level;
	}

	public final void setYoshi(final boolean yoshi) {
		this.yoshi = yoshi;
		if (level != null) {
			level.setYoshiFlag(yoshi);
		}
	}

	public final void setCurrentArtifact(final int artifactId) {
		this.currentArtifact = artifactId;
		if (level != null) {
			level.setHeldArtifactFlag(artifactId);
		}
	}

	public final void setTransitionDelayEnd(final long transitionDelayEnd) {
		this.transitionDelayEnd = transitionDelayEnd;
	}

	public final void setTransitionDelayEndSafe(final long transitionDelayEnd) {
		this.transitionDelayEnd = Math.max(this.transitionDelayEnd,
				transitionDelayEnd);
	}

	public final int getWalkSpeed() {
		return WALK_SPEED + ((hasYoshi()) ? YOSHI_SPEED_BONUS : 0);
	}

	public final int getRunSpeed() {
		int speed;
		if (isInCastle()) {
			speed = CASTLE_RUN_SPEED;
		} else {
			speed = LEVEL_RUN_SPEED;
		}
		return speed + ((hasYoshi()) ? YOSHI_SPEED_BONUS : 0);
	}

	public final long getTimeToReach(final Position destPos) {
		return (long) Math.ceil(((double) pos.getXdistance(destPos))
				/ getRunSpeed());
	}

	public final Position walk(final boolean left, final long time) {
		return move(getRunSpeed(), left, time);
	}

	public final Position run(final boolean left, final long time) {
		return move(getRunSpeed(), left, time);
	}

	public final Position move(final int speed, final boolean left,
			final long time) {
		// Advance the player the given number of frames in the given direction.
		final int velocity = speed * ((left) ? -1 : 1);
		pos = pos.plusXOffset((int) (velocity * time));

		// Bound the player according to the road the player is on. It is
		// possible for us to get, e.g., Koopa-bounced past the edge of the
		// screen given our current algorithms.
		// XXX Can't do this if we're on the castle road. Oh, well. It shouldn't
		// come up in that case.
		final Road road = getRoad();
		if (road != null) {
			// The game forces there to be a bit of space between Luigi and the
			// edges of the road. Account for that space.
			// XXX I think it might technically be possible for Luigi to fail to
			// stomp on a Koopa if the Koopa gets to the _very_ edge of the
			// screen. I hope that doesn't come up...
			final int x = pos.getX();
			final int rightBound = (int) road.getLength() - LUIGI_X_MARGIN;
			if (x < LUIGI_X_MARGIN) {
				pos = pos.withX(LUIGI_X_MARGIN);
			} else if (x > rightBound) {
				pos = pos.withX(rightBound);
			}
		}

		return pos;
	}

	@Override
	public int hashCode() {
		int hash = floor;
		hash = (hash * 37) + level.hashCode();
		hash = (hash * 37) + pos.hashCode();
		hash = (hash * 37) + ((yoshi) ? 31 : 0);
		return hash;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof Player)) {
			return false;
		} else {
			final Player player = (Player) obj;

			return (floor == player.floor) && (level == player.level)
					&& (pos.equals(player.pos)) && (yoshi == player.yoshi);
		}
	}

	@Override
	public String toString() {
		final StringBuilder str = new StringBuilder();
		str.append("Luigi in ");

		if (isInCastle()) {
			str.append("castle ").append(floor);
		} else {
			str.append(floor).append("-").append(level);
		}

		str.append(" @ ").append(pos);
		str.append(" holding ").append(currentArtifact);

		if (yoshi) {
			str.append(" w/Yoshi");
		}

		return str.toString();
	}

}
