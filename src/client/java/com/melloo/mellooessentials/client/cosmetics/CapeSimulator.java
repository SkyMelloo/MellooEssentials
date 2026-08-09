package com.melloo.mellooessentials.client.cosmetics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A small Verlet-integrated cloth grid per player, backing the Physics Cape cosmetic - unlike every
 * other cosmetic here (which trace a fixed procedural shape), this one actually simulates: the cloth
 * trails opposite the player's own movement like real wind drag, ripples on its own from an ambient
 * headwind that always blows from the player's own front (only its STRENGTH gusts over time, never
 * its direction - so it's genuinely wavey even standing still without ever swinging the cape to a
 * weird angle), is only mildly buoyant while the player is submerged in water, and rests on the
 * ground instead of clipping through it. Top row is rigidly anchored to the shoulders every tick;
 * everything below is free-simulated.
 */
final class CapeSimulator {
	private static final int COLS = 7;
	private static final int ROWS = 9; // row 0 = anchored at the shoulders, rows 1..8 simulated
	private static final double NODE_SPACING = 0.3; // ROWS * this comfortably exceeds player height, so the hem drapes past the feet
	private static final double GRAVITY = 0.014;
	// Higher damping = more apparent mass/inertia - the cloth resists sudden changes instead of
	// whipping and overshooting, without needing a real per-node mass term.
	private static final double DAMPING = 0.9;
	// How strongly the player's own velocity pushes the cape - kept low so it trails behind
	// naturally instead of flinging violently on every direction change.
	private static final double WIND_DRAG = 0.22;
	private static final double BODY_RADIUS = 0.4; // the wearer's own body, treated as a simple cylinder the cloth can't pass through
	private static final int CONSTRAINT_ITERATIONS = 3;

	private static final class Grid {
		final Vec3[][] pos = new Vec3[ROWS][COLS];
		final Vec3[][] prev = new Vec3[ROWS][COLS];
		boolean initialized = false;
	}

	private static final Map<UUID, Grid> grids = new HashMap<>();

	private CapeSimulator() {
	}

	/** Horizontal rest-length between adjacent columns at row r - wider toward the hem, narrow at the shoulders, the trapezoid silhouette. */
	private static double rowWidth(int r) {
		return NODE_SPACING * (0.5 + (double) r / (ROWS - 1) * 0.9);
	}

