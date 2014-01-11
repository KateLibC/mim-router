package io.github.uusdfg.mim.strategy;

import io.github.uusdfg.mim.action.Action;
import io.github.uusdfg.mim.action.GetYoshi;
import io.github.uusdfg.mim.action.PickUpArtifact;
import io.github.uusdfg.mim.action.PickUpMultipleArtifacts;
import io.github.uusdfg.mim.action.ReturnArtifact;
import io.github.uusdfg.mim.action.StompKoopa;
import io.github.uusdfg.mim.action.StompMultipleKoopas;
import io.github.uusdfg.mim.action.TakeTransition;
import io.github.uusdfg.mim.data.Game;
import io.github.uusdfg.mim.data.Kiosk;
import io.github.uusdfg.mim.data.Koopa;
import io.github.uusdfg.mim.data.Level;
import io.github.uusdfg.mim.data.Player;
import io.github.uusdfg.mim.data.Road;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class DefaultActionGenerator implements ActionGenerator {

	public Collection<Action> getPossibleActions(final Game game) {
		final Player player = game.getPlayer();
		final Level level = player.getLevel();
		final Collection<Action> actions;
		if (level == null) {
			actions = getPossibleActionsOutsideLevel(game);
		} else {
			actions = getPossibleActionsInLevel(level, game);
		}
		return actions;
	}

	protected Collection<Action> getPossibleActionsOutsideLevel(final Game game) {
		final Road castleRoad = game.getFloor(game.getPlayer().getFloor())
				.getCastleRoad();
		final List<Action> actions = new ArrayList<>();
		addRoadActions(castleRoad, game, actions);
		return actions;
	}

	protected Collection<Action> getPossibleActionsInLevel(final Level level,
			final Game game) {
		final Road road = level.getRoad(game.getPlayer().getPos()
				.getRoadIndex());

		final Collection<Action> actions = new ArrayList<>();
		addKioskActions(level, game, actions);
		addKoopaActions(level, game, actions);
		addRoadActions(road, game, actions);
		addYoshiAction(game, actions);

		return actions;
	}

	protected void addKioskActions(final Level level, final Game game,
			final Collection<Action> outActions) {
		for (Kiosk kiosk : level.getKiosks()) {
			addActionIfPossible(new ReturnArtifact(kiosk.getArtifactId()),
					game, outActions);
		}
	}

	protected void addKoopaActions(final Level level, final Game game,
			final Collection<Action> outActions) {
		// We handle individual-Koopa and multi-Koopa actions separately because
		// the simplified logic makes the individual ones go faster (I hope so,
		// anyway).
		for (Koopa koopa : level.getKoopas()) {
			if (koopa == null) {
				continue;
			}
			final int koopaId = koopa.getId();
			addActionIfPossible(new PickUpArtifact(koopaId), game, outActions);

			// There are lots of different ways to stomp a Koopa, turns out.
			addActionIfPossible(new StompKoopa(koopaId, false, false), game,
					outActions);
			addActionIfPossible(new StompKoopa(koopaId, false, true), game,
					outActions);
			addActionIfPossible(new StompKoopa(koopaId, true, false), game,
					outActions);
			addActionIfPossible(new StompKoopa(koopaId, true, true), game,
					outActions);
		}

		final Collection<Koopa> koopasOnRoad = level.getKoopasOnRoad(game
				.getPlayer().getPos().getRoadIndex());
		if (koopasOnRoad != null) {
			addMultiKoopaActions(new HashSet<Koopa>(), new HashSet<Koopa>(
					koopasOnRoad), game, outActions);
		}
	}

	protected void addMultiKoopaActions(final Set<Koopa> selection,
			final Set<Koopa> remaining, final Game game,
			final Collection<Action> outActions) {
		for (final Iterator<Koopa> it = remaining.iterator(); it.hasNext();) {
			final Koopa koopa = it.next();

			// Put in one of the remaining Koopas each time. If that actually
			// gives us multiple Koopas, consider adding each of the possible
			// multi-Koopa actions.
			selection.add(koopa);
			if (selection.size() > 1) {
				final Collection<Integer> koopaIds = new ArrayList<>(
						selection.size());
				for (Koopa selected : selection) {
					koopaIds.add(selected.getId());
				}

				addActionIfPossible(new PickUpMultipleArtifacts(koopaIds),
						game, outActions);
				addActionIfPossible(new StompMultipleKoopas(koopaIds, false,
						false), game, outActions);
				addActionIfPossible(new StompMultipleKoopas(koopaIds, false,
						true), game, outActions);
				addActionIfPossible(new StompMultipleKoopas(koopaIds, true,
						false), game, outActions);
				addActionIfPossible(new StompMultipleKoopas(koopaIds, true,
						true), game, outActions);
			}

			// Consider all possible actions using any of the other Koopas, plus
			// the current selection.
			it.remove();
			addMultiKoopaActions(selection, new HashSet<>(remaining), game,
					outActions);

			// Go through and try again, without again considering this Koopa.
			selection.remove(koopa);
		}
	}

	protected void addRoadActions(final Road road, final Game game,
			final Collection<Action> outActions) {
		final int roadId = road.getRoadId();
		for (int i = 0; i < road.getTransitions().size(); i++) {
			addActionIfPossible(new TakeTransition(roadId, i), game, outActions);
		}
	}

	protected void addYoshiAction(final Game game,
			final Collection<Action> outActions) {
		addActionIfPossible(new GetYoshi(), game, outActions);
	}

	protected void addActionIfPossible(final Action action, final Game game,
			final Collection<Action> outActions) {
		if (action.checkIfPossible(game)) {
			outActions.add(action);
		}
	}

}
