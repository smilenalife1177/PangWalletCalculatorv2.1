package tw.smilenalife.pangwallet.v2

import android.content.Context
import android.graphics.Color
import java.io.File

object WidgetPrefs {
    private const val PREFS = "pang_wallet_v2_widget_prefs"
    const val DEFAULT_BODY_COLOR = "#F6E29A"

    data class Design(
        val title1: String = "胖錢包",
        val title2: String = "計算機",
        val fontSizeSp: Float = 18f,
        val heart: String = "♥",
        val bodyColor: Int = Color.parseColor(DEFAULT_BODY_COLOR),
        val imageMode: String = "default1",
        val customImagePath: String? = null
    )

    private fun k(id: Int, field: String) = "w_${id}_$field"

    fun load(context: Context, id: Int): Design {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Design(
            title1 = p.getString(k(id, "title1"), "胖錢包") ?: "胖錢包",
            title2 = p.getString(k(id, "title2"), "計算機") ?: "計算機",
            fontSizeSp = p.getFloat(k(id, "font"), 18f),
            heart = p.getString(k(id, "heart"), "♥") ?: "♥",
            bodyColor = p.getInt(k(id, "body"), Color.parseColor(DEFAULT_BODY_COLOR)),
            imageMode = p.getString(k(id, "imageMode"), "default1") ?: "default1",
            customImagePath = p.getString(k(id, "customPath"), null)
        )
    }

    fun save(context: Context, id: Int, d: Design) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(k(id, "title1"), d.title1)
            .putString(k(id, "title2"), d.title2)
            .putFloat(k(id, "font"), d.fontSizeSp)
            .putString(k(id, "heart"), d.heart)
            .putInt(k(id, "body"), d.bodyColor)
            .putString(k(id, "imageMode"), d.imageMode)
            .putString(k(id, "customPath"), d.customImagePath)
            .apply()
    }

    fun delete(context: Context, id: Int) {
        val old = load(context, id)
        old.customImagePath?.let { runCatching { File(it).delete() } }
        val e = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        listOf("title1", "title2", "font", "heart", "body", "imageMode", "customPath")
            .forEach { e.remove(k(id, it)) }
        e.apply()
    }
}
