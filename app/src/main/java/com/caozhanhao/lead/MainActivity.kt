package com.caozhanhao.lead

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.drake.statusbar.immersive
import java.io.File
import java.io.FileOutputStream

class LeadServer
{
    external fun run(addr: String, port: Int, resPath: String)
}

fun copyFilesFromAssets(context: Context, assetsPath: String, savePath: String)
{
    try {
        val fileNames = context.assets.list(assetsPath)
        if (fileNames!!.size > 0) {
            val file = File(savePath)
            file.mkdirs()
            for (fileName in fileNames) {
                copyFilesFromAssets(
                    context, "$assetsPath/$fileName",
                    "$savePath/$fileName"
                )
            }
        } else {
            val `is` = context.assets.open(assetsPath)
            val fos = FileOutputStream(File(savePath))
            val buffer = ByteArray(1024)
            var byteCount = 0
            while (`is`.read(buffer).also { byteCount = it } != -1) {
                fos.write(buffer, 0, byteCount)
            }
            fos.flush()
            `is`.close()
            fos.close()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

class MainActivity : AppCompatActivity() {
    init
    {
        System.loadLibrary("lead_server")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        immersive(getColor(R.color.primaryDark), false)
        copyFilesFromAssets(this, "css", filesDir.toString() + "/css")
        copyFilesFromAssets(this, "fonts", filesDir.toString() + "/fonts")
        copyFilesFromAssets(this, "html", filesDir.toString() + "/html")
        copyFilesFromAssets(this, "icons", filesDir.toString() + "/icons")
        copyFilesFromAssets(this, "js", filesDir.toString() + "/js")
        copyFilesFromAssets(this, "records", filesDir.toString() + "/records")
        copyFilesFromAssets(this, "voc", filesDir.toString() + "/voc")
        val svr = LeadServer()
        val th = Thread{svr.run("127.0.0.1", 8968, filesDir.toString())}
        th.start()
        val swipeRefresh : SwipeRefreshLayout = findViewById(R.id.swipe_refresh);
        swipeRefresh.setOnRefreshListener(object : SwipeRefreshLayout.OnRefreshListener {
            override fun onRefresh() {
                val webView: WebView = findViewById(R.id.webview)
                webView.getUrl()?.let { webView.loadUrl(it) };
                swipeRefresh.isRefreshing = false;
            }
        })
        swipeRefresh.setColorSchemeResources(android.R.color.holo_blue_light,
            android.R.color.holo_red_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_green_light);

        val webView: WebView = findViewById(R.id.webview)
        webView.loadUrl("http://127.0.0.1:8968")
        webView.settings.javaScriptEnabled = true
        webView.settings.defaultTextEncodingName = "utf-8"
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                Log.d("webview", "url: $url")
                view.loadUrl(url)
                return true
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val webView: WebView = findViewById(R.id.webview)
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        val webView: WebView = findViewById(R.id.webview)
        if (webView != null) {
            webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null)
            webView.setTag(null)
            webView.clearHistory()
            (webView.getParent() as ViewGroup).removeView(webView)
            webView.destroy()
        }
        super.onDestroy()
    }
}