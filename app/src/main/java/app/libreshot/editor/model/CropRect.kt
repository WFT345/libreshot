package app.libreshot.editor.model

import kotlin.math.roundToInt

/** Which grabber is being dragged: the four corner brackets or an edge midpoint. */
enum class CropHandle { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, TOP, BOTTOM, LEFT, RIGHT }

/** Integer pixel rect inside a bitmap. */
data class PixelRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    fun isFull(bitmapWidth: Int, bitmapHeight: Int): Boolean =
        left == 0 && top == 0 && right == bitmapWidth && bottom == bitmapHeight
}

/**
 * Crop region as fractions (0..1) of the bitmap, so it survives zoom and export unchanged.
 * Pure Kotlin for unit tests. Non-destructive until the Compositor applies it at export.
 */
data class CropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val isFull: Boolean get() = this == FULL

    /**
     * Copy with [handle] moved to ([x], [y]) in normalized space. The dragged edge(s) clamp
     * to [0, 1] and never cross the opposite edge - the rect keeps at least [minSize].
     */
    fun dragged(handle: CropHandle, x: Float, y: Float, minSize: Float = MIN_SIZE): CropRect {
        val cx = x.coerceIn(0f, 1f)
        val cy = y.coerceIn(0f, 1f)
        var l = left
        var t = top
        var r = right
        var b = bottom
        when (handle) {
            CropHandle.TOP_LEFT -> {
                l = cx.coerceAtMost(r - minSize)
                t = cy.coerceAtMost(b - minSize)
            }
            CropHandle.TOP_RIGHT -> {
                r = cx.coerceAtLeast(l + minSize)
                t = cy.coerceAtMost(b - minSize)
            }
            CropHandle.BOTTOM_LEFT -> {
                l = cx.coerceAtMost(r - minSize)
                b = cy.coerceAtLeast(t + minSize)
            }
            CropHandle.BOTTOM_RIGHT -> {
                r = cx.coerceAtLeast(l + minSize)
                b = cy.coerceAtLeast(t + minSize)
            }
            CropHandle.TOP -> t = cy.coerceAtMost(b - minSize)
            CropHandle.BOTTOM -> b = cy.coerceAtLeast(t + minSize)
            CropHandle.LEFT -> l = cx.coerceAtMost(r - minSize)
            CropHandle.RIGHT -> r = cx.coerceAtLeast(l + minSize)
        }
        return CropRect(l, t, r, b)
    }

    /** Pixel-space rect inside a [bitmapWidth] × [bitmapHeight] bitmap, at least 1 px each side. */
    fun toPixelRect(bitmapWidth: Int, bitmapHeight: Int): PixelRect {
        val l = (left * bitmapWidth).roundToInt().coerceIn(0, bitmapWidth - 1)
        val t = (top * bitmapHeight).roundToInt().coerceIn(0, bitmapHeight - 1)
        val r = (right * bitmapWidth).roundToInt().coerceIn(l + 1, bitmapWidth)
        val b = (bottom * bitmapHeight).roundToInt().coerceIn(t + 1, bitmapHeight)
        return PixelRect(l, t, r, b)
    }

    companion object {
        /** Smallest crop: 2 % of the bitmap per axis. */
        const val MIN_SIZE = 0.02f
        val FULL = CropRect(0f, 0f, 1f, 1f)
    }
}
