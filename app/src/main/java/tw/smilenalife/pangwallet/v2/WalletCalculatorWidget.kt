package tw.smilenalife.pangwallet.v2

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import java.io.File
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

class WalletCalculatorWidget : AppWidgetProvider() {
    companion object {
        private const val ACTION_KEY = "tw.smilenalife.pangwallet.v2.ACTION_KEY"
        private const val EXTRA_KEY = "calc_key"
        private const val STATE_PREFS = "pang_wallet_v2_calc_state"
        private val MC = MathContext(16, RoundingMode.HALF_UP)

        private data class CalcState(var input: String = "0", var accumulator: String? = null, var operator: String? = null, var freshInput: Boolean = true)
        private fun sk(id: Int, field: String) = "w_${id}_$field"

        private fun loadState(context: Context, id: Int): CalcState {
            val p = context.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
            return CalcState(
                p.getString(sk(id, "input"), "0") ?: "0",
                p.getString(sk(id, "acc"), null),
                p.getString(sk(id, "op"), null),
                p.getBoolean(sk(id, "fresh"), true)
            )
        }

        private fun saveState(context: Context, id: Int, s: CalcState) {
            context.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE).edit()
                .putString(sk(id, "input"), s.input)
                .putString(sk(id, "acc"), s.accumulator)
                .putString(sk(id, "op"), s.operator)
                .putBoolean(sk(id, "fresh"), s.freshInput).apply()
        }

