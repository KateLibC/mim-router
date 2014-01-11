package io.github.uusdfg.mim.action;

import io.github.uusdfg.mim.action.Route.HistoryEntry;
import io.github.uusdfg.mim.data.Game;
import io.github.uusdfg.mim.data.Kiosk;
import io.github.uusdfg.mim.data.Koopa;
import io.github.uusdfg.mim.data.Level;
import io.github.uusdfg.mim.data.Player;
import io.github.uusdfg.mim.data.Position;
import io.github.uusdfg.mim.data.Koopa.Status;

import java.util.Collection;

public class ReturnArtifact implements Action {

	// TODO Consider per-kiosk time estimates.
	// In the meantime, based on my playthroughs, 90 seems like an okay guess
	// for the first floor, where the questions are pre-determined, and 120
	// seems like a fairly aggressive estimate for the later floors where I have
	// to actually read.
	public static final long EASY_QUESTION_TIME = 90;

	public static final long HARD_QUESTION_TIME = 120;

	// Based on observation, I seem to spend about 150 frames in the menu when I
	// switch artifacts, and there's usually some slop on either side of the
	// menu. Therefore, 160 frames seems like a good guess.
	public static final long SWITCH_TIME = 160;

	private final int artifactId;

	public ReturnArtifact(final int artifactId) {
		this.artifactId = artifactId;
	}

	public boolean checkIfPossible(final Game game) {
		// 1. The Kiosk must exist.
		final Kiosk kiosk = getKiosk(game);
		if (kiosk == null) {
			return false;
		}

		// 2. The player must be on the same road as the kiosk in order to walk
		// to it.
		final Player player = game.getPlayer();
		final Level level = player.getLevel();
		if (level == null) {
			return false;
		}

		if (!kiosk.getBasePos().sharesRoadWith(player.getPos())) {
			return false;
		}

		// 3. The kiosk can't already have had the artifact passed back.
		if (kiosk.isCompleted()) {
			return false;
		}

		// 4. The player must be carrying the required artifact.
		return (level.getKoopaByArtifact(kiosk.getArtifactId()).getStatus() == Status.CARRIED);
	}

	public long getTimeRequired(final Game game, final Route pastActions) {
		// The player has to run to within range of the kiosk's hitbox, and then
		// answer the questions there. Also, if the player currently has the
		// wrong artifact equipped, the player has to switch to it.
		final Kiosk kiosk = getKiosk(game);
		final Player player = game.getPlayer();
		final Position kioskPos = kiosk.getActivePosNearestTo(player.getPos());
		return (player.getTimeToReach(kioskPos)
				+ getArtifactReturningTime(player) + getArtifactSwitchingTime(player));
	}

	public long perform(final Game game, final Route pastActions) {
		// The player runs over to the kiosk...
		final Kiosk kiosk = getKiosk(game);
		final Player player = game.getPlayer();
		final Position kioskPos = kiosk.getActivePosNearestTo(player.getPos());
		final long runTime = player.getTimeToReach(kioskPos);
		player.run((kioskPos.getX() < player.getPos().getX()), runTime);

		// ...and then the kiosk is completed.
		kiosk.setCompleted(true);

		// The game switches the artifact equipped by the player to the one with
		// the lowest ID that the player is holding. If the player is not
		// holding any other artifacts, great.
		final Level level = player.getLevel();
		final long artifactSwitchingTime = getArtifactSwitchingTime(player);
		player.setCurrentArtifact(getFirstHeldArtifact(level));

		// Meanwhile, the rest of the stage goes on.
		final long npcTime = runTime + getArtifactReturningTime(player);
		level.advanceNpcs(npcTime);
		return npcTime + artifactSwitchingTime;
	}

	@Override
	public String toString() {
		return String.format("Return artifact %d", artifactId);
	}

	public String toDetailedString(final Game previousState,
			final HistoryEntry historyEntry) {
		final Player player = previousState.getPlayer();
		final int playerArtifact = player.getCurrentArtifact();

		final Kiosk kiosk = player.getLevel().getKiosk(artifactId);

		return String.format(
				"Return artifact %d (previously holding %d%s); kiosk at %s",
				artifactId, playerArtifact, (artifactId == playerArtifact) ? ""
						: "!!!!!", kiosk.getBasePos());
	}

	public static long getArtifactReturningTime(final Player player) {
		return getArtifactReturningTime(player.getFloor());
	}

	public static long getArtifactReturningTime(final int floorId) {
		return (floorId <= 1) ? EASY_QUESTION_TIME : HARD_QUESTION_TIME;
	}

	protected final Kiosk getKiosk(final Game game) {
		final Level level = game.getPlayer().getLevel();
		return (level == null) ? null : level.getKiosk(artifactId);
	}

	protected long getArtifactSwitchingTime(final Player player) {
		return (player.getCurrentArtifact() == artifactId) ? 0 : SWITCH_TIME;
	}

	protected final int getFirstHeldArtifact(final Level level) {
		final Collection<Koopa> artifactHolders = level.getArtifactHolders();
		int firstHeld = artifactHolders.size();
		for (Koopa koopa : artifactHolders) {
			final int artifactId = koopa.getArtifactNumber();
			if ((koopa.getStatus() == Status.CARRIED)
					&& !level.getKiosk(artifactId).isCompleted()
					&& (artifactId < firstHeld)) {
				firstHeld = artifactId;
			}
		}

		// If the player has no artifacts... artifact 0 should make the
		// completion code look good, I guess.
		if (firstHeld >= artifactHolders.size()) {
			firstHeld = 0;
		}
		return firstHeld;
	}

}
