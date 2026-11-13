package com.eduardo.crvlauncher

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import android.webkit.WebViewClient
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Base64
import java.io.ByteArrayOutputStream
import android.widget.ArrayAdapter
import android.widget.Toast
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var web: WebView
    private var geoCallback: GeolocationPermissions.Callback? = null
    private var geoOrigin: String? = null
    private val locationRequest = 4401

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        val assetLoader = WebViewAssetLoader.Builder().addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this)).build()
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = true
        web.settings.javaScriptCanOpenWindowsAutomatically = true
        web.settings.setGeolocationEnabled(true)
        web.addJavascriptInterface(Bridge(this), "AndroidBridge")
        web.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false)
                } else {
                    geoOrigin = origin; geoCallback = callback
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), locationRequest)
                }
            }
        }
        web.settings.allowFileAccess = false
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: android.webkit.WebResourceRequest): android.webkit.WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }
        }
        setContentView(web)
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationRequest) {
            val granted = grantResults.any { it == PackageManager.PERMISSION_GRANTED }
            geoCallback?.invoke(geoOrigin ?: "", granted, false)
            geoCallback = null; geoOrigin = null
            if (!granted) Toast.makeText(this, "Sin ubicación no se puede centrar Waze ni consultar el clima local.", Toast.LENGTH_LONG).show()
        }
    }

    inner class Bridge(private val activity: Activity) {
        @JavascriptInterface fun getAppIcon(pkg: String): String = appIconData(pkg)
        @JavascriptInterface fun getRadioAppIcon(): String {
            val radio = launcherApps().firstOrNull { it.label.contains("radio", true) || it.label.contains("fm", true) }
            return radio?.let { appIconData(it.pkg) } ?: ""
        }
        @JavascriptInterface fun getAppIconByLabel(label: String): String {
            val match = launcherApps().firstOrNull { it.label.equals(label, true) }
                ?: launcherApps().firstOrNull { it.label.contains(label, true) || label.contains(it.label, true) }
            return match?.let { appIconData(it.pkg) } ?: ""
        }
        @JavascriptInterface fun launchAppByLabel(label: String) = activity.runOnUiThread {
            val match = launcherApps().firstOrNull { it.label.equals(label, true) }
                ?: launcherApps().firstOrNull { it.label.contains(label, true) || label.contains(it.label, true) }
            val intent = match?.let { packageManager.getLaunchIntentForPackage(it.pkg) }
            if (intent != null) startActivity(intent)
            else Toast.makeText(activity, "No encontré «$label» entre las aplicaciones instaladas.", Toast.LENGTH_LONG).show()
        }
        @JavascriptInterface fun launchPackage(pkg: String) = activity.runOnUiThread {
            if (pkg.isBlank()) { this@MainActivity.showApps(); return@runOnUiThread }
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) startActivity(intent) else Toast.makeText(activity, "La aplicación no está instalada o no tiene acceso directo.", Toast.LENGTH_LONG).show()
        }
        @JavascriptInterface fun openUrl(url: String) = activity.runOnUiThread {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            catch (_: Exception) { Toast.makeText(activity, "No se pudo abrir el enlace.", Toast.LENGTH_SHORT).show() }
        }
        @JavascriptInterface fun selectMusicApp(pkg: String) = activity.runOnUiThread {
            MediaNotificationListener.preferredPackage = pkg
            ensureMediaAccess()
        }
        @JavascriptInterface fun refreshMedia() { MediaNotificationListener.refresh(activity) }
        @JavascriptInterface fun getMediaState(): String = MediaNotificationListener.stateJson(activity)
        @JavascriptInterface fun mediaPlayPause() = MediaNotificationListener.control("playPause", activity)
        @JavascriptInterface fun mediaNext() = MediaNotificationListener.control("next", activity)
        @JavascriptInterface fun mediaPrevious() = MediaNotificationListener.control("previous", activity)
        @JavascriptInterface fun pickMusicApp() { activity.runOnUiThread { this@MainActivity.showApps(musicOnly = true) } }    
        @JavascriptInterface fun showApps() { activity.runOnUiThread { this@MainActivity.showApps() } }
        @JavascriptInterface fun openRadio() = activity.runOnUiThread { showRadioApps() }
        @JavascriptInterface fun openBluetooth() = activity.runOnUiThread { safeStart(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
        @JavascriptInterface fun openSettings() = activity.runOnUiThread { safeStart(Intent(Settings.ACTION_SETTINGS)) }
        @JavascriptInterface fun openAndroidAuto() = activity.runOnUiThread {
            val pkg = "com.google.android.projection.gearhead"
            val launch = packageManager.getLaunchIntentForPackage(pkg)
            if (launch != null) startActivity(launch) else Toast.makeText(activity, "Android Auto no aparece como aplicación iniciable. Puede activarse al conectar un teléfono compatible.", Toast.LENGTH_LONG).show()
        }
        @JavascriptInterface fun openPhone() = activity.runOnUiThread { safeStart(Intent(Intent.ACTION_DIAL)) }
    }


    private fun appIconData(pkg: String): String {
        return try {
            val drawable = packageManager.getApplicationIcon(pkg)
            val size = 192
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()
            "data:image/png;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) { "" }
    }

    private fun safeStart(intent: Intent) { try { startActivity(intent) } catch (_: Exception) { Toast.makeText(this, "Función no disponible en este equipo.", Toast.LENGTH_SHORT).show() } }

    private fun ensureMediaAccess() {
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        val component = ComponentName(this, MediaNotificationListener::class.java).flattenToString()
        if (!enabled.contains(component)) {
            AlertDialog.Builder(this).setTitle("Permiso para controlar música")
                .setMessage("Activa el acceso de CR-V Launcher a las notificaciones. Así podrá leer título, artista y controlar reproducir, pausa y siguiente en la sesión multimedia activa.")
                .setNegativeButton("Ahora no", null)
                .setPositiveButton("Abrir ajustes") { _, _ -> safeStart(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                .show()
        } else MediaNotificationListener.refresh(this)
    }

    private data class AppEntry(val label: String, val pkg: String)
    private fun launcherApps(): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { AppEntry(it.loadLabel(packageManager).toString(), it.activityInfo.packageName) }
            .distinctBy { it.pkg }.sortedBy { it.label.lowercase() }
    }
    private fun showApps(musicOnly: Boolean = false) {
        val all = launcherApps()
        val entries = if (musicOnly) all.filter { it.label.contains("music", true) || it.label.contains("spotify", true) || it.label.contains("radio", true) || it.label.contains("música", true) } else all
        if (entries.isEmpty()) { Toast.makeText(this, "No se encontraron aplicaciones instaladas.", Toast.LENGTH_SHORT).show(); return }
        AlertDialog.Builder(this).setTitle(if (musicOnly) "Selecciona reproductor" else "Aplicaciones")
            .setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, entries.map { it.label })) { _, which ->
                val chosen = entries[which]
                if (musicOnly) MediaNotificationListener.preferredPackage = chosen.pkg
                packageManager.getLaunchIntentForPackage(chosen.pkg)?.let { startActivity(it) }
            }.setNegativeButton("Cerrar", null).show()
    }
    private fun showRadioApps() {
        val radios = launcherApps().filter { it.label.contains("radio", true) || it.label.contains("fm", true) }
        if (radios.isEmpty()) { Toast.makeText(this, "No encontré una app llamada Radio o FM. Si tu radio es del fabricante, abre Apps y localízala.", Toast.LENGTH_LONG).show(); return }
        AlertDialog.Builder(this).setTitle("Selecciona tu Radio FM")
            .setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, radios.map { it.label })) { _, which ->
                packageManager.getLaunchIntentForPackage(radios[which].pkg)?.let { startActivity(it) }
            }.setNegativeButton("Cancelar", null).show()
    }
}
