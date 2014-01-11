package io.github.uusdfg.mim.data.loader;

import io.github.uusdfg.mim.data.Crossing;
import io.github.uusdfg.mim.data.Exit;
import io.github.uusdfg.mim.data.Pipe;
import io.github.uusdfg.mim.data.Position;
import io.github.uusdfg.mim.data.Transition;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.EndianUtils;
import org.apache.commons.io.IOUtils;

final class RoadNode {

	public static final int NO_TRANSITION_ID = 0xff;

	public static final int EXIT_NODE_ID = 0;

	private static final int LEFT_TRANSITION_OFFSET = -48;

	private static final int RIGHT_TRANSITION_OFFSET = 48;

	private final int nodeId;

	private int leftId;

	private int rightId;

	private int upCrossingId;

	private int downCrossingId;

	private int pipeId;

	private int leftDistance;

	private int rightDistance;

	public RoadNode(final int nodeId) {
		this.nodeId = nodeId;
	}

	public boolean isPipe() {
		return (pipeId != NO_TRANSITION_ID);
	}

	public boolean isUpCrossing() {
		return (upCrossingId != NO_TRANSITION_ID);
	}

	public boolean isDownCrossing() {
		return (downCrossingId != NO_TRANSITION_ID);
	}

	public boolean isCrossing() {
		return (isUpCrossing() || isDownCrossing());
	}

	public boolean isLeftEnd() {
		return (leftId == NO_TRANSITION_ID);
	}

	public boolean isRightEnd() {
		return (rightId == NO_TRANSITION_ID);
	}

	public boolean isExit() {
		return (nodeId == EXIT_NODE_ID);
	}

	public int getNodeId() {
		return nodeId;
	}

	public int getLeftId() {
		return leftId;
	}

	public int getRightId() {
		return rightId;
	}

	public int getUpCrossingId() {
		return upCrossingId;
	}

	public int getDownCrossingId() {
		return downCrossingId;
	}

	public int getPipeId() {
		return pipeId;
	}

	public int getLeftDistance() {
		return leftDistance;
	}

	public int getRightDistance() {
		return rightDistance;
	}

	public void setLeftId(final int leftId) {
		this.leftId = leftId;
	}

	public void setRightId(final int rightId) {
		this.rightId = rightId;
	}

	public void setUpCrossingId(final int upCrossingId) {
		this.upCrossingId = upCrossingId;
	}

	public void setDownCrossingId(final int downCrossingId) {
		this.downCrossingId = downCrossingId;
	}

	public void setPipeId(final int pipeId) {
		this.pipeId = pipeId;
	}

	public void setLeftDistance(final int leftDistance) {
		this.leftDistance = leftDistance;
	}

	public void setRightDistance(final int rightDistance) {
		this.rightDistance = rightDistance;
	}

	public void loadIds(final InputStream in) throws IOException {
		upCrossingId = in.read();
		rightId = in.read();
		downCrossingId = in.read();
		leftId = in.read();
		pipeId = in.read();
	}

	public void loadDistances(final InputStream in) throws IOException {
		IOUtils.skip(in, 2);
		rightDistance = EndianUtils.readSwappedUnsignedShort(in) * 16;
		IOUtils.skip(in, 2);
		leftDistance = EndianUtils.readSwappedUnsignedShort(in) * 16;
	}

	@Override
	public String toString() {
		return String.format("Node %d [< %d,%d; > %d,%d; ^ %d; \\/ %d; || %d]",
				nodeId, leftId, leftDistance, rightId, rightDistance,
				upCrossingId, downCrossingId, pipeId);
	}

	public List<Transition> toTransitionList(
			final Map<Integer, Position> transitionPositions,
			final Position exitPosition) {
		final Position srcPos = transitionPositions.get(nodeId);
		final List<Transition> transitions = new ArrayList<>();

		if (isPipe()) {
			transitions.add(new Pipe(srcPos, transitionPositions.get(pipeId)));
		}

		if (isUpCrossing()) {
			final Position baseDestPos = transitionPositions.get(upCrossingId);
			transitions.add(new Crossing(srcPos
					.plusXOffset(LEFT_TRANSITION_OFFSET), baseDestPos
					.plusXOffset(LEFT_TRANSITION_OFFSET), true, true));
			transitions.add(new Crossing(srcPos
					.plusXOffset(RIGHT_TRANSITION_OFFSET), baseDestPos
					.plusXOffset(RIGHT_TRANSITION_OFFSET), false, true));
		}

		if (isDownCrossing()) {
			final Position baseDestPos = transitionPositions
					.get(downCrossingId);
			transitions.add(new Crossing(srcPos
					.plusXOffset(LEFT_TRANSITION_OFFSET), baseDestPos
					.plusXOffset(LEFT_TRANSITION_OFFSET), true, false));
			transitions.add(new Crossing(srcPos
					.plusXOffset(RIGHT_TRANSITION_OFFSET), baseDestPos
					.plusXOffset(RIGHT_TRANSITION_OFFSET), false, false));
		}

		if (isExit()) {
			transitions.add(new Exit(srcPos, exitPosition));
		}

		return transitions;
	}
}
