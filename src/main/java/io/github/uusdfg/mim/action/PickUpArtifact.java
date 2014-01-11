package io.github.uusdfg.mim.action;

import io.github.uusdfg.mim.action.Route.HistoryEntry;
import io.github.uusdfg.mim.data.Game;
import io.github.uusdfg.mim.data.Koopa;
import io.github.uusdfg.mim.data.Level;
import io.github.uusdfg.mim.data.Player;
import io.github.uusdfg.mim.data.Koopa.Status;

public class PickUpArtifact implements Action {

	private final int koopaId;

	public PickUpArtifact(final int koopaId) {
		this.koopaId = koopaId;
	}

	public boolean checkIfPossible(final Game game) {
		// 1. The Koopa must exist in the current level, which must also exist.
		final Koopa koopa = getKoopa(game);
		if (koopa == null) {
			return false;
		}

		// 2. The Koopa must actually have an artifact.
		// 3. The Koopa in question must have been stomped so that the artifact
		// is available.
		// 4. The player must be on the same road as the artifact in order to
		// pick it up without first taking a transition.
		return koopa.hasArtifact()
				&& (koopa.getStatus() == Status.STOMPED)
				&& koopa.getCurrentPos().sharesRoadWith(
						game.getPlayer().getPos());
	}

	public long getTimeRequired(final Game game, final Route pastActions) {
		// There's no delay in picking up an artifact. The player can run there
		// directly.
		return getKoopa(game).getTimeToHitboxEntry(game.getPlayer().getPos(),
				game.getPlayer().getRunSpeed());
	}

	public long perform(final Game game, final Route pastActions) {
		final long time = getTimeRequired(game, pastActions);

		// The player is going to run over to where the Koopa is.
		final Koopa koopa = getKoopa(game);
		final Player player = game.getPlayer();
		final int oldX = player.getPos().getX();
		final int artX = koopa.getCurrentPos().getX();
		player.run((artX < oldX), time);

		// The Koopa will change its state to being picked up. Also, the game
		// will switch the artifact "equipped" by the player to the new one.
		koopa.setStatus(Status.CARRIED);
		player.setCurrentArtifact(koopa.getArtifactNumber());

		// Meanwhile, everything else will run around.
		player.getLevel().advanceNpcs(time);

		return time;
	}

	@Override
	public String toString() {
		return String.format("Pick up artifact from Koopa %d", koopaId);
	}

	public String toDetailedString(final Game previousState,
			final HistoryEntry historyEntry) {
		final long time = getTimeRequired(previousState,
				historyEntry.backReference);
		final Koopa koopaClone = new Koopa(getKoopa(previousState),
				previousState.getPlayer().getLevel());
		koopaClone.advance(time);

		return String.format("Pick up artifact from Koopa %d at %s", koopaId,
				koopaClone.getCurrentPos());
	}

	protected final Koopa getKoopa(final Game game) {
		final Level level = game.getPlayer().getLevel();
		return (level == null) ? null : level.getKoopa(koopaId);
	}
}
