package com.glassbrowser

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {
    private val tabs = mutableListOf<WebView>()
    private val titles = mutableMapOf<WebView, String>()
    private var cur = -1
    private lateinit var host: FrameLayout
    private lateinit var addr: EditText
    private lateinit var vpnBtn: Button
    private lateinit var tabBtn: Button
    private var vpnOn = false
    private var proxy = ""
    private val inline = Executor { it.run() }
    private val handler = Handler(Looper.getMainLooper())
    private val wipe = object : Runnable {
        override fun run() {
            tabs.forEach { it.clearHistory() }
            Toast.makeText(this@MainActivity, "History cleared", Toast.LENGTH_SHORT).show()
            handler.postDelayed(this, 10 * 60 * 1000L)
        }
    }
    private val START = "file:///android_asset/start.html"

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(R.layout.activity_main)
        host = findViewById(R.id.host); addr = findViewById(R.id.addr)
        vpnBtn = findViewById(R.id.vpn); tabBtn = findViewById(R.id.tabs)
        proxy = getPreferences(MODE_PRIVATE).getString("proxy", "") ?: ""

        addr.setOnEditorActionListener { _, id, e ->
            if (id == EditorInfo.IME_ACTION_GO || e?.keyCode == KeyEvent.KEYCODE_ENTER) {
                web().loadUrl(norm(addr.text.toString())); addr.clearFocus(); true
            } else false
        }
        addr.setOnFocusChangeListener { _, f -> if (f) addr.post { addr.selectAll() } }
        findViewById<Button>(R.id.back).setOnClickListener { if (web().canGoBack()) web().goBack() }
        findViewById<Button>(R.id.fwd).setOnClickListener { if (web().canGoForward()) web().goForward() }
        findViewById<Button>(R.id.reload).setOnClickListener { web().reload() }
        findViewById<Button>(R.id.newtab).setOnClickListener { addTab() }
        tabBtn.setOnClickListener { showTabs() }
        vpnBtn.setOnClickListener { showVpn() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (web().canGoBack()) web().goBack()
                else if (tabs.size > 1) closeTab(cur) else finish()
            }
        })
        addTab()
        handler.postDelayed(wipe, 10 * 60 * 1000L)
    }

    override fun onDestroy() { handler.removeCallbacks(wipe); super.onDestroy() }

    private fun web() = tabs[cur]

    private fun norm(s: String): String {
        val t = s.trim()
        return when {
            Regex("^[a-z][a-z0-9+.-]*://", RegexOption.IGNORE_CASE).containsMatchIn(t) -> t
            Regex("^(localhost|\\S+\\.\\S{2,})(:\\d+)?(/.*)?$").matches(t) -> "https://$t"
            else -> "https://duckduckgo.com/?q=" + java.net.URLEncoder.encode(t, "UTF-8")
        }
    }

    private fun addTab(url: String = START) {
        val wv = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            setBackgroundColor(0xFF0A0F24.toInt())
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(v: WebView, u: String, f: android.graphics.Bitmap?) {
                    if (v === tabs.getOrNull(cur) && !addr.hasFocus()) addr.setText(if (u.startsWith("file:")) "" else u)
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onReceivedTitle(v: WebView, t: String) { titles[v] = t }
            }
            loadUrl(url)
        }
        tabs.add(wv); select(tabs.size - 1)
    }

    private fun select(i: Int) {
        cur = i; host.removeAllViews(); host.addView(web())
        tabBtn.text = tabs.size.toString()
        val u = web().url ?: ""
        addr.setText(if (u.startsWith("file:")) "" else u)
    }

    private fun closeTab(i: Int) {
        val w = tabs.removeAt(i); host.removeView(w); w.destroy()
        if (tabs.isEmpty()) addTab() else select(minOf(i, tabs.size - 1))
    }

    private fun showTabs() {
        val names = tabs.map { titles[it] ?: "New tab" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Tabs")
            .setItems(names) { _, i -> select(i) }
            .setPositiveButton("New tab") { _, _ -> addTab() }
            .setNegativeButton("Close this tab") { _, _ -> closeTab(cur) }.show()
    }

    private fun showVpn() {
        val input = EditText(this).apply { hint = "socks5://your-server:1080"; setText(proxy); setSingleLine() }
        AlertDialog.Builder(this).setTitle("Private tunnel")
            .setMessage("Routes this browser's traffic through your proxy. Other apps are not covered.")
            .setView(input)
            .setPositiveButton(if (vpnOn) "Disconnect" else "Connect") { _, _ ->
                proxy = input.text.toString().trim()
                getPreferences(MODE_PRIVATE).edit().putString("proxy", proxy).apply()
                setVpn(!vpnOn)
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun setVpn(on: Boolean) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            Toast.makeText(this, "This WebView version doesn't support proxies", Toast.LENGTH_LONG).show(); return
        }
        if (on && proxy.isEmpty()) { Toast.makeText(this, "Enter a proxy address first", Toast.LENGTH_SHORT).show(); return }
        val done = Runnable { vpnOn = on; vpnBtn.text = if (on) "VPN on" else "VPN off"; web().reload() }
        if (on) ProxyController.getInstance().setProxyOverride(ProxyConfig.Builder().addProxyRule(proxy).build(), inline, done)
        else ProxyController.getInstance().clearProxyOverride(inline, done)
    }
}
