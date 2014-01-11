package io.github.uusdfg.mim.data;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Floor {

	public static final int DEFAULT_CASTLE_ROAD_WIDTH = 448;

	private final int floorId;

	private final Map<Integer, Level> levels = new LinkedHashMap<>();

	private Position startPos;

	private Road castleRoad;

	public Floor(final int floorId, final Position startPos) {
		this.floorId = floorId;
		this.castleRoad = new Road(null, Road.CASTLE_ROAD_ID,
				DEFAULT_CASTLE_ROAD_WIDTH);
		this.startPos = startPos;
	}

	public Floor(final Floor src) {
		this.floorId = src.floorId;
		this.castleRoad = src.castleRoad;
		this.startPos = src.startPos;

		for (Map.Entry<Integer, Level> entry : src.levels.entrySet()) {
			this.levels.put(entry.getKey(), new Level(entry.getValue()));
		}
	}

	public int getFloorId() {
		return floorId;
	}

	public Position getStartPos() {
		return startPos;
	}

	public Road getCastleRoad() {
		return castleRoad;
	}

	public void setStartPos(final Position startPos) {
		this.startPos = startPos;
	}

	public void setCastleRoad(final Road castleRoad) {
		this.castleRoad = castleRoad;
	}

	public Level getLevel(final int levelId) {
		return levels.get(levelId);
	}

	public Collection<Level> getLevels() {
		return Collections.unmodifiableCollection(levels.values());
	}

	public void addLevel(final Level level) {
		levels.put(level.getLevelId(), level);
	}

	public boolean isCompleted() {
		for (Level level : levels.values()) {
			if (!level.isCompleted()) {
				return false;
			}
		}
		return true;
	}

	public boolean sameState(final Floor floor) {
		if (levels.size() != floor.levels.size()) {
			return false;
		}

		for (Map.Entry<Integer, Level> entry : levels.entrySet()) {
			final int levelId = entry.getKey();
			final Level expectedLevel = entry.getValue();
			final Level actualLevel = floor.levels.get(levelId);
			if ((actualLevel == null) || !expectedLevel.sameState(actualLevel)) {
				return false;
			}
		}

		return true;
	}
}
