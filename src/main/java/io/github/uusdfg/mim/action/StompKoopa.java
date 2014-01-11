package io.github.uusdfg.mim.action;

import io.github.uusdfg.mim.action.Route.HistoryEntry;
import io.github.uusdfg.mim.data.Game;
import io.github.uusdfg.mim.data.Koopa;
import io.github.uusdfg.mim.data.Level;
import io.github.uusdfg.mim.data.Player;
import io.github.uusdfg.mim.data.Koopa.Status;

public class StompKoopa implements Action {

	public static final long POST_STOMP_TIME = 40;

	private final int koopaId;

	private final boolean running;

	private final boolean landLeft;

	public StompKoopa(final int koopaId, final boolean running,
			final boolean landLeft) {
		this.koopaId = koopaId;
		this.running = running;
		this.landLeft = landLeft;
	}

	public boolean checkIfPossible(final Game game) {
		// 1. The Koopa must exist in the current level (and there must be a
		// current level).
		final Koopa koopa = getKoopa(game);
		if (koopa == null) {
			return false;
		}

		// 2. The player must be on the same road as the Koopa in order to run
		// up to it and stomp it.
		// 3. The Koopa must not have already been stomped.
		final Player player = game.getPlayer();
		return koopa.getCurrentPos().sharesRoadWith(player.getPos())
				&& (koopa.getStatus() == Status.ACTIVE);
	}

	// TODO Account for the inherent delay in jumping to stomp something. If the
	// player is too close, additional time may be required.
	// TODO Account for the fact that if you the player just did something that
	// requires jumping (mainly picking up an artifact), the player either needs
	// to land on the Koopa after picking it up or needs to wait until the
	// player has jumped and landed again.
	public long getTimeRequired(final Game game, final Route pastActions) {
		final Koopa koopa = getKoopa(game);
		final Player player = game.getPlayer();
		return POST_STOMP_TIME
				+ koopa.getTimeToHitboxEntry(player.getPos(),
						player.getRunSpeed());
	}

	public long perform(final Game game, final Route pastActions) {
		// The player first runs over to the Koopa...
		final Koopa koopa = getKoopa(game);
		final Player player = game.getPlayer();
		final long runTime = koopa.getTimeToHitboxEntry(player.getPos(),
				player.getRunSpeed());
		player.run((koopa.getCurrentPos().getX() < player.getPos().getX()),
				runTime);

		// ...during which time, all of the NPCs move around...
		final Level level = player.getLevel();
		level.advanceNpcs(runTime);

		// ...and then the player bounces off.
		player.move(getBounceSpeed(player), landLeft, POST_STOMP_TIME);

		// After the player hits, the target Koopa's status changes. The NPCs
		// advance during the stomp time as well. It's important to change the
		// status first, however, so the artifact doesn't keep walking around
		// without a Koopa.
		koopa.setStatus(Status.STOMPED);
		level.advanceNpcs(POST_STOMP_TIME);

		// Finally, stomping on the Koopa adds a slight delay in when the player
		// is allowed transition to another screen. The delay begins when the
		// player lands the stomp (so after we run there).
		player.setTransitionDelayEndSafe(pastActions.getTotalTime() + runTime
				+ koopa.getAnimationTime());

		return (runTime + POST_STOMP_TIME);
	}

	@Override
	public String toString() {
		return String.format("Stomp %d while %s and facing %s", koopaId,
				running ? "running" : "walking", landLeft ? "left" : "right");
	}

	public String toDetailedString(final Game previousState,
			final HistoryEntry historyEntry) {
		final Player player = previousState.getPlayer();
		final Koopa koopaClone = new Koopa(getKoopa(previousState),
				player.getLevel());
		final long runTime = koopaClone.getTimeToHitboxEntry(player.getPos(),
				player.getRunSpeed());
		koopaClone.advance(runTime);

		return String.format("Stomp %d at %s while %s and facing %s", koopaId,
				koopaClone.getCurrentPos(), running ? "running" : "walking",
				landLeft ? "left" : "right");
	}

	protected final Koopa getKoopa(final Game game) {
		final Level level = game.getPlayer().getLevel();
		return (level == null) ? null : level.getKoopa(koopaId);
	}

	private int getBounceSpeed(final Player player) {
		return (running) ? player.getRunSpeed() : player.getWalkSpeed();
	}
}
