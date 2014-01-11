package io.github.uusdfg.mim.data;

import static org.junit.Assert.assertEquals;
import io.github.uusdfg.mim.data.Koopa;
import io.github.uusdfg.mim.data.Level;
import io.github.uusdfg.mim.data.Position;
import io.github.uusdfg.mim.data.Road;

import org.junit.Test;

/**
 * Tests for how a {@link Koopa} simulates its behavior in a level.
 */
public class KoopaTest {

	private final Level level = new Level(1);

	private final Road road = new Road(level, 1, 1000);

	@Test
	public void testGetTimeToHitboxEntryWhenInHitbox() {
		final Koopa koopa = new Koopa(1, new Position(false, 1, 100), level);
		koopa.setRoad(road);

		final long timeWhenAlignedWithKoopa = koopa.getTimeToHitboxEntry(
				new Position(false, 1, 100), 4);
		assertEquals("Nonzero time when player aligned with Koopa", 0,
				timeWhenAlignedWithKoopa);

		final long timeWhenLeftOfKoopa = koopa.getTimeToHitboxEntry(
				new Position(false, 1, 90), 4);
		assertEquals("Nonzero time when player left of Koopa, but in hitbox",
				0, timeWhenLeftOfKoopa);

		final long timeWhenRightOfKoopa = koopa.getTimeToHitboxEntry(
				new Position(false, 1, 115), 4);
		assertEquals("Nonzero time when player right of Koopa, but in hitbox",
				0, timeWhenRightOfKoopa);
	}

	@Test
	public void testGetTimeToHitboxEntryLeftLeft() {
		final Koopa koopa = new Koopa(1, new Position(true, 1, 100), level);
		koopa.setRoad(road);

		final long time = koopa.getTimeToHitboxEntry(
				new Position(true, 1, 140), 3);
		assertEquals("Wrong time when player and Koopa are both moving left",
				12, time);
	}

	@Test
	public void testGetTimeToHitboxEntryLeftLeftBounce() {
		final Koopa koopa = new Koopa(1, new Position(true, 1, 100), level);
		koopa.setRoad(road);

		final long time = koopa.getTimeToHitboxEntry(
				new Position(true, 1, 800), 4);
		assertEquals(
				"Wrong time when player and Koopa are both moving left and Koopa bounces",
				171, time);
	}

	@Test
	public void testGetTimeToHitboxEntryLeftRight() {
		final Koopa koopa = new Koopa(1, new Position(false, 1, 200), level);
		koopa.setRoad(road);

		final long time = koopa.getTimeToHitboxEntry(
				new Position(true, 1, 600), 1);
		assertEquals(
				"Wrong time when player is moving left and Koopa is moving right",
				192, time);
	}

	@Test
	public void testGetTimeToHitboxEntryRightLeft() {
		final Koopa koopa = new Koopa(1, new Position(true, 1, 200), level);
		koopa.setRoad(road);

		final long time = koopa.getTimeToHitboxEntry(
				new Position(true, 1, 100), 2);
		assertEquals(
				"Wrong time when player is moving right and Koopa is moving left",
				28, time);
	}

	@Test
	public void testGetTimeToHitboxEntryRightRight() {
		final Koopa koopa = new Koopa(1, new Position(false, 1, 575), level);
		koopa.setRoad(road);

		final long time = koopa.getTimeToHitboxEntry(
				new Position(true, 1, 128), 4);
		assertEquals("Wrong time when player and Koopa are both moving right",
				144, time);
	}

	@Test
	public void testGetTimeToHitboxEntryRightRightBounce() {
		final Koopa koopa = new Koopa(1, new Position(false, 1, 907), level);
		koopa.setRoad(road);

		final long time = koopa.getTimeToHitboxEntry(
				new Position(false, 1, 384), 3);
		assertEquals(
				"Wrong time when player and Koopa are both moving right and Koopa bounces",
				174, time);
	}

}
