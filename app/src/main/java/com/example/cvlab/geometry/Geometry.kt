package com.example.cvlab.geometry

import kotlin.math.hypot
import kotlin.math.sqrt

// ---------------------------------------------------------------------------
// 2-D Vector
// ---------------------------------------------------------------------------

/**
 * Immutable 2-D vector / point.
 *
 * All arithmetic is inline-class free and safe for both simulator and unit-test
 * contexts (no Android dependencies).
 */
data class Vec2(val x: Float, val y: Float) {

    operator fun plus(other: Vec2)  = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vec2(x * scalar, y * scalar)
    operator fun div(scalar: Float)   = Vec2(x / scalar, y / scalar)
    operator fun unaryMinus()         = Vec2(-x, -y)

    /** Euclidean distance to [other]. */
    fun distanceTo(other: Vec2): Float = hypot(x - other.x, y - other.y)

    /** Squared distance (avoids sqrt, good for comparisons). */
    fun distanceSquaredTo(other: Vec2): Float {
        val dx = x - other.x
        val dy = y - other.y
        return dx * dx + dy * dy
    }

    /** Vector magnitude. */
    fun magnitude(): Float = hypot(x, y)

    /** Normalized (unit) vector, or zero-vector when magnitude is 0. */
    fun normalized(): Vec2 {
        val m = magnitude()
        return if (m == 0f) Vec2(0f, 0f) else this / m
    }

    /** Dot product. */
    fun dot(other: Vec2): Float = x * other.x + y * other.y

    /** Linear interpolation toward [other] by factor [t] ∈ [0,1]. */
    fun lerp(other: Vec2, t: Float): Vec2 = Vec2(
        x + (other.x - x) * t,
        y + (other.y - y) * t
    )

    companion object {
        val Zero = Vec2(0f, 0f)
    }
}

// ---------------------------------------------------------------------------
// Axis-aligned bounding box
// ---------------------------------------------------------------------------

/**
 * Axis-aligned bounding box defined by its [topLeft] corner, [width] and [height].
 */
data class BoundingBox(
    val topLeft: Vec2,
    val width: Float,
    val height: Float,
) {
    val left:   Float get() = topLeft.x
    val top:    Float get() = topLeft.y
    val right:  Float get() = topLeft.x + width
    val bottom: Float get() = topLeft.y + height
    val center: Vec2  get() = Vec2(left + width / 2f, top + height / 2f)
    val area:   Float get() = width * height

    /** True when [point] lies inside this box (inclusive). */
    fun contains(point: Vec2): Boolean =
        point.x >= left && point.x <= right && point.y >= top && point.y <= bottom

    /** True when [other] overlaps this box. */
    fun overlaps(other: BoundingBox): Boolean =
        left < other.right && right > other.left && top < other.bottom && bottom > other.top

    /** Intersection bounding box, or null when boxes do not overlap. */
    fun intersection(other: BoundingBox): BoundingBox? {
        val il = maxOf(left, other.left)
        val it = maxOf(top, other.top)
        val ir = minOf(right, other.right)
        val ib = minOf(bottom, other.bottom)
        return if (ir > il && ib > it) BoundingBox(Vec2(il, it), ir - il, ib - it) else null
    }

    /**
     * Intersection-over-Union with [other].
     * Returns 0 when neither box has positive area.
     */
    fun iou(other: BoundingBox): Float {
        val inter = intersection(other)?.area ?: 0f
        val union = area + other.area - inter
        return if (union <= 0f) 0f else inter / union
    }

    /** Scale this box by [sx] horizontally and [sy] vertically. */
    fun scale(sx: Float, sy: Float): BoundingBox = BoundingBox(
        Vec2(left * sx, top * sy), width * sx, height * sy
    )

    companion object {
        /** Build a bounding box from centre coordinates. */
        fun fromCenter(center: Vec2, w: Float, h: Float) =
            BoundingBox(Vec2(center.x - w / 2f, center.y - h / 2f), w, h)
    }
}

// ---------------------------------------------------------------------------
// Coordinate transformations
// ---------------------------------------------------------------------------

