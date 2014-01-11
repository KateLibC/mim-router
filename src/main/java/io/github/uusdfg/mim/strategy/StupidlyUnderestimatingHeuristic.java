package io.github.uusdfg.mim.strategy;

import io.github.uusdfg.mim.action.GetYoshi;
import io.github.uusdfg.mim.action.ReturnArtifact;
import io.github.uusdfg.mim.data.Game;
import io.github.uusdfg.mim.data.Kiosk;
import io.github.uusdfg.mim.data.Koopa;
import io.github.uusdfg.mim.data.Level;
import io.github.uusdfg.mim.data.Player;
import io.github.uusdfg.mim.data.Koopa.Status;

import java.util.EnumMap;
import java.util.Map;

public class StupidlyUnderestimatingHeuristic implements AStarHeuristic {

	private static final Map<Status, Long> KOOPA_PENALTIES = new EnumMap<>(
			Status.class);
	static {
		KOOPA_PENALTIES.put(Status.ACTIVE, 1l);
		KOOPA_PENALTIES.put(Status.STOMPED, 0l);
		KOOPA_PENALTIES.put(Status.CARRIED, 0l);
	}

	private static final long YOSHI_PENALTY = GetYoshi.GLOBULATOR_TIME;

	public long estimateRemainingLevelsTime(final Game game) {
		return 0;
	}

	public long estimateLevelTime(final Level level, final Player player) {
		if (level.isCompleted()) {
			return 0;
		}

		return getTotalKoopaPenalty(level)
				+ getTotalKioskPenalty(level, player) + getYoshiPenalty(player);
	}

	public long getTotalKoopaPenalty(final Level level) {
		int penalty = 0;
		for (Koopa koopa : level.getKoopas()) {
			if (koopa.hasArtifact()) {
				penalty += KOOPA_PENALTIES.get(koopa.getStatus());
			}
		}
		return penalty;
	}

	public long getTotalKioskPenalty(final Level level, final Player player) {
		final long perArtifactPenalty = ReturnArtifact
				.getArtifactReturningTime(player);

		long penalty = 0;
		for (Kiosk kiosk : level.getKiosks()) {
			if (!kiosk.isCompleted()) {
				penalty += perArtifactPenalty;
			}
		}
		return penalty;
	}

	public long getYoshiPenalty(final Player player) {
		return (player.hasYoshi()) ? 0 : YOSHI_PENALTY;
	}

}
