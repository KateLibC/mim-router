package io.github.uusdfg.mim.data.loader;

import io.github.uusdfg.mim.data.Entrance;
import io.github.uusdfg.mim.data.Exit;
import io.github.uusdfg.mim.data.Floor;
import io.github.uusdfg.mim.data.Game;
import io.github.uusdfg.mim.data.Kiosk;
import io.github.uusdfg.mim.data.Level;
import io.github.uusdfg.mim.data.Player;
import io.github.uusdfg.mim.data.Position;
import io.github.uusdfg.mim.data.Road;
import io.github.uusdfg.mim.data.Transition;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.EndianUtils;
import org.apache.commons.io.IOUtils;

public class SaveStateLoader {

	public static final int NUM_FLOORS = 3;

	public static final int NUM_LEVELS_PER_FLOOR = 5;

	public static final String FILENAME_FORMAT = "%d-%d.nnn";

	private static final List<Integer> CASTLE_ROAD_XS = Arrays.asList(43, 115,
			187, 259, 331);

	private static final int FLOOR_EXIT_X = 404;

	private static final int MAX_NUM_SLOTS = 128;

	private static final List<List<Long>> LEVEL_LOAD_TIMES = new ArrayList<>(3);
	static {
		LEVEL_LOAD_TIMES.add(Arrays.asList(518l, 502l, 484l, 523l, 503l));
		LEVEL_LOAD_TIMES.add(Arrays.asList(510l, 502l, 515l, 506l, 497l));
		LEVEL_LOAD_TIMES.add(Arrays.asList(520l, 495l, 522l, 512l, 544l));
	}

	// TODO Real value here.
	private static final long FLOOR_EXIT_TIME = 0;

	private static final int HEADER_LENGTH = 0xbdb;

	public Game loadGameFromDir(final File baseDir) throws IOException {
		if (!baseDir.isDirectory()) {
			throw new FileNotFoundException("Directory not found at " + baseDir);
		}

		final Game game = new Game();
		for (int floorId = 1; floorId <= NUM_FLOORS; floorId++) {
			final Position exitPosition = new Position(false,
					Road.CASTLE_ROAD_ID, FLOOR_EXIT_X);

			final Floor floor = new Floor(floorId, exitPosition);
			final Road castleRoad = floor.getCastleRoad();
			for (int levelId = 1; levelId <= NUM_LEVELS_PER_FLOOR; levelId++) {
				final Level level = loadLevelFromSaveState(
						levelId,
						new File(baseDir, String.format(FILENAME_FORMAT,
								floorId, levelId)));
				floor.addLevel(level);
				castleRoad.addTransition(new Entrance(getLevelDoorPos(level),
						level.getStartPos(), levelId, getLevelLoadTime(floorId,
								levelId)));
			}
			castleRoad.addTransition(new Entrance(exitPosition, exitPosition,
					Entrance.FLOOR_EXIT_LEVEL_ID, FLOOR_EXIT_TIME));

			game.addFloor(floor);
		}

		initializePlayer(game.getPlayer());

		return game;
	}

	public Level loadLevelFromSaveState(final int id, final File saveFile)
			throws IOException {
		final BufferedInputStream in = new BufferedInputStream(
				new GZIPInputStream(new FileInputStream(saveFile)));
		Level level;
		try {
			level = loadLevelFromStream(id, in);
		} finally {
			IOUtils.closeQuietly(in);
		}
		return level;
	}

	public Level loadLevelFromStream(final int id, final BufferedInputStream in)
			throws IOException {
		// Mark the beginning of the actual RAM dump (past the snes9x
		// header) so we can easily address within it. Give a ridiculously
		// large read limit when marking so we don't have to worry about
		// remarking it. If we buffer the whole level as a side effect,
		// great.
		final long bytesSkipped = IOUtils.skip(in, HEADER_LENGTH);
		if (bytesSkipped != HEADER_LENGTH) {
			throw new IOException(
					"Wasn't able to skip bytes properly in level stream; giving up");
		}
		in.mark(0x400000);

		final OffsetTable table = new OffsetTable().load(in);
		sanityCheckTable(table);

		final Level level = new Level(id);

		in.reset();
		readNpcSlots(in, table, level);

		in.reset();
		readRoads(in, table, level);

		in.reset();
		readKiosks(in, table, level);

		level.setStartPos(getLevelStartPos(level));
		return level;
	}

	protected void readNpcSlots(final InputStream in, final OffsetTable table,
			final Level outLevel) throws IOException {
		final int numSlots = table.getNumSlots();

		// The road indices for the NPC slots/transitions are written in a weird
		// format. Every five bytes, a combination of the slot number and 0xFF
		// is written out. We'll just extract the interesting part.
		IOUtils.skip(in, table.getRoadIndices());

		final List<Integer> roadIndices = new ArrayList<>(numSlots);
		for (int i = 0; i < numSlots; i++) {
			IOUtils.skip(in, 1);
			roadIndices.add(in.read());
			IOUtils.skip(in, 3);
		}

		// Now if we extract the starting NPC x-positions, we can assemble the
		// slots for the possible NPC coordinates. The x-positions are the
		// second 2-byte words out of the ten-byte per-slot blocks that we'll
		// skip through here. The stored values are 1/16th of the real values.
		in.reset();
		IOUtils.skip(in, table.getNpcXs());

		for (int i = 0; i < numSlots; i++) {
			IOUtils.skip(in, 2);
			final int x = 16 * EndianUtils.readSwappedUnsignedShort(in);
			IOUtils.skip(in, 6);

			// All NPCs start out facing right.
			outLevel.addNpcSlot(i, new Position(false, roadIndices.get(i), x));
		}
	}

