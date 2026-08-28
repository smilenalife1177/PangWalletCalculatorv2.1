package tw.smilenalife.pangwallet.v2

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader

object WidgetArtwork {
    fun makeBodyBitmap(baseColor: Int, width: Int = 240, height: Int = 360): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val radius = 22f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                lighten(baseColor, 0.13f),
                darken(baseColor, 0.07f),
                Shader.TileMode.CLAMP
            )
        }
        val rect = RectF(2f, 2f, width - 2f, height - 2f)
        c.drawRoundRect(rect, radius, radius, paint)

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = darken(baseColor, 0.35f)
        }
        c.drawRoundRect(rect, radius, radius, border)
        return bmp
    }

    private fun lighten(color: Int, amount: Float): Int = blend(color, Color.WHITE, amount)
    private fun darken(color: Int, amount: Float): Int = blend(color, Color.BLACK, amount)

    private fun blend(a: Int, b: Int, ratio: Float): Int {
        val r = ratio.coerceIn(0f, 1f)
        val inv = 1f - r
        return Color.argb(
            255,
            (Color.red(a) * inv + Color.red(b) * r).toInt(),
            (Color.green(a) * inv + Color.green(b) * r).toInt(),
            (Color.blue(a) * inv + Color.blue(b) * r).toInt()
        )
    }
}
