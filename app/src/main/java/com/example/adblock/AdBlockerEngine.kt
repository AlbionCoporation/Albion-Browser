package com.example.adblock

import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

object AdBlockerEngine {

    // High-performance HashSet for O(1) domain checks
    private val AD_HOST_SET = setOf(
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "adservice.google.com",
        "adnxs.com",
        "amazon-adsystem.com",
        "taboola.com",
        "outbrain.com",
        "popads.net",
        "popcash.net",
        "adroll.com",
        "criteo.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "admob.com",
        "advertising.com",
        "casalemedia.com",
        "smartadserver.com",
        "adform.net",
        "adform.com",
        "propellerads.com",
        "mgid.com",
        "exoclick.com",
        "juicyads.com",
        "adsterra.com",
        "zeropark.com",
        "inmobi.com",
        "vungle.com",
        "tapjoy.com",
        "chartboost.com",
        "applovin.com",
        "ironsrc.com",
        "adcolony.com",
        "aaxads.com",
        "bidswitch.net",
        "indexww.com",
        "360yield.com",
        "moatads.com",
        "serving-sys.com",
        "zemanta.com",
        "ad-delivery.net",
        "adservice.com",
        "ad-system.com",
        "adsystem.com",
        "adtech.de",
        "adtech.com",
        "adacado.com",
        "adzerk.net",
        "adblade.com",
        "trafficfactory.biz",
        "yieldmanager.com",
        "exponential.com",
        "revcontent.com",
        "media.net",
        "adcash.com",
        "clickadu.com",
        "hilltopads.com",
        "popmyads.com",
        "adsupply.com",
        "ad2games.com",
        "mathtag.com",
        "bidtheatre.com",
        "media6degrees.com",
        "rlcdn.com",
        "tapad.com",
        "bluekai.com",
        "krxd.net",
        "exelator.com",
        "demdex.net",
        "agkn.com",
        "turn.com"
    )

    private val TRACKER_HOST_SET = setOf(
        "google-analytics.com",
        "segment.io",
        "mixpanel.com",
        "hotjar.com",
        "scorecardresearch.com",
        "quantserve.com",
        "clarity.ms",
        "newrelic.com",
        "sentry.io",
        "bugsnag.com",
        "chartbeat.com",
        "crazyegg.com",
        "amplitude.com",
        "mouseflow.com",
        "fullstory.com",
        "pixel.facebook.com",
        "connect.facebook.net",
        "telemetry.microsoft.com",
        "stats.wp.com",
        "mc.yandex.ru",
        "an.yandex.ru",
        "optimizely.com",
        "omtrdc.net",
        "parsely.com",
        "branch.io",
        "adjust.com",
        "appsflyer.com",
        "singular.net",
        "kochava.com"
    )

    private val PATH_PATTERNS = listOf(
        "/pagead/",
        "/adserver/",
        "/adstream/",
        "/popunder",
        "/popup.js",
        "/show_ads.js",
        "/adsbygoogle.js",
        "/adservice.js",
        "/prebid.js",
        "/gpt.js",
        "/fbevents.js",
        "tracking.js",
        "analytics.js",
        "telemetry"
    )

    sealed class BlockResult {
        object None : BlockResult()
        object BlockedAd : BlockResult()
        object BlockedTracker : BlockResult()
    }