	protected void readRoads(final InputStream in, final OffsetTable table,
			final Level outLevel) throws IOException {
		// Extract the basic data about the road transitions from the saves.
		final int numTransitions = table.getNumSlots();
		final List<RoadNode> nodes = new ArrayList<>(numTransitions);

		IOUtils.skip(in, table.getTransitionLinks());
		for (int i = 0; i < numTransitions; i++) {
			final RoadNode node = new RoadNode(i);
			node.loadIds(in);
			nodes.add(node);
		}

		in.reset();
		IOUtils.skip(in, table.getTransitionDistances());
		for (RoadNode node : nodes) {
			node.loadDistances(in);
		}

		// Organize the nodes so on each road, we can see how the transitions
		// are laid out from left to right.
		final Map<Integer, List<RoadNode>> nodesByRoad = new LinkedHashMap<>();
		for (RoadNode node : nodes) {
			// Transitions and NPC slots share the same road IDs. Seriously.
			final int roadId = outLevel.getNpcSlot(node.getNodeId())
					.getRoadIndex();
			List<RoadNode> roadNodes = nodesByRoad.get(roadId);
			if (roadNodes == null) {
				roadNodes = new ArrayList<>();
				nodesByRoad.put(roadId, roadNodes);
			}
			roadNodes.add(node);
		}

		final Map<Integer, Position> transitionPositions = new LinkedHashMap<>(
				numTransitions);
		for (Map.Entry<Integer, List<RoadNode>> entry : nodesByRoad.entrySet()) {
			final int roadId = entry.getKey();
			final List<RoadNode> roadNodes = entry.getValue();

			int previousId = RoadNode.NO_TRANSITION_ID;
			for (int i = 0; i < roadNodes.size() - 1; i++) {
				for (int j = i; j < roadNodes.size(); j++) {
					final RoadNode node = roadNodes.get(j);
					if (node.getLeftId() == previousId) {
						Collections.swap(roadNodes, i, j);
						previousId = node.getNodeId();
						break;
					}
				}
			}

			// Once the transitions in a given road are ordered, we can figure
			// out their X-positions easily.
			int x = 0;
			for (int i = 0; i < roadNodes.size(); i++) {
				final RoadNode node = roadNodes.get(i);
				final Position position = new Position(false, roadId, x);
				transitionPositions.put(node.getNodeId(), position);
				x += node.getRightDistance();
			}

			// We can also get started on assembling the roads.
			outLevel.addRoad(new Road(outLevel, roadId, x));
		}

		// Now that we have roads and we know where all the transitions are, we
		// can link them together to form the level.
		final Position exitPosition = new Position(false, Road.CASTLE_ROAD_ID,
				CASTLE_ROAD_XS.get(outLevel.getLevelId() - 1));
		for (int i = 0; i < numTransitions; i++) {
			final Road road = outLevel.getRoad(transitionPositions.get(i)
					.getRoadIndex());
			final RoadNode node = nodes.get(i);
			final List<Transition> nodeTransitions = node.toTransitionList(
					transitionPositions, exitPosition);
			for (Transition transition : nodeTransitions) {
				road.addTransition(transition);
			}
		}
	}

	protected void readKiosks(final InputStream in, final OffsetTable table,
			final Level outLevel) throws IOException {
		// There are three different places in RAM where the info about the
		// kiosks is stored. Each place uses one word per kiosk to give whatever
		// info it contains.
		final int numArtifacts = table.getNumArtifacts();

		IOUtils.skip(in, table.getKioskRoads());
		final List<Integer> kioskRoads = new ArrayList<>(numArtifacts);
		for (int i = 0; i < numArtifacts; i++) {
			kioskRoads.add((int) EndianUtils.readSwappedUnsignedShort(in));
		}

		in.reset();
		IOUtils.skip(in, table.getKioskXs());
		final List<Position> kioskPositions = new ArrayList<>(numArtifacts);
		for (int i = 0; i < numArtifacts; i++) {
			kioskPositions.add(new Position(false, kioskRoads.get(i),
					EndianUtils.readSwappedUnsignedShort(in)));
		}

		in.reset();
		IOUtils.skip(in, table.getKioskArtifacts());
		for (int i = 0; i < numArtifacts; i++) {
			outLevel.addKiosk(new Kiosk(EndianUtils
					.readSwappedUnsignedShort(in) - 1, kioskPositions.get(i),
					outLevel));
		}
	}

	protected Position getLevelDoorPos(final Level level) {
		final int x = CASTLE_ROAD_XS.get(level.getLevelId() - 1);
		return new Position(false, Road.CASTLE_ROAD_ID, x);
	}

	protected Position getLevelStartPos(final Level level) {
		for (Road road : level.getRoads()) {
			for (Transition transition : road.getTransitions()) {
				if (transition instanceof Exit) {
					return transition.getSrcPos().withLeftFlag(false);
				}
			}
		}
		return null;
	}

	protected long getLevelLoadTime(final int floorId, final int levelId) {
		return LEVEL_LOAD_TIMES.get(floorId - 1).get(levelId - 1);
	}

	protected void initializePlayer(final Player player) {
		player.setFloor(1);
		player.setLevel(null);
		player.setPos(new Position(true, Road.CASTLE_ROAD_ID, FLOOR_EXIT_X));
		player.setYoshi(false);
	}

	protected void sanityCheckTable(final OffsetTable table) throws IOException {
		final int numSlots = table.getNumSlots();
		if (numSlots > MAX_NUM_SLOTS) {
			throw new IOException("Savestate claimed to have " + numSlots
					+ " slots; assuming bad");
		}
	}

}