        private fun clearState(context: Context, id: Int) {
            val e = context.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE).edit()
            listOf("input", "acc", "op", "fresh").forEach { e.remove(sk(id, it)) }
            e.apply()
        }

        private fun calculate(a: String, b: String, op: String): String = try {
            val left = BigDecimal(a); val right = BigDecimal(b)
            val result = when (op) {
                "+" -> left.add(right, MC)
                "−" -> left.subtract(right, MC)
                "×" -> left.multiply(right, MC)
                "÷" -> if (right.compareTo(BigDecimal.ZERO) == 0) return "ERROR" else left.divide(right, MC)
                else -> right
            }
            val clean = result.stripTrailingZeros(); val plain = clean.toPlainString()
            if (plain.length <= 18) plain else clean.round(MathContext(12)).toEngineeringString()
        } catch (_: Exception) { "ERROR" }

        private fun doPending(s: CalcState) {
            val a = s.accumulator ?: return; val op = s.operator ?: return
            val r = calculate(a, s.input, op)
            s.input = r
            if (r == "ERROR") { s.accumulator = null; s.operator = null; s.freshInput = true }
            else s.accumulator = r
        }

        private fun applyKey(s: CalcState, key: String) {
            when (key) {
                "C" -> { s.input = "0"; s.accumulator = null; s.operator = null; s.freshInput = true }
                in "0".."9" -> {
                    if (s.input == "ERROR" || s.freshInput) { s.input = key; s.freshInput = false }
                    else if (s.input.count { it.isDigit() } < 14) s.input = if (s.input == "0") key else s.input + key
                }
                "." -> {
                    if (s.input == "ERROR" || s.freshInput) { s.input = "0."; s.freshInput = false }
                    else if (!s.input.contains('.')) s.input += "."
                }
                "+", "−", "×", "÷" -> {
                    if (s.input == "ERROR") { s.input = "0"; s.accumulator = null; s.operator = null }
                    if (s.accumulator == null) s.accumulator = s.input else if (s.operator != null && !s.freshInput) doPending(s)
                    if (s.input != "ERROR") { s.operator = key; s.freshInput = true }
                }
                "=" -> if (s.input != "ERROR" && s.accumulator != null && s.operator != null) {
                    doPending(s); s.accumulator = null; s.operator = null; s.freshInput = true
                }
            }
        }

        private fun displayText(raw: String): String {
            if (raw == "ERROR") return "錯誤"
            if (raw.contains("E", true)) return raw
            val negative = raw.startsWith('-'); val unsigned = if (negative) raw.drop(1) else raw
            val parts = unsigned.split('.', limit = 2)
            val grouped = parts[0].ifEmpty { "0" }.reversed().chunked(3).joinToString(",").reversed()
            val decimal = if (unsigned.contains('.')) "." + parts.getOrElse(1) { "" } else ""
            return (if (negative) "-" else "") + grouped + decimal
        }

        private fun keyPendingIntent(context: Context, id: Int, viewId: Int, key: String): PendingIntent {
            val i = Intent(context, WalletCalculatorWidget::class.java).apply {
                action = ACTION_KEY; putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id); putExtra(EXTRA_KEY, key)
            }
            return PendingIntent.getBroadcast(context, id * 1000 + viewId, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        private fun editPendingIntent(context: Context, id: Int): PendingIntent {
            val i = Intent(context, WidgetConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(context, 900000 + id, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        private fun loadMascot(context: Context, design: WidgetPrefs.Design): Bitmap? {
            val bmp = when (design.imageMode) {
                "none" -> null
                "default2" -> BitmapFactory.decodeResource(context.resources, R.drawable.mingpang_wave)
                "custom" -> design.customImagePath?.takeIf { File(it).exists() }?.let { BitmapFactory.decodeFile(it) }
                else -> BitmapFactory.decodeResource(context.resources, R.drawable.mingpang_default)
            }
            if (bmp == null) return null
            val max = 300
            val scale = minOf(1f, max.toFloat() / maxOf(bmp.width, bmp.height).toFloat())
            return if (scale < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true) else bmp
        }

        fun updateOne(context: Context, id: Int) {
            updateWidget(context, AppWidgetManager.getInstance(context), id)
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val options = manager.getAppWidgetOptions(id)
            val compact = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 260) < 230 || options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 360) < 330
            val views = RemoteViews(context.packageName, if (compact) R.layout.widget_wallet_compact else R.layout.widget_wallet_full)
            val state = loadState(context, id)
            val d = WidgetPrefs.load(context, id)

            views.setImageViewBitmap(R.id.widgetBodyBackground, WidgetArtwork.makeBodyBitmap(d.bodyColor))
            views.setTextViewText(R.id.titleLine1, d.title1)
            views.setTextViewText(R.id.titleLine2, d.title2)
            views.setTextViewTextSize(R.id.titleLine1, TypedValue.COMPLEX_UNIT_SP, d.fontSizeSp)
            views.setTextViewTextSize(R.id.titleLine2, TypedValue.COMPLEX_UNIT_SP, d.fontSizeSp)
            views.setTextViewText(R.id.btnEditStyle, d.heart)
            views.setViewVisibility(R.id.btnEditStyle, if (d.heart.isEmpty()) View.INVISIBLE else View.VISIBLE)
            val editIntent = editPendingIntent(context, id)
            views.setOnClickPendingIntent(R.id.btnEditStyle, editIntent)
            views.setOnClickPendingIntent(R.id.widgetHeader, editIntent)
            views.setOnClickPendingIntent(R.id.titlePanel, editIntent)
            views.setOnClickPendingIntent(R.id.mascot, editIntent)

            val mascot = loadMascot(context, d)
            if (mascot == null) views.setViewVisibility(R.id.mascot, View.INVISIBLE)
            else { views.setViewVisibility(R.id.mascot, View.VISIBLE); views.setImageViewBitmap(R.id.mascot, mascot) }

            views.setTextViewText(R.id.display, displayText(state.input))
            listOf(
                R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2", R.id.btn3 to "3", R.id.btn4 to "4",
                R.id.btn5 to "5", R.id.btn6 to "6", R.id.btn7 to "7", R.id.btn8 to "8", R.id.btn9 to "9",
                R.id.btnDot to ".", R.id.btnClear to "C", R.id.btnPlus to "+", R.id.btnMinus to "−",
                R.id.btnMultiply to "×", R.id.btnDivide to "÷", R.id.btnEquals to "="
            ).forEach { (viewId, key) -> views.setOnClickPendingIntent(viewId, keyPendingIntent(context, id, viewId, key)) }
            manager.updateAppWidget(id, views)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) = appWidgetIds.forEach { updateOne(context, it) }
    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) = updateOne(context, appWidgetId)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_KEY) return
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val key = intent.getStringExtra(EXTRA_KEY) ?: return
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val s = loadState(context, id); applyKey(s, key); saveState(context, id, s); updateOne(context, id)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { clearState(context, it); WidgetPrefs.delete(context, it) }
        super.onDeleted(context, appWidgetIds)
    }
}