    /**
     * Checks whether a given URL should be blocked based on user settings and whitelist.
     */
    fun shouldBlock(
        url: String,
        pageHost: String?,
        adBlockEnabled: Boolean,
        trackerBlockEnabled: Boolean,
        whitelistedDomains: Set<String>
    ): BlockResult {
        if (!adBlockEnabled && !trackerBlockEnabled) return BlockResult.None

        // Skip if main page host is whitelisted
        pageHost?.let {
            if (isWhitelisted(it, whitelistedDomains)) return BlockResult.None
        }

        val uri = try {
            Uri.parse(url)
        } catch (e: Exception) {
            return BlockResult.None
        }

        val requestHost = uri.host?.lowercase() ?: ""
        if (requestHost.isEmpty()) return BlockResult.None

        // Skip if requested host itself is whitelisted
        if (isWhitelisted(requestHost, whitelistedDomains)) return BlockResult.None

        val lowerUrl = url.lowercase()

        // 1. Tracker Check
        if (trackerBlockEnabled) {
            if (isDomainMatched(requestHost, TRACKER_HOST_SET)) {
                return BlockResult.BlockedTracker
            }
        }

        // 2. Ad Check
        if (adBlockEnabled) {
            if (isDomainMatched(requestHost, AD_HOST_SET)) {
                return BlockResult.BlockedAd
            }

            val path = uri.path?.lowercase() ?: ""
            for (pattern in PATH_PATTERNS) {
                if (path.contains(pattern) || lowerUrl.contains(pattern)) {
                    return BlockResult.BlockedAd
                }
            }
        }

        return BlockResult.None
    }

    private fun isDomainMatched(host: String, targetSet: Set<String>): Boolean {
        if (targetSet.contains(host)) return true
        // Check root domain suffix match (e.g. sub.doubleclick.net -> doubleclick.net)
        var dotIndex = host.indexOf('.')
        while (dotIndex != -1 && dotIndex < host.length - 1) {
            val parentDomain = host.substring(dotIndex + 1)
            if (targetSet.contains(parentDomain)) return true
            dotIndex = host.indexOf('.', dotIndex + 1)
        }
        return false
    }

    private fun isWhitelisted(host: String, whitelistedDomains: Set<String>): Boolean {
        val lowerHost = host.lowercase()
        return whitelistedDomains.any { domain ->
            val lowerDomain = domain.lowercase()
            lowerHost == lowerDomain || lowerHost.endsWith(".$lowerDomain")
        }
    }

    /**
     * Creates a content-type aware empty WebResourceResponse to drop blocked requests silently
     * without throwing network errors on the page.
     */
    fun createEmptyResponse(url: String = ""): WebResourceResponse {
        val mimeType = when {
            url.endsWith(".js") || url.contains(".js?") -> "application/javascript"
            url.endsWith(".css") || url.contains(".css?") -> "text/css"
            url.endsWith(".png") || url.endsWith(".jpg") || url.endsWith(".gif") || url.endsWith(".webp") -> "image/png"
            url.endsWith(".json") -> "application/json"
            else -> "text/plain"
        }

        return WebResourceResponse(
            mimeType,
            "UTF-8",
            200,
            "OK",
            mapOf(
                "Access-Control-Allow-Origin" to "*",
                "Cache-Control" to "no-cache"
            ),
            ByteArrayInputStream("".toByteArray())
        )
    }

    /**
     * JavaScript snippet that injects CSS element-hiding rules to collapse remaining empty ad containers
     * without breaking website layout or functionality.
     */
    fun getAdHidingJs(): String {
        return """
            (function() {
                try {
                    if (window.__albion_adblock_injected) return;
                    window.__albion_adblock_injected = true;
                    var css = 'ins.adsbygoogle, .adsbygoogle, [id*="google_ads"], [class*="ad-banner"], [class*="ad_banner"], [class*="banner-ad"], [id*="ad-banner"], [id*="ad_banner"], [id*="banner-ad"], div[class*="sponsored-ad"], div[id*="sponsored-ad"], .taboola-placeholder, .outbrain-template, .ad-container, .ad_container, .ad-slot, .ad_slot, .ad-wrapper, .ad_wrapper, iframe[src*="doubleclick"], iframe[src*="googlesyndication"] { display: none !important; visibility: hidden !important; height: 0 !important; max-height: 0 !important; opacity: 0 !important; pointer-events: none !important; }';
                    var style = document.createElement('style');
                    style.type = 'text/css';
                    style.appendChild(document.createTextNode(css));
                    (document.head || document.documentElement).appendChild(style);
                } catch(e) {}
            })();
        """.trimIndent()
    }
}
