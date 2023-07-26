// MIT License
//
// Copyright (c) 2023 caozhanhao
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package com.caozhanhao.lead

import android.content.Context
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.drake.statusbar.immersive
import java.io.File
import java.io.FileOutputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection

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

fun waitServer()
{
    while (true) {
        try {
            val url = URL("http://127.0.0.1:8968/about.html")
            val conn: URLConnection = url.openConnection()
            if (conn is HttpURLConnection) {
                conn.connectTimeout = 200
                conn.requestMethod = "GET";
                if (conn.responseCode == 200)
                    return
            }
        }
        catch(e: ConnectException)
        {
            Thread.sleep(200)
        }
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
        val th = Thread { svr.run("127.0.0.1", 8968, filesDir.toString()) }
        th.start()

        val swipeRefresh: SwipeRefreshLayout = findViewById(R.id.swipe_refresh);
        swipeRefresh.setOnRefreshListener(object : SwipeRefreshLayout.OnRefreshListener {
            override fun onRefresh() {
                val webView: WebView = findViewById(R.id.webview)
                webView.getUrl()?.let { webView.loadUrl(it) };
                swipeRefresh.isRefreshing = false;
            }
        })
        swipeRefresh.setColorSchemeResources(
            android.R.color.holo_blue_light,
            android.R.color.holo_red_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_green_light
        );

        val webView: WebView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true
        webView.settings.defaultTextEncodingName = "utf-8"
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                Log.d("webview", "url: $url")
                view.loadUrl(url)
                return true
            }

            override fun onReceivedError(
                view: WebView,
                webResourceRequest: WebResourceRequest,
                webResourceError: WebResourceError?
            ) {
                super.onReceivedError(view, webResourceRequest, webResourceError)
                if (webResourceRequest.isForMainFrame) {
                    webView.loadUrl("about:blank")
                    Thread.sleep(500)
                    webView.loadUrl("http://127.0.0.1:8968")
                }
            }
        }
        webView.loadDataWithBaseURL(null, "<!DOCTYPE html><html><head><style>:root{--sk-size:40px;--sk-color:#333}.sk-center{margin:auto}.sk-plane{width:var(--sk-size);height:var(--sk-size);background-color:var(--sk-color);animation:sk-plane 1.2s infinite ease-in-out}@keyframes sk-plane{0%{transform:perspective(120px) rotateX(0) rotateY(0)}50%{transform:perspective(120px) rotateX(-180.1deg) rotateY(0)}100%{transform:perspective(120px) rotateX(-180deg) rotateY(-179.9deg)}}.sk-chase{width:var(--sk-size);height:var(--sk-size);position:relative;animation:sk-chase 2.5s infinite linear both}.sk-chase-dot{width:100%;height:100%;position:absolute;left:0;top:0;animation:sk-chase-dot 2.0s infinite ease-in-out both}.sk-chase-dot:before{content:'';display:block;width:25%;height:25%;background-color:var(--sk-color);border-radius:100%;animation:sk-chase-dot-before 2.0s infinite ease-in-out both}.sk-chase-dot:nth-child(1){animation-delay:-1.1s}.sk-chase-dot:nth-child(2){animation-delay:-1.0s}.sk-chase-dot:nth-child(3){animation-delay:-0.9s}.sk-chase-dot:nth-child(4){animation-delay:-0.8s}.sk-chase-dot:nth-child(5){animation-delay:-0.7s}.sk-chase-dot:nth-child(6){animation-delay:-0.6s}.sk-chase-dot:nth-child(1):before{animation-delay:-1.1s}.sk-chase-dot:nth-child(2):before{animation-delay:-1.0s}.sk-chase-dot:nth-child(3):before{animation-delay:-0.9s}.sk-chase-dot:nth-child(4):before{animation-delay:-0.8s}.sk-chase-dot:nth-child(5):before{animation-delay:-0.7s}.sk-chase-dot:nth-child(6):before{animation-delay:-0.6s}@keyframes sk-chase{100%{transform:rotate(360deg)}}@keyframes sk-chase-dot{80%,100%{transform:rotate(360deg)}}@keyframes sk-chase-dot-before{50%{transform:scale(0.4)}100%,0%{transform:scale(1.0)}}.sk-bounce{width:var(--sk-size);height:var(--sk-size);position:relative}.sk-bounce-dot{width:100%;height:100%;border-radius:50%;background-color:var(--sk-color);opacity:.6;position:absolute;top:0;left:0;animation:sk-bounce 2s infinite cubic-bezier(0.455,0.03,0.515,0.955)}.sk-bounce-dot:nth-child(2){animation-delay:-1.0s}@keyframes sk-bounce{0%,100%{transform:scale(0)}45%,55%{transform:scale(1)}}.sk-wave{width:var(--sk-size);height:var(--sk-size);display:flex;justify-content:space-between}.sk-wave-rect{background-color:var(--sk-color);height:100%;width:15%;animation:sk-wave 1.2s infinite ease-in-out}.sk-wave-rect:nth-child(1){animation-delay:-1.2s}.sk-wave-rect:nth-child(2){animation-delay:-1.1s}.sk-wave-rect:nth-child(3){animation-delay:-1.0s}.sk-wave-rect:nth-child(4){animation-delay:-0.9s}.sk-wave-rect:nth-child(5){animation-delay:-0.8s}@keyframes sk-wave{0%,40%,100%{transform:scaleY(0.4)}20%{transform:scaleY(1)}}.sk-pulse{width:var(--sk-size);height:var(--sk-size);background-color:var(--sk-color);border-radius:100%;animation:sk-pulse 1.2s infinite cubic-bezier(0.455,0.03,0.515,0.955)}@keyframes sk-pulse{0%{transform:scale(0)}100%{transform:scale(1);opacity:0}}.sk-flow{width:calc(var(--sk-size) * 1.3);height:calc(var(--sk-size) * 1.3);display:flex;justify-content:space-between}.sk-flow-dot{width:25%;height:25%;background-color:var(--sk-color);border-radius:50%;animation:sk-flow 1.4s cubic-bezier(0.455,0.03,0.515,0.955) 0s infinite both}.sk-flow-dot:nth-child(1){animation-delay:-0.30s}.sk-flow-dot:nth-child(2){animation-delay:-0.15s}@keyframes sk-flow{0%,80%,100%{transform:scale(0.3)}40%{transform:scale(1)}}.sk-swing{width:var(--sk-size);height:var(--sk-size);position:relative;animation:sk-swing 1.8s infinite linear}.sk-swing-dot{width:45%;height:45%;position:absolute;top:0;left:0;right:0;margin:auto;background-color:var(--sk-color);border-radius:100%;animation:sk-swing-dot 2s infinite ease-in-out}.sk-swing-dot:nth-child(2){top:auto;bottom:0;animation-delay:-1s}@keyframes sk-swing{100%{transform:rotate(360deg)}}@keyframes sk-swing-dot{0%,100%{transform:scale(0.2)}50%{transform:scale(1)}}.sk-circle{width:var(--sk-size);height:var(--sk-size);position:relative}.sk-circle-dot{width:100%;height:100%;position:absolute;left:0;top:0}.sk-circle-dot:before{content:'';display:block;width:15%;height:15%;background-color:var(--sk-color);border-radius:100%;animation:sk-circle 1.2s infinite ease-in-out both}.sk-circle-dot:nth-child(1){transform:rotate(30deg)}.sk-circle-dot:nth-child(2){transform:rotate(60deg)}.sk-circle-dot:nth-child(3){transform:rotate(90deg)}.sk-circle-dot:nth-child(4){transform:rotate(120deg)}.sk-circle-dot:nth-child(5){transform:rotate(150deg)}.sk-circle-dot:nth-child(6){transform:rotate(180deg)}.sk-circle-dot:nth-child(7){transform:rotate(210deg)}.sk-circle-dot:nth-child(8){transform:rotate(240deg)}.sk-circle-dot:nth-child(9){transform:rotate(270deg)}.sk-circle-dot:nth-child(10){transform:rotate(300deg)}.sk-circle-dot:nth-child(11){transform:rotate(330deg)}.sk-circle-dot:nth-child(1):before{animation-delay:-1.1s}.sk-circle-dot:nth-child(2):before{animation-delay:-1s}.sk-circle-dot:nth-child(3):before{animation-delay:-0.9s}.sk-circle-dot:nth-child(4):before{animation-delay:-0.8s}.sk-circle-dot:nth-child(5):before{animation-delay:-0.7s}.sk-circle-dot:nth-child(6):before{animation-delay:-0.6s}.sk-circle-dot:nth-child(7):before{animation-delay:-0.5s}.sk-circle-dot:nth-child(8):before{animation-delay:-0.4s}.sk-circle-dot:nth-child(9):before{animation-delay:-0.3s}.sk-circle-dot:nth-child(10):before{animation-delay:-0.2s}.sk-circle-dot:nth-child(11):before{animation-delay:-0.1s}@keyframes sk-circle{0%,80%,100%{transform:scale(0)}40%{transform:scale(1)}}.sk-circle-fade{width:var(--sk-size);height:var(--sk-size);position:relative}.sk-circle-fade-dot{width:100%;height:100%;position:absolute;left:0;top:0}.sk-circle-fade-dot:before{content:'';display:block;width:15%;height:15%;background-color:var(--sk-color);border-radius:100%;animation:sk-circle-fade 1.2s infinite ease-in-out both}.sk-circle-fade-dot:nth-child(1){transform:rotate(30deg)}.sk-circle-fade-dot:nth-child(2){transform:rotate(60deg)}.sk-circle-fade-dot:nth-child(3){transform:rotate(90deg)}.sk-circle-fade-dot:nth-child(4){transform:rotate(120deg)}.sk-circle-fade-dot:nth-child(5){transform:rotate(150deg)}.sk-circle-fade-dot:nth-child(6){transform:rotate(180deg)}.sk-circle-fade-dot:nth-child(7){transform:rotate(210deg)}.sk-circle-fade-dot:nth-child(8){transform:rotate(240deg)}.sk-circle-fade-dot:nth-child(9){transform:rotate(270deg)}.sk-circle-fade-dot:nth-child(10){transform:rotate(300deg)}.sk-circle-fade-dot:nth-child(11){transform:rotate(330deg)}.sk-circle-fade-dot:nth-child(1):before{animation-delay:-1.1s}.sk-circle-fade-dot:nth-child(2):before{animation-delay:-1.0s}.sk-circle-fade-dot:nth-child(3):before{animation-delay:-0.9s}.sk-circle-fade-dot:nth-child(4):before{animation-delay:-0.8s}.sk-circle-fade-dot:nth-child(5):before{animation-delay:-0.7s}.sk-circle-fade-dot:nth-child(6):before{animation-delay:-0.6s}.sk-circle-fade-dot:nth-child(7):before{animation-delay:-0.5s}.sk-circle-fade-dot:nth-child(8):before{animation-delay:-0.4s}.sk-circle-fade-dot:nth-child(9):before{animation-delay:-0.3s}.sk-circle-fade-dot:nth-child(10):before{animation-delay:-0.2s}.sk-circle-fade-dot:nth-child(11):before{animation-delay:-0.1s}@keyframes sk-circle-fade{0%,39%,100%{opacity:0;transform:scale(0.6)}40%{opacity:1;transform:scale(1)}}.sk-grid{width:var(--sk-size);height:var(--sk-size)}.sk-grid-cube{width:33.33%;height:33.33%;background-color:var(--sk-color);float:left;animation:sk-grid 1.3s infinite ease-in-out}.sk-grid-cube:nth-child(1){animation-delay:.2s}.sk-grid-cube:nth-child(2){animation-delay:.3s}.sk-grid-cube:nth-child(3){animation-delay:.4s}.sk-grid-cube:nth-child(4){animation-delay:.1s}.sk-grid-cube:nth-child(5){animation-delay:.2s}.sk-grid-cube:nth-child(6){animation-delay:.3s}.sk-grid-cube:nth-child(7){animation-delay:0s}.sk-grid-cube:nth-child(8){animation-delay:.1s}.sk-grid-cube:nth-child(9){animation-delay:.2s}@keyframes sk-grid{0%,70%,100%{transform:scale3D(1,1,1)}35%{transform:scale3D(0,0,1)}}.sk-fold{width:var(--sk-size);height:var(--sk-size);position:relative;transform:rotateZ(45deg)}.sk-fold-cube{float:left;width:50%;height:50%;position:relative;transform:scale(1.1)}.sk-fold-cube:before{content:'';position:absolute;top:0;left:0;width:100%;height:100%;background-color:var(--sk-color);animation:sk-fold 2.4s infinite linear both;transform-origin:100% 100%}.sk-fold-cube:nth-child(2){transform:scale(1.1) rotateZ(90deg)}.sk-fold-cube:nth-child(4){transform:scale(1.1) rotateZ(180deg)}.sk-fold-cube:nth-child(3){transform:scale(1.1) rotateZ(270deg)}.sk-fold-cube:nth-child(2):before{animation-delay:.3s}.sk-fold-cube:nth-child(4):before{animation-delay:.6s}.sk-fold-cube:nth-child(3):before{animation-delay:.9s}@keyframes sk-fold{0%,10%{transform:perspective(140px) rotateX(-180deg);opacity:0}25%,75%{transform:perspective(140px) rotateX(0);opacity:1}90%,100%{transform:perspective(140px) rotateY(180deg);opacity:0}}.sk-wander{width:var(--sk-size);height:var(--sk-size);position:relative}.sk-wander-cube{background-color:var(--sk-color);width:20%;height:20%;position:absolute;top:0;left:0;--sk-wander-distance:calc(var(--sk-size) * 0.75);animation:sk-wander 2.0s ease-in-out -2.0s infinite both}.sk-wander-cube:nth-child(2){animation-delay:-0.5s}.sk-wander-cube:nth-child(3){animation-delay:-1.0s}@keyframes sk-wander{0%{transform:rotate(0)}25%{transform:translateX(var(--sk-wander-distance)) rotate(-90deg) scale(0.6)}50%{transform:translateX(var(--sk-wander-distance)) translateY(var(--sk-wander-distance)) rotate(-179deg)}50.1%{transform:translateX(var(--sk-wander-distance)) translateY(var(--sk-wander-distance)) rotate(-180deg)}75%{transform:translateX(0) translateY(var(--sk-wander-distance)) rotate(-270deg) scale(0.6)}100%{transform:rotate(-360deg)}}    .loading {      display: flex;      height: 500px;      align-items: center;      justify-content: center;    }  </style></head><body>  <div class=\"loading\">    <div class=\"sk-plane\"></div>  </div></body></html>", "text/html", "UTF-8", null)
        val th_wait = Thread { waitServer()}
        th_wait.start()
        th_wait.join()
        webView.loadUrl("http://127.0.0.1:8968")
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