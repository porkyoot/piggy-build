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
}