/**
 * Normalises a [point] from [imageWidth]×[imageHeight] image-space to [0,1]×[0,1].
 * Clamps the result to [0,1].
 */
fun normalizePoint(point: Vec2, imageWidth: Float, imageHeight: Float): Vec2 {
    require(imageWidth > 0f && imageHeight > 0f) { "Image dimensions must be positive" }
    return Vec2(
        (point.x / imageWidth).coerceIn(0f, 1f),
        (point.y / imageHeight).coerceIn(0f, 1f),
    )
}

/**
 * Converts a normalised point (values in [0,1]) back to pixel space given
 * [displayWidth] × [displayHeight].
 */
fun denormalizePoint(normalizedPoint: Vec2, displayWidth: Float, displayHeight: Float): Vec2 {
    require(displayWidth > 0f && displayHeight > 0f) { "Display dimensions must be positive" }
    return Vec2(normalizedPoint.x * displayWidth, normalizedPoint.y * displayHeight)
}

/**
 * Scales a bounding box from a source image space to a target display space.
 *
 * Useful for mapping model output coordinates (image pixels) to on-screen
 * positions after letterboxing / pillarboxing.
 *
 * @param box           Box in [srcW]×[srcH] space.
 * @param srcW          Source image width.
 * @param srcH          Source image height.
 * @param dstW          Target display width.
 * @param dstH          Target display height.
 * @param keepAspect    When true, adds letterbox/pillarbox offsets.
 */
fun scaleBox(
    box: BoundingBox,
    srcW: Float, srcH: Float,
    dstW: Float, dstH: Float,
    keepAspect: Boolean = false,
): BoundingBox {
    require(srcW > 0f && srcH > 0f && dstW > 0f && dstH > 0f) { "All dimensions must be positive" }
    return if (!keepAspect) {
        box.scale(dstW / srcW, dstH / srcH)
    } else {
        val scale = minOf(dstW / srcW, dstH / srcH)
        val offsetX = (dstW - srcW * scale) / 2f
        val offsetY = (dstH - srcH * scale) / 2f
        BoundingBox(
            Vec2(box.left * scale + offsetX, box.top * scale + offsetY),
            box.width * scale,
            box.height * scale,
        )
    }
}

// ---------------------------------------------------------------------------
// Circular hitbox helpers
// ---------------------------------------------------------------------------

/**
 * Tests whether [point] is inside a circle centred at [center] with [radius].
 */
fun circleContains(center: Vec2, radius: Float, point: Vec2): Boolean =
    point.distanceSquaredTo(center) <= radius * radius

/**
 * Tests whether a point is inside a composite character hitbox made of a
 * circular body and a circular head positioned above the body.
 *
 * @param bodyCenter  Centre of the body circle.
 * @param bodyRadius  Radius of the body circle.
 * @param headRadius  Radius of the head circle.
 * @return            [HitZone.HEAD], [HitZone.BODY], or [HitZone.NONE].
 */
fun classifyCircularHit(
    point: Vec2,
    bodyCenter: Vec2,
    bodyRadius: Float,
    headRadius: Float,
): HitZone {
    // Head is centred above the body's centre by 75 % of the body radius.
    val headCenter = Vec2(bodyCenter.x, bodyCenter.y - bodyRadius * 0.75f)
    return when {
        circleContains(headCenter, headRadius, point) -> HitZone.HEAD
        circleContains(bodyCenter, bodyRadius, point) -> HitZone.BODY
        else                                           -> HitZone.NONE
    }
}

enum class HitZone { HEAD, BODY, NONE }

// ---------------------------------------------------------------------------
// Validation helpers
// ---------------------------------------------------------------------------

/** Returns true when both coordinates are finite (not NaN, not Infinite). */
fun Vec2.isValid(): Boolean = x.isFinite() && y.isFinite()

/** Returns true when width and height are positive and all coordinates finite. */
fun BoundingBox.isValid(): Boolean =
    topLeft.isValid() && width > 0f && height > 0f && width.isFinite() && height.isFinite()
