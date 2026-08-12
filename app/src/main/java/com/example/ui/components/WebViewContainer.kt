package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.adblock.AdBlockerEngine
import com.example.ui.screens.NewTabPage
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.TabState

@Composable
fun WebViewContainer(
    viewModel: BrowserViewModel,
    activeTab: TabState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val whitelistedDomains by viewModel.whitelistedDomains.collectAsState()
    val findQuery by viewModel.findInPageQuery.collectAsState()

    var loadError by remember { mutableStateOf<String?>(null) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var currentLoadedUrl by remember(activeTab.id) { mutableStateOf<String?>(null) }

    // Collect web navigation actions (go back, go forward, reload)
    LaunchedEffect(viewModel) {
        viewModel.webNavAction.collect { action ->
            when (action) {
                BrowserViewModel.WebViewNavAction.GO_BACK -> {
                    if (webViewInstance?.canGoBack() == true) webViewInstance?.goBack()
                }
                BrowserViewModel.WebViewNavAction.GO_FORWARD -> {
                    if (webViewInstance?.canGoForward() == true) webViewInstance?.goForward()
                }
                BrowserViewModel.WebViewNavAction.RELOAD -> {
                    loadError = null
                    webViewInstance?.reload()
                }
            }
        }
    }

    if (activeTab.url.startsWith("albion://newtab") || activeTab.url.isEmpty()) {
        NewTabPage(
            viewModel = viewModel,
            onSearchSubmit = { query ->
                viewModel.processInputAndLoad(query)
            },
            modifier = modifier
        ) { url ->
            viewModel.processInputAndLoad(url)
        }
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        configureWebSettings(this, settings, activeTab.desktopMode)

                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                if (request == null || request.isForMainFrame) {
                                    return super.shouldInterceptRequest(view, request)
                                }
                                val url = request.url.toString()
                                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                    return super.shouldInterceptRequest(view, request)
                                }

                                val requestHost = request.url.host ?: "unknown"
                                val refererHost = request.requestHeaders["Referer"]?.let { try { Uri.parse(it).host } catch (e: Exception) { null } }
                                val activeTabHost = try { Uri.parse(activeTab.url).host } catch (e: Exception) { null }
                                val pageHost = refererHost ?: activeTabHost ?: requestHost

                                val whitelistSet = whitelistedDomains.map { it.domain }.toSet()

                                val blockResult = AdBlockerEngine.shouldBlock(
                                    url = url,
                                    pageHost = pageHost,
                                    adBlockEnabled = settings.adBlockerEnabled,
                                    trackerBlockEnabled = settings.trackerBlockerEnabled,
                                    whitelistedDomains = whitelistSet
                                )

                                return when (blockResult) {
                                    is AdBlockerEngine.BlockResult.BlockedAd -> {
                                        mainHandler.post { viewModel.onAdBlocked(url, requestHost) }
                                        AdBlockerEngine.createEmptyResponse(url)
                                    }
                                    is AdBlockerEngine.BlockResult.BlockedTracker -> {
                                        mainHandler.post { viewModel.onTrackerBlocked(url, requestHost) }
                                        AdBlockerEngine.createEmptyResponse(url)
                                    }
                                    else -> super.shouldInterceptRequest(view, request)
                                }
                            }

                            override fun onReceivedSslError(
                                view: WebView?,
                                handler: SslErrorHandler?,
                                error: SslError?
                            ) {
                                handler?.proceed()
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString() ?: return false
                                return handleCustomUri(context, url)
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                loadError = null
                                if (!url.isNull_or_blank()) {
                                    currentLoadedUrl = url
                                }
                                if (settings.adBlockerEnabled) {
                                    view?.evaluateJavascript(AdBlockerEngine.getAdHidingJs(), null)
                                }
                                viewModel.updateActiveTabState(
                                    url = url,
                                    isLoading = true,
                                    progress = 10,
                                    canGoBack = view?.canGoBack(),
                                    canGoForward = view?.canGoForward(),
                                    adsBlockedOnPage = 0,
                                    trackersBlockedOnPage = 0,
                                    pageBlockedLogs = emptyList()
                                )
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (!url.isNull_or_blank()) {
                                    currentLoadedUrl = url
                                }
                                if (settings.adBlockerEnabled) {
                                    view?.evaluateJavascript(AdBlockerEngine.getAdHidingJs(), null)
                                }
                                viewModel.updateActiveTabState(
                                    url = url,
                                    title = view?.title ?: url,
                                    isLoading = false,
                                    progress = 100,
                                    canGoBack = view?.canGoBack(),
                                    canGoForward = view?.canGoForward()
                                )
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    val description = error?.description?.toString() ?: "Page load error"
                                    if (!description.contains("ERR_ABORTED", ignoreCase = true)) {
                                        loadError = description
                                        viewModel.updateActiveTabState(isLoading = false)
                                    }
                                }
                            }

                            override fun onRenderProcessGone(
                                view: WebView?,
                                detail: RenderProcessGoneDetail?
                            ): Boolean {
                                (view?.parent as? ViewGroup)?.removeView(view)
                                view?.destroy()
                                val didCrash = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    detail?.didCrash() == true
                                } else false
                                loadError = if (didCrash) {
                                    "Web page process crashed. Tap retry to reload."
                                } else {
                                    "Web page process terminated due to low system memory."
                                }
                                viewModel.updateActiveTabState(isLoading = false)
                                return true
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                viewModel.updateActiveTabState(
                                    progress = newProgress,
                                    isLoading = newProgress < 100
                                )
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                super.onReceivedTitle(view, title)
                                if (!title.isNull_or_blank()) {
                                    viewModel.updateActiveTabState(title = title)
                                }
                            }

                            override fun onPermissionRequest(request: PermissionRequest?) {
                                try {
                                    request?.grant(request.resources)
                                } catch (e: Exception) {
                                    request?.deny()
                                }
                            }

                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: android.os.Message?
                            ): Boolean {
                                if (settings.popupsBlocked && !isUserGesture) {
                                    return false
                                }
                                val transport = resultMsg?.obj as? WebView.WebViewTransport
                                if (transport != null) {
                                    val tempWebView = WebView(view?.context ?: return false)
                                    tempWebView.webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            v: WebView?,
                                            req: WebResourceRequest?
                                        ): Boolean {
                                            req?.url?.toString()?.let { targetUrl ->
                                                viewModel.createNewTab(targetUrl, activeTab.isIncognito)
                                            }
                                            v?.destroy()
                                            return true
                                        }
                                    }
                                    transport.webView = tempWebView
                                    resultMsg.sendToTarget()
                                    return true
                                }
                                return false
                            }
                        }

                        setDownloadListener { url, _, contentDisposition, mimetype, contentLength ->
                            viewModel.startDownload(
                                context = context,
                                url = url,
                                contentDisposition = contentDisposition,
                                mimeType = mimetype,
                                contentLength = contentLength
                            )
                        }

                        if (!activeTab.url.startsWith("albion://") && activeTab.url.isNotBlank()) {
                            loadUrl(activeTab.url)
                            currentLoadedUrl = activeTab.url
                        }
                        webViewInstance = this
                    }
                },
                update = { webView ->
                    configureWebSettings(webView, settings, activeTab.desktopMode)

                    if (activeTab.url != currentLoadedUrl &&
                        !activeTab.url.startsWith("albion://") &&
                        activeTab.url.isNotBlank()
                    ) {
                        currentLoadedUrl = activeTab.url
                        webView.loadUrl(activeTab.url)
                    }

                    if (findQuery.isNotEmpty()) {
                        webView.findAllAsync(findQuery)
                    } else {
                        webView.clearMatches()
                    }
                },
                onRelease = { webView ->
                    webView.stopLoading()
                    webView.onPause()
                    webView.webChromeClient = WebChromeClient()
                    webView.webViewClient = WebViewClient()
                    webView.destroy()
                },
                modifier = Modifier.fillMaxSize()
            )

            loadError?.let { err ->
                ErrorView(
                    errorMessage = err,
                    onRetry = {
                        loadError = null
                        if (webViewInstance != null) {
                            webViewInstance?.reload()
                        } else {
                            viewModel.processInputAndLoad(activeTab.url)
                        }
                    }
                )
            }
        }
    }
}

private fun handleCustomUri(context: Context, url: String): Boolean {
    if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://") || url.startsWith("content://") || url.startsWith("about:") || url.startsWith("albion://") || url.startsWith("view-source:")) {
        return false
    }
    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        true
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.trim().isEmpty()
}

private fun configureWebSettings(
    webView: WebView,
    settings: com.example.data.repository.BrowserSettings,
    isDesktopMode: Boolean
) {
    webView.settings.apply {
        javaScriptEnabled = settings.javascriptEnabled
        domStorageEnabled = true
        useWideViewPort = true
        loadWithOverviewMode = true
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
        mediaPlaybackRequiresUserGesture = false
        allowFileAccess = true
        allowContentAccess = true
        @Suppress("DEPRECATION")
        allowFileAccessFromFileURLs = true
        @Suppress("DEPRECATION")
        allowUniversalAccessFromFileURLs = true
        javaScriptCanOpenWindowsAutomatically = true
        textZoom = settings.textZoom

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                safeBrowsingEnabled = false
            } catch (e: Exception) {
                // Ignore safe browsing initialization exceptions
            }
        }

        userAgentString = if (isDesktopMode) {
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        } else {
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, !settings.thirdPartyCookiesBlocked)
    }
}
