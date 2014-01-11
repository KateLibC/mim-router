package io.github.uusdfg.mim.action;

import io.github.uusdfg.mim.action.Route.HistoryEntry;
import io.github.uusdfg.mim.data.Game;
import io.github.uusdfg.mim.data.Koopa;
import io.github.uusdfg.mim.data.Level;
import io.github.uusdfg.mim.data.Player;
import io.github.uusdfg.mim.data.Position;
import io.github.uusdfg.mim.data.Range;
import io.github.uusdfg.mim.data.Koopa.Status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class StompMultipleKoopas implements Action {

	public static final long POST_STOMP_TIME = 40;

	private final Collection<Integer> koopaIds;

	private final boolean running;

	private final boolean landLeft;

	public StompMultipleKoopas(final Collection<Integer> koopaIds,
			final boolean running, final boolean landLeft) {
		this.koopaIds = koopaIds;
		this.running = running;
		this.landLeft = landLeft;
	}

	public boolean checkIfPossible(final Game game) {
		// 1. We can't have more than two Koopas because their movement patterns
		// make intersecting positions impossible otherwise.
		// XXX Technically, if the Koopas are moving in the same direction and
		// overlapping, we can have more than two here, but our model doesn't
		// allow such conditions to develop. So, we make it easy on ourselves.
		if (koopaIds.size() != 2) {
			return false;
		}

		// 2. The Koopas must all exist in the current level.
		final Collection<Koopa> koopas = getKoopas(game);
		if (koopas == null) {
			return false;
		}

		// 3. The player must be on the same road as the Koopas.
		// 4. The Koopas must not have already been stomped.
		final Player player = game.getPlayer();
		for (Koopa koopa : koopas) {
			if (!(koopa.getCurrentPos().sharesRoadWith(player.getPos()) && (koopa
					.getStatus() == Status.ACTIVE))) {
				return false;
			}
		}

		// If all of the above conditions hold, the Koopas will eventually cross
		// paths (because they simply walk back and forth across the level), and
		// the player will eventually be able to be at the point of interesction
		// when they do.
		return true;
	}

	public long getTimeRequired(final Game game, final Route pastActions) {
		final Collection<Koopa> koopas = getKoopas(game);
		final Player player = game.getPlayer();

		return POST_STOMP_TIME + getHitPoint(player, koopas).t;
	}

	public long perform(final Game game, final Route pastActions) {
		// The player runs over to where the Koopas will intersect...
		final Collection<Koopa> koopas = getKoopas(game);
		final Player player = game.getPlayer();
		final HitPoint hitPoint = getHitPoint(player, koopas);

		final Position oldPlayerPos = player.getPos();
		player.setPos(new Position(landLeft, oldPlayerPos.getRoadIndex(),
				hitPoint.x));
		player.run((hitPoint.t < player.getPos().getX()), hitPoint.t);

		// ...during which time, all of the NPCs move around, including these
		// Koopas (so they're at the place where they intersect)...
		final Level level = player.getLevel();
		level.advanceNpcs(hitPoint.t);

		// ...and then the player bounces off.
		player.move(getBounceSpeed(player), landLeft, POST_STOMP_TIME);

		// After the player hits, the target Koopas' statuses change. The NPCs
		// advance during the stomp time as well. It's important to change the
		// status first, however, so the Koopas don't keep moving.
		long animationTime = 0;
		for (Koopa koopa : koopas) {
			koopa.setStatus(Status.STOMPED);
			animationTime = Math.max(animationTime, koopa.getAnimationTime());
		}
		level.advanceNpcs(POST_STOMP_TIME);

		// Finally, stomping on the Koopa adds a slight delay in when the player
		// is allowed to transition to another screen. We'll use whichever of
		// the delays is longest, since the delays don't compound.
		player.setTransitionDelayEndSafe(pastActions.getTotalTime()
				+ animationTime);

		return (hitPoint.t + POST_STOMP_TIME);
	}

	@Override
	public String toString() {
		return String.format("Stomp %d while %s and facing %s", koopaIds,
				running ? "running" : "walking", landLeft ? "left" : "right");
	}

	public String toDetailedString(final Game previousState,
			final HistoryEntry historyEntry) {
		final StringBuilder str = new StringBuilder("Stomp (");

		final Collection<Koopa> koopas = getKoopas(previousState);
		final Player player = previousState.getPlayer();
		final long runTime = getHitPoint(player, koopas).t;

		for (Koopa koopa : koopas) {
			final Koopa koopaClone = new Koopa(koopa, player.getLevel());
			koopaClone.advance(runTime);
			str.append(koopaClone.getId()).append(" at ")
					.append(koopaClone.getCurrentPos());
		}

		str.append(") while ").append(running ? "running" : "walking");
		str.append(" and facing ").append(landLeft ? "left" : "right");
		return str.toString();
	}

	protected final Collection<Koopa> getKoopas(final Game game) {
		final Level level = game.getPlayer().getLevel();
		if (level == null) {
			return null;
		}

		final Collection<Koopa> koopas = new ArrayList<Koopa>(koopaIds.size());
		for (Integer koopaId : koopaIds) {
			final Koopa koopa = level.getKoopa(koopaId);
			if (koopa == null) {
				return null;
			}
			koopas.add(koopa);
		}
		return koopas;
	}

	// XXX This assumes that there are only two Koopas, pretty hard.
	protected HitPoint getHitPoint(final Player player,
			final Collection<Koopa> koopas) {
		// LOGIC OVERVIEW:
		//
		// Start with the two Koopas' current trajectories.
		// 1. If the Koopas are moving in the same direction, skip.
		// 2. Figure out when and where the two Koopas' leading edges will
		// intersect.
		// 3. Figure out which direction the player has to run to reach that
		// spot. Denote the Koopa moving in the opposite direction Koopa 1 and
		// the Koopa moving in the same direction as the player Koopa 2.
		// 4. Figure out when the following intersections will occur:
		// (a) The two Koopas' leading edges.
		// (b) Koopa 1's leading edge and the player, running toward it.
		// (c) Koopa 1's leading edge and Koopa B's trailing edge.
		// (d) The player and Koopa 2's trailing edge.
		// (e) The two Koopa's trailing edges.
		// 5. Decide what will happen based on the following:
		// i. If (e) < 0, skip. Koopas won't cross paths.
		// ii. If (b) <= (a), use (a) and put the player on the point of
		// intersection.
		// iii. If (a) < (b) <= (c), use (b).
		// iv. If (c) < (d) <= (e), use (d).
		// v. If (d) > (e), skip. Player can't reach in time.
		//
		// If we decide to skip, figure out which Koopa will bounce first. Redo
		// the math with that Koopa's post-bounce lines. If we still skip, try
		// having the other Koopa bounce and redo the math with both Koopas'
		// bounce lines. We shouldn't ever have to bounce more than twice
		// because the player moves faster than the Koopas.
		final Iterator<Koopa> koopaIt = koopas.iterator();
		final Koopa koopa1 = koopaIt.next();
		final Koopa koopa2 = koopaIt.next();
		final boolean left1 = koopa1.getCurrentPos().isFacingLeft();
		final boolean left2 = koopa2.getCurrentPos().isFacingLeft();

		final HitPoint firstIntersection = getHitPoint(koopa1.getHitbox(),
				left1, koopa2.getHitbox(), left2, player);
		if (firstIntersection != null) {
			return firstIntersection;
		}

		final HitPoint secondIntersection;
		final boolean bounce1first = koopa1.getTimeToBounce() < koopa2
				.getTimeToBounce();
		if (bounce1first) {
			secondIntersection = getHitPoint(koopa1.getBounceHitbox(), !left1,
					koopa2.getHitbox(), left2, player);
		} else {
			secondIntersection = getHitPoint(koopa1.getHitbox(), left1,
					koopa2.getBounceHitbox(), !left2, player);
		}
		if (secondIntersection != null) {
			return secondIntersection;
		}

		final HitPoint thirdIntersection = getHitPoint(
				koopa1.getBounceHitbox(), !left1, koopa2.getBounceHitbox(),
				!left2, player);
		if (thirdIntersection != null) {
			return thirdIntersection;
		}

		final HitPoint fourthIntersection;
		if (bounce1first) {
			fourthIntersection = getHitPoint(koopa1.getDoubleBounceHitbox(),
					left1, koopa2.getBounceHitbox(), !left2, player);
		} else {
			fourthIntersection = getHitPoint(koopa1.getBounceHitbox(), !left1,
					koopa2.getDoubleBounceHitbox(), left2, player);
		}
		if (fourthIntersection == null) {
			System.err.printf("WTF mate %s %s %s%n", koopa1, koopa2, player);
			System.err.printf("bounce %s %s%n", koopa1.getBounceHitbox(),
					koopa2.getBounceHitbox());
			System.err.printf("dblbounce %s %s%n",
					koopa1.getDoubleBounceHitbox(),
					koopa2.getDoubleBounceHitbox());
			throw new RuntimeException("WTF");
		}
		return fourthIntersection;
	}

	protected HitPoint getHitPoint(final Range box1, final boolean left1,
			final Range box2, final boolean left2, final Player player) {
		// If the Koopas are moving in the same direction at the moment,
		// they won't intersect until one's bounced. We'll have to be called
		// with the post-bounce coordinates.
		if (left1 == left2) {
			return null;
		}

		// TODO Confirm that this is right and then probably document it.
		final int leftLeadingX, rightLeadingX, leftTrailingX, rightTrailingX;
		if (left1) {
			leftLeadingX = box1.getMin();
			rightLeadingX = box2.getMax();
			leftTrailingX = box1.getMax();
			rightTrailingX = box2.getMin();
		} else {
			leftLeadingX = box2.getMin();
			rightLeadingX = box1.getMax();
			leftTrailingX = box2.getMax();
			rightTrailingX = box1.getMin();
		}

		final HitPoint leadingIntersection = getLineIntersection(leftLeadingX,
				-1, rightLeadingX, 1);
		final HitPoint trailingIntersection = getLineIntersection(
				leftTrailingX, -1, rightTrailingX, 1);
		if (trailingIntersection.t < 0) {
			// If the Koopas already passed each other, we need both of them to
			// reverse. That has to happen in a different call.
			return null;
		}

		final int playerX = player.getPos().getX();
		final boolean playerLeft = leadingIntersection.x < playerX;
		final int playerV = player.getRunSpeed() * (playerLeft ? -1 : 1);

		final HitPoint midIntersection, frontHit, backHit;
		if (playerLeft) {
			midIntersection = getLineIntersection(leftTrailingX, -1,
					rightLeadingX, 1);
			frontHit = getLineIntersection(playerX, playerV, rightLeadingX, 1);
			backHit = getLineIntersection(playerX, playerV, leftTrailingX, -1);
		} else {
			midIntersection = getLineIntersection(leftLeadingX, -1,
					rightTrailingX, 1);
			frontHit = getLineIntersection(playerX, playerV, leftLeadingX, -1);
			backHit = getLineIntersection(playerX, playerV, rightTrailingX, 1);
		}

		if (frontHit.t <= leadingIntersection.t) {
			return leadingIntersection;
		} else if (frontHit.t <= midIntersection.t) {
			return frontHit;
		} else if (backHit.t <= trailingIntersection.t) {
			return backHit;
		} else {
			return null;
		}
	}

	protected HitPoint getLineIntersection(final int x1, final int v1,
			final int x2, final int v2) {
		if (v1 == v2) {
			return null;
		}

		final long t = (long) Math
				.ceil((double) (x1 - x2) / (double) (v2 - v1));
		final int x = x1 + (int) (v1 * t);
		return new HitPoint(x, t);
	}

	protected int getKoopaSpeed(final Position currentPos) {
		return currentPos.isFacingLeft() ? -1 : 1;
	}

	private int getBounceSpeed(final Player player) {
		return (running) ? player.getRunSpeed() : player.getWalkSpeed();
	}

	protected static final class HitPoint {
		public int x;
		public long t;

		public HitPoint() {
		}

		public HitPoint(final int x, final long t) {
			this.x = x;
			this.t = t;
		}

		@Override
		public String toString() {
			return String.format("(x=%d, t=%d)", x, t);
		}
	}

}
