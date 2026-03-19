package com.merkezekre2020.kfcmapdroidtr

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.merkezekre2020.kfcmapdroidtr.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var latestJson: String = "[]"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_refresh -> {
                    fetchStores(forceReload = true)
                    true
                }
                else -> false
            }
        }

        binding.swipeRefresh.setOnRefreshListener { fetchStores(forceReload = true) }

        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            builtInZoomControls = false
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = true
            allowContentAccess = true
        }
        binding.webView.webChromeClient = WebChromeClient()
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                pushStoresToWebView()
            }
        }
        binding.webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")
        binding.webView.loadUrl("file:///android_asset/index.html")

        fetchStores(forceReload = false)
    }

    private fun fetchStores(forceReload: Boolean) {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { downloadAndExtractStores() }
            }
            binding.swipeRefresh.isRefreshing = false
            result.onSuccess {
                latestJson = it
                pushStoresToWebView()
                if (forceReload) Toast.makeText(this@MainActivity, "Şubeler yenilendi", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this@MainActivity, "Veri alınamadı: ${it.message}", Toast.LENGTH_LONG).show()
                pushStoresToWebView()
            }
        }
    }

    private fun pushStoresToWebView() {
        val escaped = JSONObject.quote(latestJson)
        binding.webView.evaluateJavascript("window.renderStoresFromAndroid($escaped);", null)
    }

    private fun downloadAndExtractStores(): String {
        val url = URL("https://www.kfcturkiye.com/restoranlar")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("User-Agent", "Mozilla/5.0 (Android) KFCMapDroidTR/1.0")
        }
        val html = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()

        val marker = "initialRestaurants\\\":["
        val startMarker = html.indexOf(marker)
        require(startMarker >= 0) { "initialRestaurants bulunamadı" }
        val arrayStart = html.indexOf('[', startMarker)
        require(arrayStart >= 0) { "Dizi başlangıcı bulunamadı" }

        var depth = 0
        var escaped = false
        var end = -1
        for (i in arrayStart until html.length) {
            val c = html[i]
            if (escaped) {
                escaped = false
                continue
            }
            if (c == '\\') {
                escaped = true
                continue
            }
            if (c == '[') depth++
            if (c == ']') {
                depth--
                if (depth == 0) {
                    end = i
                    break
                }
            }
        }
        require(end > arrayStart) { "Dizi sonu bulunamadı" }

        var raw = html.substring(arrayStart, end + 1)
        repeat(8) {
            raw = raw.replace("\\\\/", "/")
            raw = raw.replace("\\\"", "\"")
        }

        val arr = JSONArray(raw)
        val cleaned = JSONArray()
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            val out = JSONObject()
            out.put("id", item.optInt("id"))
            out.put("name", item.optString("name"))
            out.put("lat", item.optDouble("lat"))
            out.put("lng", item.optDouble("lng"))
            out.put("address", item.optString("address").replace("\\/", "/"))
            out.put("city", item.optString("city"))
            out.put("district", item.optString("district"))
            out.put("paket", item.optBoolean("paket"))
            out.put("gelAl", item.optBoolean("gelAl"))
            cleaned.put(out)
        }
        return cleaned.toString()
    }

    inner class AndroidBridge {
        @JavascriptInterface
        fun showToast(message: String) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
