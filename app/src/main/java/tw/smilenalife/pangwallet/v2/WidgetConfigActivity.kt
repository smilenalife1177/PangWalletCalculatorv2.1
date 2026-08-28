package tw.smilenalife.pangwallet.v2

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

class WidgetConfigActivity : Activity() {
    companion object { private const val REQ_PICK_IMAGE = 3107 }

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var title1: EditText
    private lateinit var title2: EditText
    private lateinit var hex: EditText
    private lateinit var theme: Spinner
    private lateinit var font: Spinner
    private lateinit var heart: Spinner
    private lateinit var previewBody: ImageView
    private lateinit var previewMascot: ImageView
    private lateinit var previewTitle1: TextView
    private lateinit var previewTitle2: TextView
    private lateinit var previewHeart: TextView

    private var imageMode = "default1"
    private var customImagePath: String? = null
    private var suppressThemeCallback = false

    private val themeNames = listOf("鵝黃奶油", "玫瑰金", "霧藍", "鼠尾草", "奶茶", "薰衣草", "粉霧", "自訂")
    private val themeColors = mapOf(
        "鵝黃奶油" to "#F6E29A",
        "玫瑰金" to "#D8A48F",
        "霧藍" to "#C8D6E0",
        "鼠尾草" to "#DDE6D5",
        "奶茶" to "#E6DCCF",
        "薰衣草" to "#DDD2E8",
        "粉霧" to "#E9C5C8"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        setContentView(R.layout.activity_widget_config)

        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Toast.makeText(this, "找不到桌面小工具編號，請重新新增。", Toast.LENGTH_LONG).show()
            finish(); return
        }

