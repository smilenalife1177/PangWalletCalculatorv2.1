package tw.smilenalife.pangwallet.v2

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast

class MainActivity : Activity() {
    companion object {
        private const val CALCULATOR_URL = "https://smilenalife1177.github.io/lina-calculator/"
    }

    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webCalculator)
        progress = findViewById(R.id.webProgress)

        setupWebView()

        findViewById<Button>(R.id.btnHome26).setOnClickListener { loadCalculatorHome() }
        findViewById<Button>(R.id.btnEditWidgets).setOnClickListener { editMyWidget() }
        findViewById<Button>(R.id.btnAddWidget).setOnClickListener { pinWidget() }

        if (savedInstanceState == null) loadCalculatorHome()
        else webView.restoreState(savedInstanceState)
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = false
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
            mediaPlaybackRequiresUserGesture = true
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress.progress = newProgress
                progress.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                return handleSpecialScheme(uri)
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                val uri = url?.let(Uri::parse) ?: return false
                return handleSpecialScheme(uri)
            }
        }
    }

    private fun handleSpecialScheme(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme == "http" || scheme == "https") return false
        return runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        }.getOrElse { false }
    }

    private fun loadCalculatorHome() {
        webView.loadUrl(CALCULATOR_URL)
    }

    private fun installedWidgetIds(): IntArray {
        val manager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, WalletCalculatorWidget::class.java)
        return manager.getAppWidgetIds(provider)
    }

    private fun editMyWidget() {
        val ids = installedWidgetIds()
        when (ids.size) {
            0 -> {
                Toast.makeText(this, "桌面還沒有胖錢包計算機，先新增一顆。", Toast.LENGTH_SHORT).show()
                pinWidget()
            }
            1 -> openWidgetEditor(ids.first())
            else -> {
                val labels = ids.mapIndexed { index, id ->
                    val d = WidgetPrefs.load(this, id)
                    "${index + 1}. ${d.title1} ${d.title2}"
                }.toTypedArray()
                AlertDialog.Builder(this)
                    .setTitle("要改哪一顆桌面計算機？")
                    .setItems(labels) { _, which -> openWidgetEditor(ids[which]) }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }
    }

    private fun openWidgetEditor(id: Int) {
        startActivity(Intent(this, WidgetConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        })
    }

    private fun pinWidget() {
        val manager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, WalletCalculatorWidget::class.java)
        if (manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(provider, null, null)
        } else {
            Toast.makeText(this, "請長按桌面 → 小工具 → 胖錢包計算機", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        installedWidgetIds().forEach { WalletCalculatorWidget.updateOne(this, it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        webView.apply {
            stopLoading()
            webChromeClient = null
            destroy()
        }
        super.onDestroy()
    }
}