	static Vec3[][] tick(Minecraft client, AbstractClientPlayer player) {
		Grid grid = grids.computeIfAbsent(player.getUUID(), id -> new Grid());
		float yaw = player.yBodyRot * (float) (Math.PI / 180F);
		double backX = Math.sin(yaw);
		double backZ = -Math.cos(yaw);
		double sideX = -backZ;
		double sideZ = backX;
		double bodyMid = player.getBbHeight() * 0.85;

		for (int c = 0; c < COLS; c++) {
			double lateral = (c - (COLS - 1) / 2.0) * rowWidth(0);
			double x = player.getX() + backX * 0.15 + sideX * lateral;
			double z = player.getZ() + backZ * 0.15 + sideZ * lateral;
			double y = player.getY() + bodyMid;
			Vec3 anchor = new Vec3(x, y, z);
			grid.pos[0][c] = anchor;
			grid.prev[0][c] = anchor;
		}

		if (!grid.initialized) {
			// First tick this player's had the cape on - lay the rest of the grid out hanging
			// straight down from the anchors rather than starting bunched up at one point.
			for (int r = 1; r < ROWS; r++) {
				for (int c = 0; c < COLS; c++) {
					Vec3 above = grid.pos[r - 1][c];
					Vec3 hang = above.subtract(0, NODE_SPACING, 0);
					grid.pos[r][c] = hang;
					grid.prev[r][c] = hang;
				}
			}
			grid.initialized = true;
			return grid.pos;
		}

		boolean inWater = player.isInWater();
		Vec3 playerVelocity = player.getDeltaMovement();
		// Only the component of velocity along the direction the player is actually FACING pushes the
		// cape - and only ever backward, never toward the front. Using the raw world-space velocity
		// here used to mean flying backward (velocity pointing toward the player's own front) dragged
		// the cape around to their front too, looking like an apron instead of a cape - clamping to
		// >= 0 means moving backward just lets the cape go slack instead of swinging around.
		double forwardSpeed = Math.max(0, -(playerVelocity.x * backX + playerVelocity.z * backZ));
		Vec3 movementWind = new Vec3(backX, 0, backZ).scale(forwardSpeed * WIND_DRAG);

		// Ambient wind - unlike movementWind above (which only ever kicks in while actually moving),
		// this is what makes the cloth wavey even standing still: a real per-tick force, ALWAYS
		// blowing from the same direction as the player's own front (the same "backX/backZ" direction
		// movementWind uses) - deliberately NOT a direction that drifts around over time, since wind
		// that could blow from any angle (including sideways or from behind) swung the cape to weird,
		// unnatural angles instead of just fluttering the way a cape actually looks in a headwind.
		// Only the STRENGTH gusts up and down (two layered, unrelated frequencies so it doesn't feel
		// perfectly periodic), never the direction.
		long gameTime = client.level.getGameTime();
		long windSeed = player.getUUID().hashCode() & 0xFFF; // each cape gusts on its own schedule, not in lockstep
		double windPhase = (gameTime + windSeed) * 0.05;
		double windGust = 0.02 + (Math.sin(windPhase * 0.05) * 0.5 + 0.5) * 0.022
				+ (Math.sin(windPhase * 0.13 + 1.3) * 0.5 + 0.5) * 0.01;
		Vec3 ambientWind = new Vec3(backX, 0, backZ).scale(windGust);

		for (int r = 1; r < ROWS; r++) {
			for (int c = 0; c < COLS; c++) {
				Vec3 current = grid.pos[r][c];
				Vec3 previous = grid.prev[r][c];
				Vec3 velocity = current.subtract(previous).scale(DAMPING);
				double spanFrac = (double) r / (ROWS - 1); // 0 at the shoulders, 1 at the hem
				// A "belly" that peaks in the MIDDLE of the cape's length and tapers off at both the
				// anchored top AND the very hem, instead of growing all the way to the free edge - the
				// wind visibly works the body of the cape, not just flicking the tip.
				double bellyFrac = Math.sin(spanFrac * Math.PI);

				// A genuine traveling wave down and across the cloth (not just a rigid trail) - this is
				// the actual "wavey" flutter, riding on top of the ambient wind's own slow drift.
				double ripplePhase = windPhase * 3.0 - r * 0.9 + c * 0.3;
				double rippleLateral = Math.sin(ripplePhase) * 0.032 * bellyFrac;
				double rippleLift = (Math.sin(ripplePhase * 1.3 + 0.5) * 0.5 + 0.5) * 0.012 * bellyFrac; // mostly upward, gently puffs the cloth up rather than letting it hang totally flat

				// Spread - the cape flares outward from its centerline and settles back, like it's
				// catching a gust and billowing wide, on top of (not instead of) the ripple above.
				double colCenterOffset = c - (COLS - 1) / 2.0;
				double spreadPhase = windPhase * 0.8 + r * 0.4;
				double spreadAmount = (Math.sin(spreadPhase) * 0.5 + 0.5) * 0.024 * bellyFrac;
				double spreadLateral = Math.signum(colCenterOffset) * spreadAmount;

				double totalLateral = rippleLateral + spreadLateral;

				// Only mildly buoyant while submerged, not weightless - a real wet cloth still has
				// some heft to it, it doesn't float like it's in zero gravity.
				double gravity = inWater ? GRAVITY * 0.55 : GRAVITY;
				Vec3 next = current.add(velocity).add(movementWind).add(ambientWind)
						.add(sideX * totalLateral, rippleLift, sideZ * totalLateral)
						.subtract(0, gravity, 0);
				grid.prev[r][c] = current;
				grid.pos[r][c] = next;
			}
		}

		for (int iter = 0; iter < CONSTRAINT_ITERATIONS; iter++) {
			for (int c = 0; c < COLS; c++) {
				for (int r = 1; r < ROWS; r++) {
					satisfyConstraint(grid, r - 1, c, r, c, NODE_SPACING);
				}
			}
			for (int r = 1; r < ROWS; r++) {
				for (int c = 1; c < COLS; c++) {
					satisfyConstraint(grid, r, c - 1, r, c, rowWidth(r));
				}
			}
			// Diagonal (shear) constraints - without these, columns can drift independently and the
			// whole cape visibly twists/shears sideways under directional stress instead of staying a
			// flat, coherent sheet.
			for (int r = 0; r + 1 < ROWS; r++) {
				for (int c = 0; c + 1 < COLS; c++) {
					double diagLen = Math.hypot(NODE_SPACING, rowWidth(r));
					satisfyConstraint(grid, r, c, r + 1, c + 1, diagLen);
					satisfyConstraint(grid, r, c + 1, r + 1, c, diagLen);
				}
			}
		}

		// Ground collision - a node can't sink below the nearest solid surface roughly beneath it, so
		// the cape actually pools/rests on the floor instead of clipping through it. A SOFT correction
		// (easing partway toward the floor each tick, not snapping straight to it) rather than a hard
		// clamp - a hard clamp meant that when a swaying node's XZ drifted back over a ledge after
		// hanging past its edge, it would instantly teleport up onto the surface; easing it in lets the
		// hem drape down over an edge and settle onto a lower surface instead of popping upward.
		for (int r = 1; r < ROWS; r++) {
			for (int c = 0; c < COLS; c++) {
				Vec3 p = grid.pos[r][c];
				double floorY = groundHeightNear(client.level, p.x, p.y, p.z);
				if (p.y < floorY) {
					grid.pos[r][c] = new Vec3(p.x, p.y + (floorY - p.y) * 0.35, p.z);
				}
			}
		}

		resolveBodyCollision(grid, player);

		return grid.pos;
	}