        bindViews()
        setupSpinners()
        loadExisting()
        wireEvents()
        updatePreview()
    }

    private fun bindViews() {
        title1 = findViewById(R.id.editTitle1)
        title2 = findViewById(R.id.editTitle2)
        hex = findViewById(R.id.editHexColor)
        theme = findViewById(R.id.spinnerTheme)
        font = findViewById(R.id.spinnerFontSize)
        heart = findViewById(R.id.spinnerHeart)
        previewBody = findViewById(R.id.previewBody)
        previewMascot = findViewById(R.id.previewMascot)
        previewTitle1 = findViewById(R.id.previewTitle1)
        previewTitle2 = findViewById(R.id.previewTitle2)
        previewHeart = findViewById(R.id.previewHeart)
    }

    private fun setupSpinners() {
        theme.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, themeNames)
        font.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("小 16", "中 18", "大 20"))
        heart.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("♥ 實心", "♡ 空心", "不顯示"))
    }

    private fun loadExisting() {
        val d = WidgetPrefs.load(this, appWidgetId)
        title1.setText(d.title1)
        title2.setText(d.title2)
        hex.setText(String.format("#%06X", 0xFFFFFF and d.bodyColor))
        font.setSelection(when (d.fontSizeSp.toInt()) { 16 -> 0; 20 -> 2; else -> 1 })
        heart.setSelection(when (d.heart) { "♡" -> 1; "" -> 2; else -> 0 })
        imageMode = d.imageMode
        customImagePath = d.customImagePath

        val exactTheme = themeNames.indexOfFirst { themeColors[it]?.equals(hex.text.toString(), true) == true }
        theme.setSelection(if (exactTheme >= 0) exactTheme else themeNames.lastIndex)
    }

    private fun wireEvents() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = updatePreview()
            override fun afterTextChanged(s: Editable?) = Unit
        }
        title1.addTextChangedListener(watcher)
        title2.addTextChangedListener(watcher)
        hex.addTextChangedListener(watcher)

        theme.onItemSelectedListener = SimpleItemSelectedListener { position ->
            if (suppressThemeCallback) return@SimpleItemSelectedListener
            themeColors[themeNames[position]]?.let {
                suppressThemeCallback = true
                hex.setText(it)
                suppressThemeCallback = false
            }
            updatePreview()
        }
        font.onItemSelectedListener = SimpleItemSelectedListener { updatePreview() }
        heart.onItemSelectedListener = SimpleItemSelectedListener { updatePreview() }

        findViewById<Button>(R.id.btnImageDefault1).setOnClickListener { imageMode = "default1"; customImagePath = null; updatePreview() }
        findViewById<Button>(R.id.btnImageDefault2).setOnClickListener { imageMode = "default2"; customImagePath = null; updatePreview() }
        findViewById<Button>(R.id.btnImageNone).setOnClickListener { imageMode = "none"; customImagePath = null; updatePreview() }
        findViewById<Button>(R.id.btnImageCustom).setOnClickListener { pickImage() }
        findViewById<Button>(R.id.btnReset).setOnClickListener { resetDefaults() }
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveAndFinish() }
    }

    private fun selectedFontSize(): Float = when (font.selectedItemPosition) { 0 -> 16f; 2 -> 20f; else -> 18f }
    private fun selectedHeart(): String = when (heart.selectedItemPosition) { 1 -> "♡"; 2 -> ""; else -> "♥" }

    private fun parseBodyColor(): Int? = try {
        val raw = hex.text.toString().trim().let { if (it.startsWith("#")) it else "#$it" }
        Color.parseColor(raw)
    } catch (_: Exception) { null }

    private fun updatePreview() {
        if (!::previewBody.isInitialized) return
        val color = parseBodyColor() ?: Color.parseColor(WidgetPrefs.DEFAULT_BODY_COLOR)
        previewBody.setImageBitmap(WidgetArtwork.makeBodyBitmap(color, 320, 220))
        previewTitle1.text = title1.text.toString().ifBlank { "胖錢包" }
        previewTitle2.text = title2.text.toString().ifBlank { "計算機" }
        val fs = selectedFontSize()
        previewTitle1.textSize = fs
        previewTitle2.textSize = fs
        previewHeart.text = selectedHeart()
        previewHeart.visibility = if (selectedHeart().isEmpty()) View.GONE else View.VISIBLE

        when (imageMode) {
            "default2" -> previewMascot.setImageResource(R.drawable.mingpang_wave)
            "none" -> previewMascot.setImageDrawable(null)
            "custom" -> {
                val bmp = customImagePath?.let { BitmapFactory.decodeFile(it) }
                if (bmp != null) previewMascot.setImageBitmap(bmp) else previewMascot.setImageResource(R.drawable.mingpang_default)
            }
            else -> previewMascot.setImageResource(R.drawable.mingpang_default)
        }
    }

    private fun pickImage() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(i, REQ_PICK_IMAGE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_PICK_IMAGE || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val path = copyImageToInternal(uri)
        if (path != null) {
            imageMode = "custom"
            customImagePath = path
            updatePreview()
        } else Toast.makeText(this, "圖片讀取失敗，請換一張試試。", Toast.LENGTH_LONG).show()
    }

    private fun copyImageToInternal(uri: Uri): String? = try {
        val original = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null
        val maxSide = 720
        val scale = minOf(1f, maxSide.toFloat() / max(original.width, original.height).toFloat())
        val w = (original.width * scale).toInt().coerceAtLeast(1)
        val h = (original.height * scale).toInt().coerceAtLeast(1)
        val scaled = if (scale < 1f) Bitmap.createScaledBitmap(original, w, h, true) else original
        val dir = File(filesDir, "widget_images").apply { mkdirs() }
        val file = File(dir, "widget_${appWidgetId}.png")
        FileOutputStream(file).use { scaled.compress(Bitmap.CompressFormat.PNG, 92, it) }
        if (scaled !== original) scaled.recycle()
        original.recycle()
        file.absolutePath
    } catch (_: Exception) { null }

    private fun resetDefaults() {
        title1.setText("胖錢包")
        title2.setText("計算機")
        hex.setText(WidgetPrefs.DEFAULT_BODY_COLOR)
        theme.setSelection(0)
        font.setSelection(1)
        heart.setSelection(0)
        imageMode = "default1"
        customImagePath = null
        updatePreview()
    }

    private fun saveAndFinish() {
        val color = parseBodyColor()
        if (color == null) {
            Toast.makeText(this, "機身色格式請用 #F6E29A 這種 HEX 色碼。", Toast.LENGTH_LONG).show(); return
        }
        val d = WidgetPrefs.Design(
            title1 = title1.text.toString().trim().ifBlank { "胖錢包" },
            title2 = title2.text.toString().trim().ifBlank { "計算機" },
            fontSizeSp = selectedFontSize(),
            heart = selectedHeart(),
            bodyColor = color,
            imageMode = imageMode,
            customImagePath = customImagePath
        )
        WidgetPrefs.save(this, appWidgetId, d)
        WalletCalculatorWidget.updateOne(this, appWidgetId)

        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, result)
        finish()
    }
}
