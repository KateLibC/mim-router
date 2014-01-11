package io.github.uusdfg.mim.data;

import io.github.uusdfg.mim.rng.SlotRng;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Game {

	private final Map<Integer, Floor> floors = new LinkedHashMap<>();

	private final Player player;

	private final SlotRng slotRng;

	public Game() {
		player = new Player();
		slotRng = new SlotRng();
	}

	public Game(final Game src) {
		// Initialize the floors first so the player clone can look up its
		// level.
		for (Map.Entry<Integer, Floor> entry : src.floors.entrySet()) {
			floors.put(entry.getKey(), new Floor(entry.getValue()));
		}

		this.player = new Player(src.player, this);
		this.slotRng = new SlotRng(src.slotRng);
	}

	public Player getPlayer() {
		return player;
	}

	public Floor getFloor(final int floorId) {
		return floors.get(floorId);
	}

	public Collection<Floor> getFloors() {
		return Collections.unmodifiableCollection(floors.values());
	}

	public void addFloor(final Floor floor) {
		floors.put(floor.getFloorId(), floor);
	}

	public Level getLevel(final int floorId, final int levelId) {
		final Floor floor = floors.get(floorId);
		if (floor == null) {
			return null;
		}

		return floor.getLevel(levelId);
	}

	public boolean isCompleted() {
		for (Floor floor : getFloors()) {
			if (!floor.isCompleted()) {
				return false;
			}
		}
		return true;
	}

	public int getNumLevelsCompleted() {
		int num = 0;
		for (Floor floor : getFloors()) {
			for (Level level : floor.getLevels()) {
				if (level.isCompleted()) {
					num++;
				}
			}
		}
		return num;
	}

	public boolean sameState(final Game game) {
		if (this == game) {
			return true;
		}

		if (!slotRng.equals(game.slotRng)) {
			return false;
		}

		if (!player.equals(game.player)) {
			return false;
		}

		if (floors.size() != game.floors.size()) {
			return false;
		}
		for (Map.Entry<Integer, Floor> entry : floors.entrySet()) {
			final int floorId = entry.getKey();
			final Floor expectedFloor = entry.getValue();
			final Floor actualFloor = game.floors.get(floorId);
			if ((actualFloor == null) || !expectedFloor.sameState(actualFloor)) {
				return false;
			}
		}

		return true;
	}

	public SlotRng getSlotRng() {
		return slotRng;
	}

}