	/** The wearer's own body, treated as a simple cylinder the cloth gets pushed back out of instead of clipping through - most noticeable when wind pushes the cape forward against your own legs/back. */
	private static void resolveBodyCollision(Grid grid, AbstractClientPlayer player) {
		double feetY = player.getY();
		double headY = feetY + player.getBbHeight();
		for (int r = 1; r < ROWS; r++) {
			for (int c = 0; c < COLS; c++) {
				Vec3 p = grid.pos[r][c];
				if (p.y < feetY - 0.2 || p.y > headY + 0.1) {
					continue;
				}
				double dx = p.x - player.getX();
				double dz = p.z - player.getZ();
				double dist = Math.sqrt(dx * dx + dz * dz);
				if (dist > 1.0E-4 && dist < BODY_RADIUS) {
					double push = BODY_RADIUS / dist;
					grid.pos[r][c] = new Vec3(player.getX() + dx * push, p.y, player.getZ() + dz * push);
				}
			}
		}
	}

	/**
	 * Scans straight DOWN from (x,y,z) for the topmost solid surface, using each block's ACTUAL
	 * collision shape rather than assuming every non-air block is a full cube - so a slab, a stair, a
	 * pane, or any other partial-height block gets the cape resting at its real surface height instead
	 * of either floating above it or sinking a half-block into it. Deliberately never looks upward: it
	 * used to start 2 blocks above the query point, which meant a low ceiling right over the player's
	 * head got mistaken for "the ground" and the cape snapped UP onto it instead of hanging down
	 * normally.
	 */
	private static double groundHeightNear(Level level, double x, double y, double z) {
		BlockPos center = BlockPos.containing(x, y, z);
		for (int dy = 0; dy >= -3; dy--) {
			BlockPos check = center.offset(0, dy, 0);
			BlockState state = level.getBlockState(check);
			if (state.isAir()) {
				continue;
			}
			double shapeTop;
			try {
				VoxelShape shape = state.getCollisionShape(level, check);
				if (shape.isEmpty()) {
					// Not actually a solid surface - a tripwire, a rail, an open trapdoor, etc. Keep
					// scanning further down instead of treating it as ground.
					continue;
				}
				// Capped at 1.0 - fences/walls/some fence gates report a collision shape taller than
				// their own block (up to 1.5) for their real player-collision post, which otherwise
				// left the cape resting oddly high above them instead of at a normal "ground" height.
				shapeTop = Math.min(1.0, shape.max(Direction.Axis.Y));
			} catch (Exception ignored) {
				// A small number of blocks (some with unusual/context-dependent shapes) can throw
				// computing their collision shape outside a full collision context - fall back to
				// treating them as a normal full block rather than letting one bad block break the
				// whole cape simulation.
				shapeTop = 1.0;
			}
			return check.getY() + shapeTop + 0.02;
		}
		return y;
	}

	private static void satisfyConstraint(Grid grid, int r1, int c1, int r2, int c2, double restLength) {
		Vec3 a = grid.pos[r1][c1];
		Vec3 b = grid.pos[r2][c2];
		Vec3 delta = b.subtract(a);
		double dist = delta.length();
		if (dist < 1.0E-4) {
			return;
		}
		double diff = (dist - restLength) / dist;
		Vec3 correction = delta.scale(diff * 0.5);
		if (r1 == 0) {
			// The shoulder row is rigidly anchored elsewhere every tick - only the far end may move.
			grid.pos[r2][c2] = b.subtract(correction.scale(2));
		} else {
			grid.pos[r1][c1] = a.add(correction);
			grid.pos[r2][c2] = b.subtract(correction);
		}
	}
}
