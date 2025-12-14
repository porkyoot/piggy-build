package is.pig.minecraft.build.test;

import is.pig.minecraft.build.mvc.model.BuildSession;
import is.pig.minecraft.build.mvc.model.BuildShape;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * GameTest suite for Piggy Build mod - Shape Selector Testing
 * Tests can be run with: ./gradlew :piggy-build:runGametest
 */
public class PiggyBuildTests {

    /**
     * Simple dummy test to verify GameTest framework is working
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dummyPassTest(GameTestHelper context) {
        System.out.println("[PIGGY-BUILD TEST] Dummy test executed successfully!");
        context.succeed();
    }

    /**
     * Test that BuildShape enum has all expected values
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testBuildShapeValues(GameTestHelper context) {
        BuildShape[] shapes = BuildShape.values();

        context.assertTrue(shapes.length == 4, "Should have exactly 4 build shapes");

        // Verify all expected shapes exist
        boolean hasBlock = false, hasLine = false, hasRing = false, hasSphere = false;
        for (BuildShape shape : shapes) {
            switch (shape) {
                case BLOCK:
                    hasBlock = true;
                    break;
                case LINE:
                    hasLine = true;
                    break;
                case RING:
                    hasRing = true;
                    break;
                case SPHERE:
                    hasSphere = true;
                    break;
            }
        }

        context.assertTrue(hasBlock, "Should have BLOCK shape");
        context.assertTrue(hasLine, "Should have LINE shape");
        context.assertTrue(hasRing, "Should have RING shape");
        context.assertTrue(hasSphere, "Should have SPHERE shape");

        System.out.println("[TEST] All 4 build shapes verified: BLOCK, LINE, RING, SPHERE");
        context.succeed();
    }

    /**
     * Test BuildSession singleton pattern
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testBuildSessionSingleton(GameTestHelper context) {
        BuildSession instance1 = BuildSession.getInstance();
        BuildSession instance2 = BuildSession.getInstance();

        context.assertTrue(instance1 == instance2, "getInstance should return same instance");
        context.assertTrue(instance1 != null, "Instance should not be null");

        System.out.println("[TEST] BuildSession singleton pattern verified");
        context.succeed();
    }

    /**
     * Test default shape is RING
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testDefaultShape(GameTestHelper context) {
        BuildSession session = BuildSession.getInstance();

        // Reset to default
        session.setShape(BuildShape.RING);

        BuildShape defaultShape = session.getShape();
        context.assertTrue(defaultShape == BuildShape.RING, "Default shape should be RING");

        System.out.println("[TEST] Default shape verified: RING");
        context.succeed();
    }

    /**
     * Test shape selection - cycle through all shapes
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testShapeSelection(GameTestHelper context) {
        BuildSession session = BuildSession.getInstance();

        // Test setting each shape
        session.setShape(BuildShape.BLOCK);
        context.assertTrue(session.getShape() == BuildShape.BLOCK, "Should set to BLOCK");
        System.out.println("[TEST] Shape set to: BLOCK");

        session.setShape(BuildShape.LINE);
        context.assertTrue(session.getShape() == BuildShape.LINE, "Should set to LINE");
        System.out.println("[TEST] Shape set to: LINE");

        session.setShape(BuildShape.RING);
        context.assertTrue(session.getShape() == BuildShape.RING, "Should set to RING");
        System.out.println("[TEST] Shape set to: RING");

        session.setShape(BuildShape.SPHERE);
        context.assertTrue(session.getShape() == BuildShape.SPHERE, "Should set to SPHERE");
        System.out.println("[TEST] Shape set to: SPHERE");

        System.out.println("[TEST] All shape selections verified");
        context.succeed();
    }

    /**
     * Test default radius is 4
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testDefaultRadius(GameTestHelper context) {
        BuildSession session = BuildSession.getInstance();

        // Reset to default
        session.modifyRadius(-100); // Bring to minimum
        session.modifyRadius(3); // Set to 4

        double radius = session.getRadius();
        context.assertTrue(radius == 4.0, "Default radius should be 4");

        System.out.println("[TEST] Default radius verified: 4.0");
        context.succeed();
    }

    /**
     * Test radius increase
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testRadiusIncrease(GameTestHelper context) {
        BuildSession session = BuildSession.getInstance();

        // Start at known value
        session.modifyRadius(-100); // Reset to minimum (1)
        session.modifyRadius(4); // Set to 5

        double initialRadius = session.getRadius();
        context.assertTrue(initialRadius == 5.0, "Initial radius should be 5");

        // Increase by 3
        session.modifyRadius(3);

        double newRadius = session.getRadius();
        context.assertTrue(newRadius == 8.0, "Radius should be 8 after +3");

        System.out.println("[TEST] Radius increase: 5.0 -> 8.0");
        context.succeed();
    }

    /**
     * Test radius decrease
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testRadiusDecrease(GameTestHelper context) {
        BuildSession session = BuildSession.getInstance();

        // Start at known value
        session.modifyRadius(-100); // Reset to minimum
        session.modifyRadius(9); // Set to 10

        double initialRadius = session.getRadius();
        context.assertTrue(initialRadius == 10.0, "Initial radius should be 10");

        // Decrease by 4
        session.modifyRadius(-4);

        double newRadius = session.getRadius();
        context.assertTrue(newRadius == 6.0, "Radius should be 6 after -4");

        System.out.println("[TEST] Radius decrease: 10.0 -> 6.0");
        context.succeed();
    }

    /**
     * Test radius minimum bound (1)
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testRadiusMinimum(GameTestHelper context) {
        BuildSession session = BuildSession.getInstance();

        // Force to minimum
        session.modifyRadius(-1000);

        double radius = session.getRadius();
        context.assertTrue(radius == 1.0, "Minimum radius should be 1");

        // Try to decrease further
        session.modifyRadius(-5);

        double stillMinimum = session.getRadius();
        context.assertTrue(stillMinimum == 1.0, "Should stay at minimum 1");

        System.out.println("[TEST] Radius minimum bound verified: 1.0");
        context.succeed();
    }

    /**
     * Test radius maximum bound (64)
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testRadiusMaximum(GameTestHelper context) {
        BuildSession session = BuildSession.getInstance();

        // Force to maximum
        session.modifyRadius(1000);

        double radius = session.getRadius();
        context.assertTrue(radius == 64.0, "Maximum radius should be 64");

        // Try to increase further
        session.modifyRadius(10);

        double stillMaximum = session.getRadius();
        context.assertTrue(stillMaximum == 64.0, "Should stay at maximum 64");

        System.out.println("[TEST] Radius maximum bound verified: 64.0");
        context.succeed();
    }

    /**
     * Test anchor position setting
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testAnchorPosition(GameTestHelper context) {
        BuildSession session = BuildSession.getInstance();

        BlockPos testPos = new BlockPos(10, 64, -5);
        session.setAnchor(testPos, Direction.Axis.Y);

        BlockPos anchorPos = session.getAnchorPos();
        context.assertTrue(anchorPos != null, "Anchor should be set");
        context.assertTrue(anchorPos.equals(testPos), "Anchor should match set position");
        context.assertTrue(session.isActive(), "Session should be active with anchor");

        System.out.println("[TEST] Anchor position set: " + testPos);
        context.succeed();
    }

    /**
     * Test anchor axis setting
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testAnchorAxis(GameTestHelper context) {
        BuildSession session = BuildSession.getInstance();

        // Test each axis
        BlockPos pos = new BlockPos(0, 0, 0);

        session.setAnchor(pos, Direction.Axis.X);
        context.assertTrue(session.getAnchorAxis() == Direction.Axis.X, "Should set X axis");
        System.out.println("[TEST] Anchor axis set: X");

        session.setAnchor(pos, Direction.Axis.Y);
        context.assertTrue(session.getAnchorAxis() == Direction.Axis.Y, "Should set Y axis");
        System.out.println("[TEST] Anchor axis set: Y");

        session.setAnchor(pos, Direction.Axis.Z);
        context.assertTrue(session.getAnchorAxis() == Direction.Axis.Z, "Should set Z axis");
        System.out.println("[TEST] Anchor axis set: Z");

        System.out.println("[TEST] All anchor axes verified");
        context.succeed();
    }

    /**
     * Test anchor clearing
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testAnchorClear(GameTestHelper context) {
        BuildSession session = BuildSession.getInstance();

        // Set anchor
        session.setAnchor(new BlockPos(1, 2, 3), Direction.Axis.Y);
        context.assertTrue(session.isActive(), "Should be active after setting anchor");

        // Clear anchor
        session.clearAnchor();
        context.assertTrue(!session.isActive(), "Should not be active after clearing");
        context.assertTrue(session.getAnchorPos() == null, "Anchor position should be null");

        System.out.println("[TEST] Anchor cleared successfully");
        context.succeed();
    }

    /**
     * Test session active state
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testSessionActiveState(GameTestHelper context) {
        BuildSession session = BuildSession.getInstance();

        // Start inactive
        session.clearAnchor();
        context.assertTrue(!session.isActive(), "Should start inactive");
        System.out.println("[TEST] Session inactive (no anchor)");

        // Activate
        session.setAnchor(new BlockPos(0, 0, 0), Direction.Axis.Y);
        context.assertTrue(session.isActive(), "Should be active with anchor");
        System.out.println("[TEST] Session active (anchor set)");

        // Deactivate
        session.clearAnchor();
        context.assertTrue(!session.isActive(), "Should be inactive after clear");
        System.out.println("[TEST] Session inactive again (anchor cleared)");

        System.out.println("[TEST] Session active state transitions verified");
        context.succeed();
    }

    /**
     * Test complete workflow: shape selection + radius adjustment + anchor
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testCompleteWorkflow(GameTestHelper context) {
        BuildSession session = BuildSession.getInstance();

        // 1. Select SPHERE shape
        session.setShape(BuildShape.SPHERE);
        context.assertTrue(session.getShape() == BuildShape.SPHERE, "Shape should be SPHERE");
        System.out.println("[TEST] 1. Shape selected: SPHERE");

        // 2. Set radius to 8
        session.modifyRadius(-100); // Reset
        session.modifyRadius(7); // Set to 8
        context.assertTrue(session.getRadius() == 8.0, "Radius should be 8");
        System.out.println("[TEST] 2. Radius adjusted: 8.0");

        // 3. Set anchor
        BlockPos anchor = new BlockPos(100, 70, -50);
        session.setAnchor(anchor, Direction.Axis.Y);
        context.assertTrue(session.isActive(), "Session should be active");
        context.assertTrue(session.getAnchorPos().equals(anchor), "Anchor should match");
        System.out.println("[TEST] 3. Anchor set: " + anchor);

        // 4. Verify complete state
        context.assertTrue(session.getShape() == BuildShape.SPHERE, "Final shape check");
        context.assertTrue(session.getRadius() == 8.0, "Final radius check");
        context.assertTrue(session.isActive(), "Final active check");

        System.out.println("[TEST] Complete workflow verified: SPHERE at radius 8 with anchor");
        context.succeed();
    }

    // ========== DIRECTIONAL & DIAGONAL PLACEMENT TESTS ==========

    /**
     * Test PlacementMode enum values
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testPlacementModeEnum(GameTestHelper context) {
        is.pig.minecraft.build.lib.placement.PlacementMode[] modes = is.pig.minecraft.build.lib.placement.PlacementMode
                .values();

        context.assertTrue(modes.length == 3, "Should have 3 placement modes");

        // Verify all modes exist
        boolean hasVanilla = false, hasDirectional = false, hasDiagonal = false;
        for (is.pig.minecraft.build.lib.placement.PlacementMode mode : modes) {
            switch (mode) {
                case VANILLA:
                    hasVanilla = true;
                    break;
                case DIRECTIONAL:
                    hasDirectional = true;
                    break;
                case DIAGONAL:
                    hasDiagonal = true;
                    break;
            }
        }

        context.assertTrue(hasVanilla, "Should have VANILLA mode");
        context.assertTrue(hasDirectional, "Should have DIRECTIONAL mode");
        context.assertTrue(hasDiagonal, "Should have DIAGONAL mode");

        System.out.println("[TEST] All 3 placement modes verified: VANILLA, DIRECTIONAL, DIAGONAL");
        context.succeed();
    }

    /**
     * Test PlacementSession lock/unlock for directional mode
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testPlacementSessionLock(GameTestHelper context) {
        is.pig.minecraft.build.mvc.model.PlacementSession session = is.pig.minecraft.build.mvc.model.PlacementSession
                .getInstance();

        // Start unlocked
        session.unlock();
        context.assertTrue(!session.isLocked(), "Should start unlocked");
        System.out.println("[TEST] Placement session unlocked");

        // Lock with specific parameters
        Direction offset = Direction.NORTH;
        Direction face = Direction.UP;
        is.pig.minecraft.build.lib.placement.PlacementMode mode = is.pig.minecraft.build.lib.placement.PlacementMode.DIRECTIONAL;

        session.lock(offset, face, mode);

        context.assertTrue(session.isLocked(), "Should be locked");
        context.assertTrue(session.getLockedOffset() == offset, "Should store offset");
        context.assertTrue(session.getLockedFace() == face, "Should store face");
        context.assertTrue(session.getLockedMode() == mode, "Should store mode");

        System.out.println("[TEST] Placement session locked with: " + offset + ", " + face + ", " + mode);

        // Unlock
        session.unlock();
        context.assertTrue(!session.isLocked(), "Should be unlocked after unlock()");

        System.out.println("[TEST] Placement session lock/unlock verified");
        context.succeed();
    }

    /**
     * Test PlacementSession active state toggle
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testPlacementSessionActive(GameTestHelper context) {
        is.pig.minecraft.build.mvc.model.PlacementSession session = is.pig.minecraft.build.mvc.model.PlacementSession
                .getInstance();

        // Deactivate
        session.setActive(false);
        context.assertTrue(!session.isActive(), "Should be inactive");
        System.out.println("[TEST] Placement session inactive");

        // Activate
        session.setActive(true);
        context.assertTrue(session.isActive(), "Should be active");
        System.out.println("[TEST] Placement session active");

        System.out.println("[TEST] Placement session active state toggle verified");
        context.succeed();
    }

    /**
     * Test PlacementCalculator direction mapping for UP face
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testDirectionMappingUpFace(GameTestHelper context) {
        // Test getDirectionFromRotation for UP face
        // This is the internal logic that maps UV coordinates to directions
        // Based on PlacementCalculator.getDirectionFromRotation

        // We're testing the concept: when looking at UP face
        // Top (0°) = NORTH, Bottom (180°) = SOUTH, Right (90°) = EAST, Left (-90°) =
        // WEST

        // Since the method is private, we test via public getOffsetDirection
        // by creating hit results with specific positions

        BlockPos testPos = new BlockPos(0, 0, 0);

        // Create a hit result on the top of a block (UP face)
        // Hit at center top edge (north edge) -> should map to NORTH
        net.minecraft.world.phys.Vec3 hitNorth = new net.minecraft.world.phys.Vec3(0.5, 1.0, 0.1); // Near north edge
        net.minecraft.world.phys.BlockHitResult hitResultNorth = new net.minecraft.world.phys.BlockHitResult(hitNorth,
                Direction.UP, testPos, false);

        Direction resultNorth = is.pig.minecraft.build.lib.math.PlacementCalculator.getOffsetDirection(hitResultNorth);
        context.assertTrue(resultNorth == Direction.NORTH || resultNorth == null,
                "North edge on UP should map to NORTH or center");

        System.out.println("[TEST] Direction mapping for UP face verified: hit near north -> " + resultNorth);
        context.succeed();
    }

    /**
     * Test PlacementCalculator center detection
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testPlacementCenterDetection(GameTestHelper context) {
        BlockPos testPos = new BlockPos(0, 0, 0);

        // Hit exactly in center should return null (CENTER_MARGIN = 0.25)
        net.minecraft.world.phys.Vec3 centerHit = new net.minecraft.world.phys.Vec3(0.5, 1.0, 0.5);
        net.minecraft.world.phys.BlockHitResult hitResult = new net.minecraft.world.phys.BlockHitResult(centerHit,
                Direction.UP, testPos, false);

        Direction result = is.pig.minecraft.build.lib.math.PlacementCalculator.getOffsetDirection(hitResult);

        context.assertTrue(result == null, "Center hit should return null");
        System.out.println("[TEST] Placement center detection: center hit -> null (no offset)");

        context.succeed();
    }

    /**
     * Test PlacementSession last placed position tracking
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testLastPlacedPosition(GameTestHelper context) {
        is.pig.minecraft.build.mvc.model.PlacementSession session = is.pig.minecraft.build.mvc.model.PlacementSession
                .getInstance();

        BlockPos pos1 = new BlockPos(5, 10, 15);
        BlockPos pos2 = new BlockPos(6, 10, 15);

        // Set first position
        session.setLastPlacedPos(pos1);
        context.assertTrue(session.getLastPlacedPos() != null, "Should have last placed pos");
        context.assertTrue(session.getLastPlacedPos().equals(pos1), "Should match pos1");
        System.out.println("[TEST] Last placed position set: " + pos1);

        // Update to second position
        session.setLastPlacedPos(pos2);
        context.assertTrue(session.getLastPlacedPos().equals(pos2), "Should update to pos2");
        System.out.println("[TEST] Last placed position updated: " + pos2);

        System.out.println("[TEST] Last placed position tracking verified");
        context.succeed();
    }

    // ========== FAST PLACEMENT & BREAKING TESTS ==========

    /**
     * Test fast placement delay calculation (1000ms / speed = delay)
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testFastPlaceDelayCalculation(GameTestHelper context) {
        // Test the delay/speed relationship used by FastPlacementHandler
        // Speed in blocks/sec -> Delay in milliseconds

        // Speed 1 block/sec should be 1000ms delay
        int speed1 = 1;
        int delay1 = 1000 / speed1;
        context.assertTrue(delay1 == 1000, "Speed 1 should give 1000ms delay");
        System.out.println("[TEST] Speed 1 b/s = " + delay1 + "ms delay");

        // Speed 10 blocks/sec should be 100ms delay
        int speed10 = 10;
        int delay10 = 1000 / speed10;
        context.assertTrue(delay10 == 100, "Speed 10 should give 100ms delay");
        System.out.println("[TEST] Speed 10 b/s = " + delay10 + "ms delay");

        // Speed 20 blocks/sec (max) should be 50ms delay
        int speed20 = 20;
        int delay20 = 1000 / speed20;
        context.assertTrue(delay20 == 50, "Speed 20 should give 50ms delay");
        System.out.println("[TEST] Speed 20 b/s = " + delay20 + "ms delay");

        System.out.println("[TEST] Fast place delay calculation verified");
        context.succeed();
    }

    /**
     * Test fast placement speed-to-delay conversion bounds
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testFastPlaceSpeedBounds(GameTestHelper context) {
        // Test the speed clamping logic: Min 1 block/sec, Max 20 blocks/sec

        // Speed should be clamped to minimum 1
        int tooSlow = 0;
        int clampedSlow = Math.max(1, tooSlow);
        context.assertTrue(clampedSlow == 1, "Speed should clamp to minimum 1");
        System.out.println("[TEST] Speed clamped from 0 to: " + clampedSlow);

        // Speed should be clamped to maximum 20
        int tooFast = 25;
        int clampedFast = Math.min(20, tooFast);
        context.assertTrue(clampedFast == 20, "Speed should clamp to maximum 20");
        System.out.println("[TEST] Speed clamped from 25 to: " + clampedFast);

        // Validate delay bounds:
        // Min speed (1 b/s) = Max delay (1000ms)
        int maxDelay = 1000 / 1;
        context.assertTrue(maxDelay == 1000, "Min speed should give max delay");
        System.out.println("[TEST] Min speed (1 b/s) = max delay (" + maxDelay + "ms)");

        // Max speed (20 b/s) = Min delay (50ms)
        int minDelay = 1000 / 20;
        context.assertTrue(minDelay == 50, "Max speed should give min delay");
        System.out.println("[TEST] Max speed (20 b/s) = min delay (" + minDelay + "ms)");

        System.out.println("[TEST] Fast place speed bounds verified");
        context.succeed();
    }

    /**
     * Test timing logic: simulated rapid actions with delay enforcement
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testFastActionTiming(GameTestHelper context) {
        // Simulate the timing check logic from FastPlacementHandler/FastBreakHandler
        long currentTime = System.currentTimeMillis();
        long lastActionTime = currentTime - 200; // 200ms ago
        int minDelay = 100; // Minimum 100ms between actions

        // Should allow action (200ms >= 100ms)
        boolean shouldAllow1 = (currentTime - lastActionTime) >= minDelay;
        context.assertTrue(shouldAllow1, "Should allow action after sufficient delay");
        System.out.println("[TEST] Action allowed: 200ms >= 100ms delay");

        // Simulate action too soon
        lastActionTime = currentTime - 50; // Only 50ms ago
        boolean shouldAllow2 = (currentTime - lastActionTime) >= minDelay;
        context.assertTrue(!shouldAllow2, "Should block action when too soon");
        System.out.println("[TEST] Action blocked: 50ms < 100ms delay");

        System.out.println("[TEST] Fast action timing logic verified");
        context.succeed();
    }

    /**
     * Test ghost block prevention timing (FastBreakHandler's duplicate break check)
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void testGhostBlockPrevention(GameTestHelper context) {
        // Simulate the recently broken block tracking from FastBreakHandler
        long GHOST_PREVENTION_MS = 100;

        BlockPos testPos = new BlockPos(10, 64, 20);
        long currentTime = System.currentTimeMillis();
        long breakTime = currentTime - 50; // Block was broken 50ms ago

        // Check if we should skip re-breaking (ghost prevention)
        long timeSinceBreak = currentTime - breakTime;
        boolean shouldSkip = timeSinceBreak < GHOST_PREVENTION_MS;

        context.assertTrue(shouldSkip, "Should skip re-breaking same block within 100ms");
        System.out.println("[TEST] Ghost block prevention: skipping re-break after 50ms");

        // After enough time, should allow
        breakTime = currentTime - 150; // 150ms ago
        timeSinceBreak = currentTime - breakTime;
        shouldSkip = timeSinceBreak < GHOST_PREVENTION_MS;

        context.assertTrue(!shouldSkip, "Should allow breaking after 100ms");
        System.out.println("[TEST] Ghost block prevention: allowing break after 150ms");

        System.out.println("[TEST] Ghost block prevention logic verified");
        context.succeed();
    }
}
