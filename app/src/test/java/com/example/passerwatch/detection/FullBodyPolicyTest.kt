package com.example.passerwatch.detection

import org.junit.Assert.*
import org.junit.Test

class FullBodyPolicyTest {
    private fun body() = MutableList(33) { BodyPoint(0.5f, 0.5f, 0.9f, 0.9f) }.apply {
        this[0] = BodyPoint(0.5f, 0.1f, 0.2f, 0.9f) // face may be blurred
        this[27] = BodyPoint(0.4f, 0.85f, 0.9f, 0.9f)
        this[28] = BodyPoint(0.6f, 0.85f, 0.9f, 0.9f)
        this[31] = BodyPoint(0.4f, 0.9f, 0.9f, 0.9f)
        this[32] = BodyPoint(0.6f, 0.9f, 0.9f, 0.9f)
    }
    @Test fun wholeBodyCanQualifyWithBlurredFace() { assertTrue(FullBodyPolicy.accepts(body())) }
    @Test fun missingFeetCannotQualify() {
        val points = body(); points[32] = points[32].copy(visibility = 0.1f)
        assertFalse(FullBodyPolicy.accepts(points))
    }
    @Test fun armsOutsideFrameOrHiddenCannotQualify() {
        val cropped = body(); cropped[15] = cropped[15].copy(x = -0.1f)
        assertFalse(FullBodyPolicy.accepts(cropped))
        val hidden = body(); hidden[13] = hidden[13].copy(visibility = 0.2f)
        assertFalse(FullBodyPolicy.accepts(hidden))
    }
    @Test fun croppedHeadOrFeetCannotQualify() {
        val points = body(); points[0] = points[0].copy(y = -0.1f)
        assertFalse(FullBodyPolicy.accepts(points))
        val other = body(); other[31] = other[31].copy(y = 1.1f)
        assertFalse(FullBodyPolicy.accepts(other))
    }
    @Test fun noseInsideButInsufficientRoomForHeadCannotQualify() {
        val points = body()
        points[11] = points[11].copy(x = 0.3f)
        points[12] = points[12].copy(x = 0.7f)
        points[0] = points[0].copy(y = 0.05f)
        assertFalse(FullBodyPolicy.accepts(points))
    }
    @Test fun invisibleJointsAndMissingPoseCannotQualify() {
        assertFalse(FullBodyPolicy.accepts(emptyList()))
        val points = body(); points[23] = points[23].copy(presence = 0.2f)
        assertFalse(FullBodyPolicy.accepts(points))
    }
    @Test fun nonFiniteCoordinatesCannotQualify() {
        val points = body(); points[25] = points[25].copy(x = Float.NaN)
        assertFalse(FullBodyPolicy.accepts(points))
    }
}
