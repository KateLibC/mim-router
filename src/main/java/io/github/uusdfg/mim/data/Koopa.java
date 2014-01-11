package io.github.uusdfg.mim.data;

public final class Koopa extends NPC {

	public static final int NO_ARTIFACT = -1;

	private static final Range HITBOX = new Range(-16, 16);

	private static final int MOVEMENT_SPEED = 1;

	private static final int LEFT_X_MARGIN = 15;

	private final Level level;

	private int artifactNumber = NO_ARTIFACT;

	private int animationTime;

	private Status status = Status.ACTIVE;

	public Koopa(final int id, final Level level) {
		super(id);
		this.level = level;
	}

	public Koopa(final int id, final Position startPos, final Level level) {
		super(id, startPos);
		this.level = level;
	}

	public Koopa(final Koopa src, final Level level) {
		super(src);
		this.artifactNumber = src.artifactNumber;
		this.animationTime = src.animationTime;
		this.status = src.status;
		this.level = level;

		setCurrentPos(src.getCurrentPos());
	}

	public Range getHitbox() {
		return HITBOX.withOffset(getCurrentPos().getX());
	}

	public boolean hasArtifact() {
		return (artifactNumber != NO_ARTIFACT);
	}

	public int getArtifactNumber() {
		return artifactNumber;
	}

	public Status getStatus() {
		return status;
	}

	public int getAnimationTime() {
		return animationTime;
	}

	public void setArtifactNumber(final int artifactNumber) {
		this.artifactNumber = artifactNumber;
	}

	public void setStatus(final Status status) {
		this.status = status;

		if (hasArtifact()) {
			level.setKoopaCompletionFlag(artifactNumber, status);
		}
	}

	public void setAnimationTime(final int animationTime) {
		this.animationTime = animationTime;
	}

	@Override
	public boolean isInHitbox(final Position pos) {
		return isInHitbox(pos, this);
	}

	@Override
	public void advance(final long time) {
		if (status != Status.ACTIVE) {
			// Inactive Koopas don't move.
			return;
		}

		final Position fromPos = getCurrentPos();
		boolean left = fromPos.isFacingLeft();
		long x = fromPos.getX();
		x += ((left) ? -1 : 1) * time * MOVEMENT_SPEED;
		final long roadLength = getRoad().getLength();

		// Have the Koopa turn around when it hits the end of the road. Note
		// that the Koopa is supposed to pause a frame when it turns around.
		// Also note that the edges are weird: there's a space on the left side,
		// and the Koopa goes all the way up to the theoretical width of the
		// road on the right.
		// TODO Less lame algorithm. You can make it O(1).
		while ((x < LEFT_X_MARGIN) || (x > roadLength)) {
			if (x < LEFT_X_MARGIN) {
				x = LEFT_X_MARGIN + (LEFT_X_MARGIN - x - 1);
				left = false;
			} else {
				x = roadLength + 1 - (x - roadLength);
				left = true;
			}
		}

		final Position toPos = fromPos.withLeftFlag(left).withX((int) x);
		setCurrentPos(toPos);
	}

	public long getTimeToHitboxEntry(final Position playerPos,
			final int playerSpeed) {
		return getTimeToHitboxEntry(playerPos, playerSpeed, this);
	}

