package com.example.passerwatch.detection

data class BodyPoint(val x: Float, val y: Float, val visibility: Float, val presence: Float)

object FullBodyPolicy {
    private val joints = intArrayOf(11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22,
        23, 24, 25, 26, 27, 28, 29, 30, 31, 32)
    fun accepts(points: List<BodyPoint>, imageAspectRatio: Float = 1f): Boolean {
        if (points.size < 33 || !imageAspectRatio.isFinite() || imageAspectRatio <= 0) return false
        fun inside(point: BodyPoint) = point.x.isFinite() && point.y.isFinite() &&
            point.x in 0.02f..0.98f && point.y in 0.02f..0.98f && point.presence >= 0.6f
        // Head must be in the image; face visibility is intentionally NOT required here.
        if (!inside(points[0]) || !joints.all { inside(points[it]) && points[it].visibility >= 0.6f }) return false
        // Conservative head-room estimate; a nose barely inside the frame is not a full head.
        val shoulderSpan = kotlin.math.hypot(points[11].x - points[12].x,
            (points[11].y - points[12].y) / imageAspectRatio)
        val radiusX = maxOf(0.02f, shoulderSpan * 0.30f)
        val radiusY = radiusX * imageAspectRatio
        val head = points[0]
        return head.x - radiusX >= 0.02f && head.x + radiusX <= 0.98f &&
            head.y - radiusY >= 0.02f && head.y + radiusY <= 0.98f
    }
}
