package io.github.uusdfg.mim.action;

import io.github.uusdfg.mim.action.Route.HistoryEntry;
import io.github.uusdfg.mim.data.Game;
import io.github.uusdfg.mim.data.Koopa;
import io.github.uusdfg.mim.data.Level;
import io.github.uusdfg.mim.data.Player;
import io.github.uusdfg.mim.data.Range;
import io.github.uusdfg.mim.data.Koopa.Status;

import java.util.ArrayList;
import java.util.Collection;

public class PickUpMultipleArtifacts implements Action {

	private final Collection<Integer> koopaIds;

	public PickUpMultipleArtifacts(final Collection<Integer> koopaIds) {
		this.koopaIds = koopaIds;
	}

	public boolean checkIfPossible(final Game game) {
		// 1. All Koopas must be in the current level, or we've got a problem.
		final Collection<Koopa> koopas = getKoopas(game);
		if (koopas == null) {
			return false;
		}

		// For each of those koopas...
		// 2. The Koopa must actually have an artifact.
		// 3. The Koopa in question must have been stomped so that the artifact
		// is available.
		// 4. The player must be on the same road as the artifact in order to
		// pick it up without first taking a transition.
		for (Koopa koopa : koopas) {
			boolean possible = koopa.hasArtifact()
					&& (koopa.getStatus() == Status.STOMPED)
					&& koopa.getCurrentPos().sharesRoadWith(
							game.getPlayer().getPos());
			if (!possible) {
				return false;
			}
		}

		// 5. The hitbox must intersect that of all of the other Koopas, so the
		// player can indeed pick them up with a single action.
		return (getHitbox(koopas) != null);
	}

	public long getTimeRequired(final Game game, final Route pastActions) {
		// Since the Koopas have all presumably been stomped, we don't have to
		// worry about when they'll come into intersection. We just need to
		// figure out how long it takes the player to reach the point of
		// intersection.
		Range hitbox = getHitbox(getKoopas(game));

		final int speed = game.getPlayer().getRunSpeed();
		final int playerX = game.getPlayer().getPos().getX();
		if (hitbox.includes(playerX)) {
			return 0;
		}

		final int hitboxEntryPoint = (playerX > hitbox.getMax()) ? hitbox
				.getMax() : hitbox.getMin();
		return (long) Math.ceil(Math.abs((double) playerX - hitboxEntryPoint)
				/ ((double) speed));
	}

	public long perform(final Game game, final Route pastActions) {
		final long time = getTimeRequired(game, pastActions);

		// The player is going to run to the nearest edge of the Koopas'
		// aggregate hitbox.
		final Collection<Koopa> koopas = getKoopas(game);
		final Range hitbox = getHitbox(koopas);
		final Player player = game.getPlayer();
		final int oldX = player.getPos().getX();
		player.run((hitbox.getMax() < oldX), time);

		// All of the Koopas will change their state to being picked up. The
		// game will switch the player to carrying whichever of the Koopas'
		// artifacts has the highest index.
		int artifactNumber = 0;
		for (Koopa koopa : koopas) {
			koopa.setStatus(Status.CARRIED);
			artifactNumber = Math
					.max(artifactNumber, koopa.getArtifactNumber());
		}
		player.setCurrentArtifact(artifactNumber);

		// The rest of the level will move around in the meantime.
		player.getLevel().advanceNpcs(time);

		return time;
	}

	@Override
	public String toString() {
		return String.format("Pick up artifact from Koopas %s", koopaIds);
	}

	public String toDetailedString(final Game previousState,
			final HistoryEntry historyEntry) {
		final long time = getTimeRequired(previousState,
				historyEntry.backReference);
		final StringBuilder str = new StringBuilder("Pick up artifacts from (");

		final Level level = previousState.getPlayer().getLevel();
		boolean firstKoopa = true;
		for (Koopa koopa : getKoopas(previousState)) {
			if (firstKoopa) {
				firstKoopa = false;
			} else {
				str.append(" & ");
			}

			final Koopa koopaClone = new Koopa(koopa, level);
			koopaClone.advance(time);
			str.append(koopa.getArtifactNumber()).append(" at ")
					.append(koopaClone.getCurrentPos());
		}

		str.append(")");
		return str.toString();
	}

	protected final Collection<Koopa> getKoopas(final Game game) {
		final Level level = game.getPlayer().getLevel();
		if (level == null) {
			return null;
		}

		final Collection<Koopa> koopas = new ArrayList<Koopa>(koopaIds.size());
		for (int koopaId : koopaIds) {
			final Koopa koopa = level.getKoopa(koopaId);
			if (koopa == null) {
				return null;
			}
			koopas.add(koopa);
		}
		return koopas;
	}

	protected final Range getHitbox(final Collection<Koopa> koopas) {
		Range hitbox = null;
		for (Koopa koopa : koopas) {
			if (hitbox == null) {
				hitbox = koopa.getHitbox();
			} else {
				hitbox = koopa.getHitbox().intersectionWith(hitbox);
				if (hitbox == null) {
					return null;
				}
			}
		}
		return hitbox;
	}
}
