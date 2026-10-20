package space.club0451.store

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import space.club0451.store.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var updater: AppUpdate
    private var pendingPermissionRequest: PermissionRequest? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        updater = AppUpdate(
            this,
            binding.updateBar,
            binding.updateStatus,
            binding.updateProgress,
        )

        val web = binding.webView
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            mediaPlaybackRequiresUserGesture = false
            userAgentString = "$userAgentString CompClubStore/${BuildConfig.VERSION_NAME}"
        }
        web.settings.setSupportZoom(false)
        web.settings.builtInZoomControls = false
        web.settings.displayZoomControls = false

        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest,
            ): Boolean {
                val url = request.url
                val path = url.path.orEmpty()
                if (path == "/store-app.apk" || path.endsWith("/store-app.apk")) {
                    updater.downloadFromSite()
                    return true
                }
                if (isClubHost(url) && !isStorePath(path)) {
                    view.loadUrl(clubUrl("/store/login"))
                    return true
                }
                return false
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                val needsCamera = request.resources.any {
                    it == PermissionRequest.RESOURCE_VIDEO_CAPTURE
                }
                if (!needsCamera) {
                    request.grant(request.resources)
                    return
                }
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.CAMERA,
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    request.grant(request.resources)
                } else {
                    pendingPermissionRequest = request
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                        REQ_MEDIA,
                    )
                }
            }
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (web.canGoBack()) web.goBack() else finish()
                }
            },
        )

        if (savedInstanceState == null) {
            web.loadUrl(resolveStartUrl(intent?.data))
        }
        updater.start()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        binding.webView.loadUrl(resolveStartUrl(intent.data))
    }

    override fun onResume() {
        super.onResume()
        if (::updater.isInitialized) updater.onResume()
    }

    override fun onDestroy() {
        if (::updater.isInitialized) updater.stop()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQ_MEDIA) return
        val req = pendingPermissionRequest ?: return
        pendingPermissionRequest = null
        if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            req.grant(req.resources)
        } else {
            req.deny()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        binding.webView.restoreState(savedInstanceState)
    }

    companion object {
        private const val REQ_MEDIA = 41

        fun clubUrl(path: String): String {
            return BuildConfig.CLUB_URL.trimEnd('/') + path
        }

        fun isClubHost(url: Uri): Boolean {
            val clubHost = Uri.parse(BuildConfig.CLUB_URL).host.orEmpty()
            val host = url.host.orEmpty()
            return host.equals(clubHost, ignoreCase = true)
        }

        fun isStorePath(path: String): Boolean {
            return path == "/store"
                || path.startsWith("/store/")
                || path == "/admin/salary"
                || path.startsWith("/admin/salary/")
                || path == "/admin/store"
                || path.startsWith("/admin/store/")
                || path == "/admin/logout"
                || path == "/admin/docs"
                || path.startsWith("/admin/docs/")
                || path == "/admin/api"
                || path.startsWith("/admin/api/")
                || path == "/pc"
                || path.startsWith("/pc/")
        }
    }

    private fun resolveStartUrl(data: Uri?): String {
        if (data != null && isClubHost(data) && isStorePath(data.path.orEmpty())) {
            return data.toString()
        }
        return clubUrl("/store/login")
    }
}
