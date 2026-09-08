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

// A small Verlet-integrated cloth grid per player, backing the Physics Cape cosmetic. Top row is
// rigidly anchored to the shoulders every tick; everything below is free-simulated.
final class CapeSimulator {
	private static final int COLS = 7;
	private static final int ROWS = 9; // row 0 = anchored at the shoulders, rows 1..8 simulated
	private static final double NODE_SPACING = 0.3; // ROWS * this comfortably exceeds player height, so the hem drapes past the feet
	private static final double GRAVITY = 0.014;
	// Higher = more apparent mass/inertia, resisting sudden changes without a real per-node mass term.
	private static final double DAMPING = 0.9;
	// How strongly the player's velocity pushes the cape - kept low so it trails naturally.
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

	// Horizontal rest-length at row r - wider toward the hem, narrow at the shoulders (trapezoid silhouette).
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
			// First tick with the cape on - lay the grid out hanging straight down instead of bunched up.
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
		// Only the backward-facing component of velocity pushes the cape - clamped to >= 0 so moving
		// backward just lets it go slack instead of swinging around to the front like an apron.
		double forwardSpeed = Math.max(0, -(playerVelocity.x * backX + playerVelocity.z * backZ));
		Vec3 movementWind = new Vec3(backX, 0, backZ).scale(forwardSpeed * WIND_DRAG);

		// Ambient headwind, always from the player's front - only its strength gusts over time, never
		// its direction, so the cape flutters naturally instead of swinging to odd angles.
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
				// Peaks in the middle of the cape's length, tapering at both the anchor and the hem.
				double bellyFrac = Math.sin(spanFrac * Math.PI);

				// A traveling wave down and across the cloth - the actual "wavey" flutter.
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
			// Diagonal (shear) constraints - without these, columns drift independently and the cape twists sideways.
			for (int r = 0; r + 1 < ROWS; r++) {
				for (int c = 0; c + 1 < COLS; c++) {
					double diagLen = Math.hypot(NODE_SPACING, rowWidth(r));
					satisfyConstraint(grid, r, c, r + 1, c + 1, diagLen);
					satisfyConstraint(grid, r, c + 1, r + 1, c, diagLen);
				}
			}
		}

		// Soft correction (eases toward the floor, doesn't snap) so a node drifting back over a ledge
		// settles onto the lower surface instead of teleporting up.
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

	// The wearer's body, treated as a simple cylinder the cloth is pushed back out of.
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

	// Scans straight down for the topmost solid surface, using each block's real collision shape so
	// a slab/stair/pane rests correctly instead of floating or sinking half a block.
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
				// Capped at 1.0 - fences/walls report a collision shape up to 1.5 for their player-collision post.
				shapeTop = Math.min(1.0, shape.max(Direction.Axis.Y));
			} catch (Exception ignored) {
				shapeTop = 1.0; // some blocks throw computing their shape outside a full collision context
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
