package com.gyjian.bishun

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.ConsoleMessage
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.view.ViewTreeObserver
import android.util.DisplayMetrics
import java.io.IOException
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var editTextInput: EditText
    private lateinit var buttonShow: Button
    private lateinit var buttonReplay: Button
    private lateinit var charsDisplayContainer: LinearLayout
    private lateinit var webViewContainer: LinearLayout

    private var currentChars: String = ""
    private var selectedCharIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 隐藏 ActionBar
        supportActionBar?.hide()

        setContentView(R.layout.activity_main)

        initViews()
        setupWebView()
        setupListeners()
    }

    private fun initViews() {
        webView = findViewById(R.id.webViewStroke)
        editTextInput = findViewById(R.id.editTextInput)
        buttonShow = findViewById(R.id.buttonShow)
        buttonReplay = findViewById(R.id.buttonReplay)
        charsDisplayContainer = findViewById(R.id.charsDisplayContainer)
        webViewContainer = findViewById(R.id.webViewContainer)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        // 启用 JavaScript 和调试
        WebView.setWebContentsDebuggingEnabled(true)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true

        // 添加 JavaScript 接口
        webView.addJavascriptInterface(WebAppInterface(this), "AndroidInterface")

        // 设置 WebViewClient 以处理页面加载
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("MainActivity", "WebView 页面加载完成: $url")
                // 页面加载完成后启用按钮
                buttonShow.isEnabled = true
                buttonReplay.isEnabled = true

                // 测试 JavaScript 连接
                webView.evaluateJavascript("console.log('JavaScript 连接测试')") { result ->
                    Log.d("MainActivity", "JavaScript 测试结果: $result")
                }
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                Log.e("MainActivity", "WebView 加载错误: $description, URL: $failingUrl")
                Toast.makeText(this@MainActivity, "加载页面出错: $description", Toast.LENGTH_SHORT).show()
            }
        }

        // 设置 WebChromeClient 以显示进度和控制台
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    Log.d("WebViewConsole", "${it.messageLevel()} at ${it.sourceId()}:${it.lineNumber()} - ${it.message()}")
                }
                return true
            }
        }

        // 加载本地 HTML 文件
        val htmlUrl = "file:///android_asset/stroke_animation.html"
        Log.d("MainActivity", "加载 HTML 文件: $htmlUrl")
        webView.loadUrl(htmlUrl)

        // 初始状态下禁用按钮，等待页面加载完成
        buttonShow.isEnabled = false
        buttonReplay.isEnabled = false

        // 设置WebView为正方形
        setupSquareWebView()
    }

    private fun setupListeners() {
        // 显示按钮点击事件
        buttonShow.setOnClickListener {
            val inputText = editTextInput.text.toString().trim()
            if (inputText.isEmpty()) {
                Toast.makeText(this, "请输入汉字", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 设置当前字符和选中第一个字符
            currentChars = inputText
            selectedCharIndex = 0

            // 显示字符选择按钮
            displayCharButtons()

            // 显示第一个字符的动画
            showCharAnimation(selectedCharIndex)
        }

        // 重新播放按钮点击事件
        buttonReplay.setOnClickListener {
            // 调用 JavaScript 函数重新播放动画
            val jsCode = "javascript:replayAnimation()"
            webView.evaluateJavascript(jsCode, null)
        }

        // 输入框回车键事件
        editTextInput.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                buttonShow.performClick()
                return@setOnKeyListener true
            }
            false
        }
    }

    private fun displayCharButtons() {
        // 清空之前的按钮
        charsDisplayContainer.removeAllViews()

        // 为每个字符创建按钮
        for ((index, char) in currentChars.withIndex()) {
            val button = Button(this)
            button.text = char.toString()
            button.textSize = 20f
            button.setPadding(6, 8, 6, 8)

            // 设置按钮样式
            if (index == selectedCharIndex) {
                button.setBackgroundColor(resources.getColor(android.R.color.holo_blue_dark, theme))
                button.setTextColor(resources.getColor(android.R.color.white, theme))
            } else {
                button.setBackgroundColor(resources.getColor(android.R.color.darker_gray, theme))
                button.setTextColor(resources.getColor(android.R.color.black, theme))
            }

            // 设置点击事件
            button.setOnClickListener {
                selectedCharIndex = index
                displayCharButtons() // 重新渲染按钮以更新选中状态
                showCharAnimation(index)
            }

            // 添加到容器
            charsDisplayContainer.addView(button)

            // 设置等分宽度，确保所有按钮平均分配空间
            val params = LinearLayout.LayoutParams(
                0, // 宽度为0，使用weight来分配
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.weight = 1.0f
            params.setMargins(2, 0, 2, 0) // 减少间距以节省空间
            button.layoutParams = params
        }

        // 显示容器
        charsDisplayContainer.visibility = View.VISIBLE
    }

    private fun showCharAnimation(index: Int) {
        if (index < 0 || index >= currentChars.length) return

        val char = currentChars[index]
        Log.d("MainActivity", "显示字符动画: $char (索引: $index)")

        // 调用 JavaScript 函数显示笔顺动画
        val jsCode = "javascript:showStrokeAnimation('$char')"
        webView.evaluateJavascript(jsCode, null)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        // 清理 WebView 资源
        webView.destroy()
        super.onDestroy()
    }

    /**
     * JavaScript 接口类，用于 WebView 与 Android 之间的通信
     */
    inner class WebAppInterface(private val context: Context) {

        /**
         * 加载指定 Unicode 码点的 SVG 文件
         */
        @JavascriptInterface
        fun loadSvg(codePoint: Int) {
            Log.d("MainActivity", "开始加载 SVG: $codePoint")
            val svgContent = readSvgFromAssets(codePoint)
            // 在主线程中更新 WebView
            runOnUiThread {
                if (svgContent != null) {
                    Log.d("MainActivity", "SVG 读取成功，长度: ${svgContent.length}")
                    // 将 SVG 内容编码为 Base64 来避免转义问题
                    val encodedSvg = android.util.Base64.encodeToString(
                        svgContent.toByteArray(Charsets.UTF_8),
                        android.util.Base64.NO_WRAP
                    )
                    Log.d("MainActivity", "Base64 编码长度: ${encodedSvg.length}")
                    val jsCode = "javascript:updateSvgContentFromBase64('$encodedSvg')"
                    Log.d("MainActivity", "执行 JavaScript: ${jsCode.take(100)}...")
                    webView.evaluateJavascript(jsCode) { result ->
                        Log.d("MainActivity", "JavaScript 执行结果: $result")
                    }
                } else {
                    Log.w("MainActivity", "SVG 文件未找到: $codePoint")
                    val jsCode = "javascript:updateSvgContent('')"
                    webView.evaluateJavascript(jsCode, null)
                }
            }
        }
    }

    /**
     * 从 assets 目录读取 SVG 文件内容
     */
    private fun readSvgFromAssets(codePoint: Int): String? {
        return try {
            val fileName = "$codePoint.svg"
            Log.d("MainActivity", "尝试读取文件: stroke_svgs/$fileName")
            val inputStream: InputStream = assets.open("stroke_svgs/$fileName")
            val size = inputStream.available()
            Log.d("MainActivity", "文件大小: $size bytes")
            val buffer = ByteArray(size)
            val bytesRead = inputStream.read(buffer)
            inputStream.close()
            val content = String(buffer, Charsets.UTF_8)
            Log.d("MainActivity", "成功读取文件，读取字节数: $bytesRead")
            content
        } catch (e: IOException) {
            Log.e("MainActivity", "读取 SVG 文件失败: $codePoint.svg", e)
            null
        }
    }

    private fun setupSquareWebView() {
        // 使用ViewTreeObserver在布局完成后设置正方形尺寸
        webViewContainer.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // 移除监听器避免重复调用
                webViewContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)

                // 获取容器的宽度
                val containerWidth = webViewContainer.width

                // 设置容器为正方形
                val layoutParams = webViewContainer.layoutParams
                layoutParams.height = containerWidth
                webViewContainer.layoutParams = layoutParams

                Log.d("MainActivity", "WebView容器设置为正方形: ${containerWidth}x${containerWidth}")
            }
        })
    }
}