package io.github.uusdfg.mim.data;

import io.github.uusdfg.mim.rng.SlotRng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Level {

	private static final int NUM_NPCS = 5;

	private static final int NUM_USELESS_KOOPAS = 13;

	private static final int NUM_ARTIFACTS = 3;

	private static final int KOOPA_STATUS_WIDTH = 2;

	private static final int HELD_ARTIFACT_BIT_OFFSET = 12;

	private static final int HELD_ARTIFACT_WIDTH = 2;

	private static final int KIOSK_BIT_OFFSET = 16;

	private static final int YOSHI_BIT_OFFSET = 20;

	private static final int COMPLETION_BIT_OFFSET = 21;

	private static final int ROAD_BIT_OFFSET = 24;

	private static final int ROAD_WIDTH = 6;

	// The Koopas have different animation times, depending on which artifacts
	// they hold.
	private static final List<Integer> ANIMATION_TIMES = Arrays.asList(80, 48,
			152);

	private final int levelId;

	private List<Road> roads;

	private List<Kiosk> kiosks;

	private List<Position> npcSlots;

	private List<Koopa> koopas;

	private List<Koopa> koopasByArtifactNumber;

	private List<List<Koopa>> koopasByRoad;

	private Position startPos;

	private boolean completed = false;

	private int completionCode = 0;

	public Level(final int levelId) {
		this(levelId, new Position(false, 0, 0));
	}

	public Level(final int levelId, final Position startPos) {
		this.levelId = levelId;
		this.startPos = startPos;

		this.roads = new ArrayList<>();
		this.npcSlots = new ArrayList<>();
		this.kiosks = new ArrayList<>();
		this.koopas = new ArrayList<>();
		this.koopasByArtifactNumber = new ArrayList<>();
		this.koopasByRoad = new ArrayList<>();
	}

	// NOTE: I adopt the fields that don't change during normal gameplay because
	// it's faster.
	public Level(final Level src) {
		this.completed = src.completed;
		this.completionCode = src.completionCode;
		this.levelId = src.levelId;
		this.startPos = src.startPos;

		this.roads = src.roads;
		this.npcSlots = src.npcSlots;

		this.kiosks = new ArrayList<Kiosk>(src.kiosks.size());
		for (Kiosk srcKiosk : src.kiosks) {
			if (srcKiosk == null) {
				this.kiosks.add(null);
			} else {
				this.kiosks.add(new Kiosk(srcKiosk, this));
			}
		}

		final int numKoopas = src.koopas.size();
		this.koopas = new ArrayList<>(numKoopas);
		this.koopasByArtifactNumber = new ArrayList<>(numKoopas);
		this.koopasByRoad = new ArrayList<>(roads.size());
		for (Koopa srcKoopa : src.koopas) {
			if (srcKoopa == null) {
				this.koopas.add(null);
			} else {
				this.addKoopa(new Koopa(srcKoopa, this));
			}
		}

	}

	public int getLevelId() {
		return levelId;
	}

	public Position getStartPos() {
		return startPos;
	}

	public boolean isCompleted() {
		return completed;
	}

	public int getCompletionCode() {
		return completionCode;
	}

	public boolean isArtifactCollected(final int artifactId) {
		// TODO Better data structure so this is less lame.
		Koopa koopa = null;
		for (Koopa candidate : getKoopas()) {
			if (candidate.getArtifactNumber() == artifactId) {
				koopa = candidate;
				break;
			}
		}

		return (koopa != null) && (koopa.getStatus() == Koopa.Status.CARRIED);
	}

	public boolean isArtifactReturned(final int artifactId) {
		final Kiosk kiosk = getKiosk(artifactId);
		return (kiosk != null) && kiosk.isCompleted();
	}

	public boolean areAllArtifactsReturned() {
		for (Kiosk kiosk : getKiosks()) {
			if (!kiosk.isCompleted()) {
				return false;
			}
		}
		return true;
	}

	public void setStartPos(final Position startPos) {
		this.startPos = startPos;
	}

	public void setCompleted(final boolean completed) {
		this.completed = completed;
		this.completionCode |= (1 << COMPLETION_BIT_OFFSET);
	}

	public void setKoopaCompletionFlag(final int artifactId,
			final Koopa.Status status) {
		final int offset = KOOPA_STATUS_WIDTH * artifactId;
		completionCode &= ~(((1 << KOOPA_STATUS_WIDTH) - 1) << offset);
		completionCode |= status.ordinal() << offset;
	}

	public void setHeldArtifactFlag(final int artifactId) {
		completionCode &= ~(((1 << HELD_ARTIFACT_WIDTH) - 1) << HELD_ARTIFACT_BIT_OFFSET);
		completionCode |= artifactId << HELD_ARTIFACT_BIT_OFFSET;
	}

	public void setKioskCompletionFlag(final int artifactId,
			final boolean completed) {
		final int bit = 1 << (artifactId + KIOSK_BIT_OFFSET);
		if (completed) {
			completionCode |= bit;
		} else {
			completionCode &= ~bit;
		}
	}

	public void setYoshiFlag(final boolean gotten) {
		if (gotten) {
			completionCode |= (1 << YOSHI_BIT_OFFSET);
		} else {
			completionCode &= ~(1 << YOSHI_BIT_OFFSET);
		}
	}

	public void setPlayerRoadFlag(final int roadId) {
		completionCode &= ~(((1 << ROAD_WIDTH) - 1) << ROAD_BIT_OFFSET);
		completionCode |= roadId << ROAD_BIT_OFFSET;
	}

	public Road getRoad(final int roadId) {
		return (roads.size() > roadId) ? roads.get(roadId) : null;
	}

	public Collection<Road> getRoads() {
		return roads;
	}

	public Collection<Koopa> getKoopasOnRoad(final int roadId) {
		return (koopasByRoad.size() > roadId) ? koopasByRoad.get(roadId) : null;
	}

	public void addRoad(final Road road) {
		final int roadId = road.getRoadId();
		expandList(roads, roadId + 1);
		roads.set(road.getRoadId(), road);
	}

	public Kiosk getKiosk(final int artifactId) {
		return (kiosks.size() > artifactId) ? kiosks.get(artifactId) : null;
	}

	public Collection<Kiosk> getKiosks() {
		return kiosks;
	}

	public void addKiosk(final Kiosk kiosk) {
		final int kioskId = kiosk.getArtifactId();
		expandList(kiosks, kioskId + 1);
		kiosks.set(kioskId, kiosk);
	}

	public Position getNpcSlot(final int id) {
		return (npcSlots.size() > id) ? npcSlots.get(id) : null;
	}

	public Collection<Position> getNpcSlots() {
		return npcSlots;
	}

	public void addNpcSlot(final int id, final Position npcSlot) {
		expandList(npcSlots, id + 1);
		npcSlots.set(id, npcSlot);
	}

	public Koopa getKoopa(final int koopaId) {
		return (koopas.size() > koopaId) ? koopas.get(koopaId) : null;
	}

	public Koopa getKoopaByArtifact(final int artifactNumber) {
		return (koopasByArtifactNumber.size() > artifactNumber) ? koopasByArtifactNumber
				.get(artifactNumber) : null;
	}

	public Collection<Koopa> getKoopas() {
		return koopas;
	}

	public Collection<Koopa> getArtifactHolders() {
		return koopasByArtifactNumber;
	}

	public void addKoopa(final Koopa koopa) {
		final int koopaId = koopa.getId();
		expandList(koopas, koopaId + 1);
		koopas.set(koopaId, koopa);

		if (koopa.hasArtifact()) {
			final int artifactNumber = koopa.getArtifactNumber();
			expandList(koopasByArtifactNumber, artifactNumber + 1);
			koopasByArtifactNumber.set(artifactNumber, koopa);

			final int roadId = koopa.getStartPos().getRoadIndex();
			expandList(koopasByRoad, roadId + 1);
			List<Koopa> koopasOnRoad = koopasByRoad.get(roadId);
			if (koopasOnRoad == null) {
				koopasOnRoad = new ArrayList<>();
				koopasByRoad.set(roadId, koopasOnRoad);
			}
			koopasOnRoad.add(koopa);
		}
	}

	public void respawn(final SlotRng slotRng) {
		// Throw out the old NPC positions since we're redoing them.
		koopas.clear();

		// We don't track of the NPCs, but the game does, and it advances the
		// RNG for spawning them. Therefore, we have to pretend to spawn them.
		final Set<Integer> occupiedIds = new HashSet<>();
		final Set<Integer> occupiedNpcRoads = new HashSet<>();
		for (int i = 0; i < NUM_NPCS; i++) {
			getNextNpcSlot(occupiedIds, occupiedNpcRoads, slotRng);
		}

		// We also don't track the Koopas that spawn artifacts, but they affect
		// the RNG as well. They follow different spawn logic from the NPCs.
		for (int i = 0; i < NUM_USELESS_KOOPAS; i++) {
			getNextKoopaSlot(occupiedIds, slotRng);
		}

		// Now we can respawn the (useful) koopas.
		for (int i = 0; i < NUM_ARTIFACTS; i++) {
			final int slotId = getNextKoopaSlot(occupiedIds, slotRng);
			final Position pos = npcSlots.get(slotId);

			final Koopa koopa = new Koopa(NUM_NPCS + NUM_USELESS_KOOPAS + i,
					pos, this);
			koopa.setRoad(roads.get(pos.getRoadIndex()));
			koopa.setArtifactNumber(i);
			koopa.setAnimationTime(ANIMATION_TIMES.get(i));
			addKoopa(koopa);
		}

		// Also, any artifacts the player may have collected last time are no
		// longer valid.
		for (Kiosk kiosk : getKiosks()) {
			kiosk.setCompleted(false);
		}

		// And I guess the level can't be complete now, either.
		completed = false;
		completionCode = 0;
	}

	public void advanceNpcs(final long time) {
		for (Koopa koopa : getKoopas()) {
			if (koopa != null) {
				koopa.advance(time);
			}
		}
	}

	public boolean sameState(final Level level) {
		if (completed != level.completed) {
			return false;
		}

		if (kiosks.size() != level.kiosks.size()) {
			return false;
		}
		for (int i = 0; i < kiosks.size(); i++) {
			final Kiosk expectedKiosk = kiosks.get(i);
			if (expectedKiosk == null) {
				continue;
			}

			final Kiosk actualKiosk = level.kiosks.get(i);
			if (expectedKiosk.isCompleted() != actualKiosk.isCompleted()) {
				return false;
			}
		}

		if (koopas.size() != level.koopas.size()) {
			return false;
		}
		for (int i = 0; i < koopas.size(); i++) {
			final Koopa expectedKoopa = koopas.get(i);
			if (expectedKoopa == null) {
				continue;
			}

			final Koopa actualKoopa = level.koopas.get(i);
			if (!expectedKoopa.sameState(actualKoopa)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public String toString() {
		return String.format("Level %d", levelId);
	}

	private int getNextNpcSlot(final Set<Integer> occupiedIds,
			final Set<Integer> occupiedNpcRoads, final SlotRng slotRng) {
		// For each NPC, the game repeatedly chooses a random slot until it gets
		// one that's not already taken and that isn't on the same road as the
		// slot for a previous NPC.
		int slot;
		int road;
		do {
			slot = selectRandomNpcSlot(slotRng);
			road = npcSlots.get(slot).getRoadIndex();
		} while (occupiedIds.contains(slot) || occupiedNpcRoads.contains(road));

		final Position pos = npcSlots.get(slot);
		occupiedIds.add(slot);
		occupiedNpcRoads.add(pos.getRoadIndex());

		return slot;
	}

	private int getNextKoopaSlot(final Set<Integer> occupiedIds,
			final SlotRng slotRng) {
		// The game only looks at which slots have been taken when spawning
		// Koopas.
		int slot;
		do {
			slot = selectRandomNpcSlot(slotRng);
		} while (occupiedIds.contains(slot));

		occupiedIds.add(slot);
		return slot;
	}

	private int selectRandomNpcSlot(final SlotRng slotRng) {
		final int numSlots = npcSlots.size();
		return slotRng.advanceInLevel(numSlots).getSlot(numSlots);
	}

	private <E> void expandList(final List<E> list, final int size) {
		for (int i = list.size(); i < size; i++) {
			list.add(null);
		}
	}

}