	public boolean sameState(final Koopa koopa) {
		return (status == koopa.status)
				&& (getCurrentPos().equals(koopa.getCurrentPos()));
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof Koopa)) {
			return false;
		} else {
			final Koopa koopa = (Koopa) obj;

			return (getId() == koopa.getId());
		}
	}

	@Override
	public int hashCode() {
		return (31 * getId());
	}

	@Override
	public String toString() {
		final StringBuilder str = new StringBuilder();
		str.append("Koopa ").append(getId()).append(" (");
		str.append(status);

		if (hasArtifact()) {
			str.append(", artifact #").append(artifactNumber);
		}

		str.append(" @ ").append(getCurrentPos()).append(")");

		return str.toString();
	}

	// XXX This is ugly :/
	public Range getBounceHitbox() {
		return HITBOX.withOffset(getBouncePosition().getX());
	}

	public Range getDoubleBounceHitbox() {
		return HITBOX.withOffset(getDoubleBouncePosition().getX());
	}

	// XXX This is also ugly. :s
	public Position getBouncePosition() {
		final Position pos = getCurrentPos();
		final int x = pos.getX();
		final int roadIndex = pos.getRoadIndex();
		if (pos.isFacingLeft()) {
			return new Position(false, roadIndex, LEFT_X_MARGIN
					+ (LEFT_X_MARGIN - x - 1));
		} else {
			final long roadLength = getRoad().getLength();
			return new Position(true, roadIndex,
					(int) (roadLength + 1 + (roadLength - x)));
		}
	}

	public Position getDoubleBouncePosition() {
		final Position pos = getCurrentPos();
		final int x = pos.getX();
		final int roadIndex = pos.getRoadIndex();
		final int roadLength = (int) getRoad().getLength();

		if (pos.isFacingLeft()) {
			return new Position(true, roadIndex, x + roadLength + 2);
		} else {
			return new Position(false, roadIndex, x - roadLength - 2);
		}
	}

	public long getTimeToBounce() {
		final Position pos = getCurrentPos();
		final int x = pos.getX();
		if (pos.isFacingLeft()) {
			return (long) Math.ceil((double) (x + 1 - LEFT_X_MARGIN)
					/ MOVEMENT_SPEED);
		} else {
			final long roadLength = getRoad().getLength();
			return (long) Math.ceil((double) (roadLength + 1 - x)
					/ MOVEMENT_SPEED);
		}
	}

	// TODO I probably should have a hitbox class.
	public static long getTimeToHitboxEntry(final Position playerPos,
			final int playerSpeed, final Koopa koopa) {
		// Sanity check.
		final Position koopaPos = koopa.getCurrentPos();
		if (isInHitbox(playerPos, koopa)) {
			return 0;
		}

		final long roadLength = koopa.getRoad().getLength();
		final long playerX = playerPos.getX();
		final long koopaX = koopaPos.getX();
		final boolean koopaLeft = koopaPos.isFacingLeft();
		final boolean playerLeft = (playerX > koopaX);

		// Note that there are some -1/+1s thrown into the bounce X to account
		// for the fact that the Koopa pauses when it hits the edge of the road.
		final Range hitbox = koopa.getHitbox();
		final long hitboxX = (playerLeft) ? hitbox.getMax() : hitbox.getMin();
		final long bounceHitboxX = ((koopaLeft) ? (2 * LEFT_X_MARGIN - koopaX - 1)
				: (2 * roadLength - koopaX + 1))
				+ ((playerLeft) ? HITBOX.getMax() : HITBOX.getMin());

		final int koopaV = ((koopaLeft) ? -1 : 1)
				* ((koopa.getStatus() == Status.ACTIVE) ? MOVEMENT_SPEED : 0);
		final int playerV = ((playerLeft) ? -1 : 1) * playerSpeed;

		final long time = (long) Math.ceil(((double) playerX - hitboxX)
				/ ((double) koopaV - playerV));
		final long bounceTime;
		if (koopaV == -playerV) {
			bounceTime = Long.MAX_VALUE;
		} else {
			bounceTime = (long) Math.ceil(((double) bounceHitboxX - playerX)
					/ ((double) koopaV + playerV));
		}

		return Math.min(time, (bounceTime >= 0) ? bounceTime : Long.MAX_VALUE);
	}

	public static boolean isInHitbox(final Position playerPos, final Koopa koopa) {
		final Position koopaPos = koopa.getCurrentPos();
		return playerPos.sharesRoadWith(koopaPos)
				&& koopa.getHitbox().includes(playerPos.getX());
	}

	public static enum Status {
		ACTIVE, STOMPED, CARRIED;
	}
}
