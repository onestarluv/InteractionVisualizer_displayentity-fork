package com.loohp.interactionvisualizer.nms;

import org.bukkit.util.EulerAngle;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DisplayTransformsTest {
    private static void vector(Vector3f actual, float x, float y, float z) {
        assertEquals(x, actual.x, 0.00001f);
        assertEquals(y, actual.y, 0.00001f);
        assertEquals(z, actual.z, 0.00001f);
    }

    @Test void neutralHandIsBelowRightShoulder() {
        vector(DisplayTransforms.hand(0, EulerAngle.ZERO, 1), -0.3125f, 0.75f, 0);
    }

    @Test void smallStandScalesThePivotAndArmTogether() {
        vector(DisplayTransforms.hand(0, EulerAngle.ZERO, 0.5f), -0.15625f, 0.375f, 0);
    }

    @Test void worldYawRotatesAroundStandOrigin() {
        vector(DisplayTransforms.hand(90, EulerAngle.ZERO, 1), 0, 0.75f, -0.3125f);
    }

    @Test void poseUsesRadiansAndPreservesArmLength() {
        vector(DisplayTransforms.hand(0, new EulerAngle(Math.PI / 2, 0, 0), 1), -0.3125f, 1.375f, -0.625f);
    }

    @Test void craftingModesConvergeOnTheSameGridCenter() {
        String[] modes = {"Item", "Block", "LowBlock", "Tool", "Standing"};
        Vector3f[] modeOffsets = {new Vector3f(), new Vector3f(.102f, .084f, .14f),
                new Vector3f(.09f, .02f, .15f), new Vector3f(-.3f, -.26f, -.1f),
                new Vector3f(-.323f, -.32f, .115f)};
        for (float yaw : new float[] {0, 90, 180, 270}) {
            for (int i = 0; i < modes.length; i++) {
                Vector3f legacy = new Vector3f(0, 0, .19f).rotateY((float) Math.toRadians(100))
                        .add(0, 0, -.11f).add(modeOffsets[i]);
                DisplayTransforms.yaw(yaw).transform(legacy);
                float sourceYaw = yaw + (i == 1 || i == 2 ? 45 : 0);
                legacy.add(DisplayTransforms.craftingOffset(sourceYaw, modes[i]));
                vector(legacy, 0, 1.10f - .600781f, 0);
            }
        }
    }

    @Test void craftingSlotSpacingSurvivesRendering() {
        Vector3f offset = DisplayTransforms.craftingOffset(90, "Item");
        Vector3f left = new Vector3f(-.2f, 0, 0).add(offset);
        Vector3f right = new Vector3f(.2f, 0, 0).add(offset);
        vector(right.sub(left), .4f, 0, 0);
    }

    @Test void anvilModesAlignBothAngledSlotsForEveryCardinalDirection() {
        String[] modes = {"Item", "Block", "LowBlock", "Tool", "Standing"};
        Vector3f[] deltas = {new Vector3f(), new Vector3f(.102f, .084f, .14f),
                new Vector3f(.09f, .02f, .15f), new Vector3f(-.3f, -.26f, -.1f),
                new Vector3f(-.323f, -.32f, .115f)};
        for (float tableYaw : new float[] {-180, -90, 0, 90, 180, 270}) {
            for (float angle : new float[] {-20, 20}) {
                float standYaw = tableYaw + angle;
                for (int i = 0; i < modes.length; i++) {
                    Vector3f legacy = new Vector3f(0, 0, .19f).rotateY((float) Math.toRadians(100)).add(0, 0, -.11f);
                    DisplayTransforms.yaw(tableYaw).transform(legacy);
                    legacy.add(DisplayTransforms.yaw(standYaw).transform(new Vector3f(deltas[i])));
                    float sourceYaw = standYaw + (i == 1 || i == 2 ? 45 : 0);
                    legacy.add(DisplayTransforms.anvilOffset(sourceYaw, modes[i]));
                    vector(legacy, 0, 1.14f - .600781f, 0);
                }
            }
        }
    }

    @Test void grindstoneKeepsToolHeightAndAlignsBooksAtBothSlots() {
        EulerAngle toolPose = new EulerAngle(357.99, 0, 300);
        for (float tableYaw : new float[] {-180, -90, 0, 90, 180, 270}) {
            for (float angle : new float[] {-20, 20}) {
                float standYaw = tableYaw + angle;
                Vector3f tool = DisplayTransforms.grindstoneOffset(standYaw, "Tool", .5f);
                Vector3f book = DisplayTransforms.grindstoneOffset(standYaw, "Item", .5f);
                float originalToolY = DisplayTransforms.hand(standYaw, toolPose, .5f).y;
                assertEquals(originalToolY, tool.y, .00001f, "tool height must not change");
                assertEquals(tool.y - .26f, book.y, .00001f, "book must compensate for the tool mode's -0.26 displacement");
                Vector3f legacyCompensation = new Vector3f(0, 0, .19f).rotateY((float) Math.toRadians(100)).add(0, 0, -.11f);
                DisplayTransforms.yaw(tableYaw).transform(legacyCompensation);
                Vector3f bookPosition = new Vector3f(legacyCompensation).add(book);
                Vector3f toolPosition = new Vector3f(legacyCompensation)
                        .add(DisplayTransforms.yaw(standYaw).transform(new Vector3f(-.3f, -.26f, -.1f))).add(tool);
                vector(bookPosition, 0, book.y, 0);
                vector(toolPosition, 0, book.y, 0);
            }
        }
    }
}
