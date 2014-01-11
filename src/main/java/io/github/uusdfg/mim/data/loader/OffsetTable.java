package io.github.uusdfg.mim.data.loader;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.EndianUtils;
import org.apache.commons.io.IOUtils;

class OffsetTable {

	private static final int BASE_TABLE_OFFSET = 0x13000;

	private static final int DEFAULT_KIOSK_XS = 0x10883;

	private static final int DEFAULT_KIOSK_ROADS = 0x10c19;

	private static final int DEFAULT_KIOSK_ARTIFACTS = 0x13022;

	private static final int DEFAULT_NUM_ARTIFACTS = 3;

	private static final int DEFAULT_NUM_NON_ARTIFACT_NPCS = 18;

	private int numSlots; // 0x00

	private int roadIndices; // 0x1c

	private int npcXs; // 0x18

	private int transitionLinks; // 0x0e

	private int transitionDistances; // 0x10

	private int roadWidths; // 0x16

	private int kioskXs = DEFAULT_KIOSK_XS;

	private int kioskRoads = DEFAULT_KIOSK_ROADS;

	private int kioskArtifacts = DEFAULT_KIOSK_ARTIFACTS;

	private int numArtifacts = DEFAULT_NUM_ARTIFACTS;

	private int numNonArtifactNpcs = DEFAULT_NUM_NON_ARTIFACT_NPCS;

	public final int getNumSlots() {
		return numSlots;
	}

	public final int getRoadIndices() {
		return roadIndices;
	}

	public final int getNpcXs() {
		return npcXs;
	}

	public final int getTransitionLinks() {
		return transitionLinks;
	}

	public final int getTransitionDistances() {
		return transitionDistances;
	}

	public final int getRoadWidths() {
		return roadWidths;
	}

	public final int getKioskXs() {
		return kioskXs;
	}

	public final int getKioskRoads() {
		return kioskRoads;
	}

	public final int getKioskArtifacts() {
		return kioskArtifacts;
	}

	public final int getNumArtifacts() {
		return numArtifacts;
	}

	public final int getNumNonArtifactNpcs() {
		return numNonArtifactNpcs;
	}

	public final void setNumSlots(final int numSlots) {
		this.numSlots = numSlots;
	}

	public final void setRoadIndices(final int roadIndices) {
		this.roadIndices = roadIndices;
	}

	public final void setNpcXs(final int npcXs) {
		this.npcXs = npcXs;
	}

	public final void setTransitionLinks(final int transitionLinks) {
		this.transitionLinks = transitionLinks;
	}

	public final void setTransitionDistances(final int transitionDistances) {
		this.transitionDistances = transitionDistances;
	}

	public final void setRoadWidths(final int roadWidths) {
		this.roadWidths = roadWidths;
	}

	public final void setKioskXs(final int kioskXs) {
		this.kioskXs = kioskXs;
	}

	public final void setKioskRoads(final int kioskRoads) {
		this.kioskRoads = kioskRoads;
	}

	public final void setKioskArtifacts(final int kioskArtifacts) {
		this.kioskArtifacts = kioskArtifacts;
	}

	public final void setNumArtifacts(final int numArtifacts) {
		this.numArtifacts = numArtifacts;
	}

	public final void setNumNonArtifactNpcs(final int numNonArtifactNpcs) {
		this.numNonArtifactNpcs = numNonArtifactNpcs;
	}

	public OffsetTable load(final InputStream in) throws IOException {
		IOUtils.skip(in, BASE_TABLE_OFFSET);

		numSlots = EndianUtils.readSwappedUnsignedShort(in);

		IOUtils.skip(in, 12);
		transitionLinks = EndianUtils.readSwappedUnsignedShort(in) + 0x10000;
		transitionDistances = EndianUtils.readSwappedUnsignedShort(in) + 0x10000;

		IOUtils.skip(in, 4);
		roadWidths = EndianUtils.readSwappedUnsignedShort(in) + 0x10000;
		npcXs = EndianUtils.readSwappedUnsignedShort(in) + 0x10000;

		IOUtils.skip(in, 2);
		roadIndices = EndianUtils.readSwappedUnsignedShort(in) + 0x10000;

		kioskXs = DEFAULT_KIOSK_XS;
		kioskRoads = DEFAULT_KIOSK_ROADS;
		kioskArtifacts = DEFAULT_KIOSK_ARTIFACTS;

		numArtifacts = DEFAULT_NUM_ARTIFACTS;
		numNonArtifactNpcs = DEFAULT_NUM_NON_ARTIFACT_NPCS;

		return this;
	}
}
