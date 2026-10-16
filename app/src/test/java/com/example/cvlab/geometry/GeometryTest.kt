package com.example.cvlab.geometry

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class GeometryTest {

    // -----------------------------------------------------------------------
    // Vec2 basics
    // -----------------------------------------------------------------------

    @Test
    fun `distance between origin and 3-4-5 point is 5`() {
        assertEquals(5f, Vec2(0f, 0f).distanceTo(Vec2(3f, 4f)), 0.001f)
    }

    @Test
    fun `distance to self is zero`() {
        val v = Vec2(42f, -17f)
        assertEquals(0f, v.distanceTo(v), 0.001f)
    }

    @Test
    fun `distanceSquared matches distance squared`() {
        val a = Vec2(1f, 2f)
        val b = Vec2(4f, 6f)
        val expected = a.distanceTo(b).let { it * it }
        assertEquals(expected, a.distanceSquaredTo(b), 0.001f)
    }

    @Test
    fun `vec2 addition`() {
        val result = Vec2(1f, 2f) + Vec2(3f, 4f)
        assertEquals(Vec2(4f, 6f), result)
    }

    @Test
    fun `vec2 subtraction`() {
        val result = Vec2(5f, 9f) - Vec2(2f, 4f)
        assertEquals(Vec2(3f, 5f), result)
    }

    @Test
    fun `vec2 scalar multiplication`() {
        assertEquals(Vec2(6f, 9f), Vec2(2f, 3f) * 3f)
    }

    @Test
    fun `vec2 normalized has unit magnitude`() {
        val n = Vec2(3f, 4f).normalized()
        assertEquals(1f, n.magnitude(), 0.001f)
    }

    @Test
    fun `zero vec2 normalized stays zero`() {
        val n = Vec2.Zero.normalized()
        assertEquals(Vec2.Zero, n)
    }

    @Test
    fun `vec2 lerp at t=0 is start`() {
        val a = Vec2(0f, 0f); val b = Vec2(10f, 20f)
        assertEquals(a, a.lerp(b, 0f))
    }

    @Test
    fun `vec2 lerp at t=1 is end`() {
        val a = Vec2(0f, 0f); val b = Vec2(10f, 20f)
        assertEquals(b, a.lerp(b, 1f))
    }

    @Test
    fun `vec2 lerp at t=0_5 is midpoint`() {
        val mid = Vec2(0f, 0f).lerp(Vec2(10f, 20f), 0.5f)
        assertEquals(Vec2(5f, 10f), mid)
    }

    @Test
    fun `isValid returns false for NaN`() {
        assertFalse(Vec2(Float.NaN, 0f).isValid())
    }

    @Test
    fun `isValid returns false for Infinity`() {
        assertFalse(Vec2(0f, Float.POSITIVE_INFINITY).isValid())
    }

    @Test
    fun `isValid returns true for normal coords`() {
        assertTrue(Vec2(100f, -200f).isValid())
    }

    // -----------------------------------------------------------------------
    // BoundingBox
    // -----------------------------------------------------------------------

    @Test
    fun `boundingbox contains inside point`() {
        val box = BoundingBox(Vec2(0f, 0f), 100f, 100f)
        assertTrue(box.contains(Vec2(50f, 50f)))
    }

    @Test
    fun `boundingbox does not contain outside point`() {
        val box = BoundingBox(Vec2(0f, 0f), 100f, 100f)
        assertFalse(box.contains(Vec2(150f, 50f)))
    }

    @Test
    fun `boundingbox contains boundary point`() {
        val box = BoundingBox(Vec2(0f, 0f), 100f, 100f)
        assertTrue(box.contains(Vec2(100f, 100f)))
    }

    @Test
    fun `overlapping boxes have positive iou`() {
        val a = BoundingBox(Vec2(0f, 0f), 100f, 100f)
        val b = BoundingBox(Vec2(50f, 50f), 100f, 100f)
        assertTrue(a.iou(b) > 0f)
    }

    @Test
    fun `non-overlapping boxes have iou of 0`() {
        val a = BoundingBox(Vec2(0f, 0f), 50f, 50f)
        val b = BoundingBox(Vec2(100f, 0f), 50f, 50f)
        assertEquals(0f, a.iou(b), 0.001f)
    }

    @Test
    fun `identical boxes have iou of 1`() {
        val box = BoundingBox(Vec2(10f, 10f), 40f, 40f)
        assertEquals(1f, box.iou(box), 0.001f)
    }

    @Test
    fun `center is correct`() {
        val box = BoundingBox(Vec2(0f, 0f), 100f, 60f)
        assertEquals(Vec2(50f, 30f), box.center)
    }

    @Test
    fun `fromCenter round-trips`() {
        val center = Vec2(50f, 50f)
        val box = BoundingBox.fromCenter(center, 40f, 60f)
        assertEquals(center.x, box.center.x, 0.001f)
        assertEquals(center.y, box.center.y, 0.001f)
    }

    @Test
    fun `scale multiplies dimensions`() {
        val box = BoundingBox(Vec2(10f, 20f), 50f, 30f)
        val scaled = box.scale(2f, 3f)
        assertEquals(100f, scaled.width, 0.001f)
        assertEquals(90f, scaled.height, 0.001f)
    }

    // -----------------------------------------------------------------------
    // Coordinate transforms
    // -----------------------------------------------------------------------

    @Test
    fun `normalise centre of image is 0_5`() {
        val p = normalizePoint(Vec2(160f, 120f), 320f, 240f)
        assertEquals(0.5f, p.x, 0.001f)
        assertEquals(0.5f, p.y, 0.001f)
    }

    @Test
    fun `denormalise round-trips normalise`() {
        val original = Vec2(123f, 456f)
        val norm  = normalizePoint(original, 640f, 480f)
        val back  = denormalizePoint(norm, 640f, 480f)
        assertEquals(original.x, back.x, 0.5f)
        assertEquals(original.y, back.y, 0.5f)
    }

    @Test
    fun `normalise clamps out-of-bounds`() {
        val p = normalizePoint(Vec2(700f, -50f), 320f, 240f)
        assertEquals(1f, p.x, 0.001f)
        assertEquals(0f, p.y, 0.001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `normalise throws on zero width`() {
        normalizePoint(Vec2(0f, 0f), 0f, 100f)
    }

    // -----------------------------------------------------------------------
    // Circular hitbox
    // -----------------------------------------------------------------------

    @Test
    fun `circleContains returns true when inside`() {
        assertTrue(circleContains(Vec2(0f, 0f), 10f, Vec2(5f, 5f)))
    }

    @Test
    fun `circleContains returns false when outside`() {
        assertFalse(circleContains(Vec2(0f, 0f), 10f, Vec2(8f, 8f)))
    }

    @Test
    fun `circleContains returns true on boundary`() {
        // point exactly on circumference (distance == radius)
        assertTrue(circleContains(Vec2(0f, 0f), 10f, Vec2(10f, 0f)))
    }
}
