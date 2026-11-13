package com.eduardo.crvlauncher

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.util.Log
import org.json.JSONObject

class MediaNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() { super.onListenerConnected(); refresh(this) }
    override fun onNotificationPosted(sbn: android.service.notification.StatusBarNotification?) { refresh(this) }
    override fun onNotificationRemoved(sbn: android.service.notification.StatusBarNotification?) { refresh(this) }

    companion object {
        @Volatile var preferredPackage: String = "com.google.android.apps.youtube.music"
        @Volatile private var active: MediaController? = null

        fun refresh(context: Context) {
            try {
                val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
                val sessions = manager.getActiveSessions(ComponentName(context, MediaNotificationListener::class.java))
                active = sessions.firstOrNull { it.packageName == preferredPackage }
                    ?: sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                    ?: sessions.firstOrNull()
            } catch (e: SecurityException) { Log.w("CRVLauncher", "Media notification access not granted", e); active = null }
            catch (e: Exception) { active = null }
        }

        fun stateJson(context: Context): String {
            refresh(context)
            val c = active ?: return JSONObject().put("title", "").toString()
            val md = c.metadata
            val state = c.playbackState?.state ?: PlaybackState.STATE_NONE
            val pkg = c.packageName
            val appName = try { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString() } catch (_: Exception) { pkg }
            return JSONObject()
                .put("title", md?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "")
                .put("artist", md?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: "")
                .put("album", md?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "")
                .put("packageName", pkg)
                .put("appName", appName)
                .put("playing", state == PlaybackState.STATE_PLAYING)
                .put("duration", md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L)
                .put("position", c.playbackState?.position ?: 0L)
                .toString()
        }

        fun control(action: String, context: Context) {
            refresh(context)
            val c = active
            if (c == null) { Log.w("CRVLauncher", "No active media session"); return }
            try {
                when (action) {
                    "playPause" -> if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play()
                    "next" -> c.transportControls.skipToNext()
                    "previous" -> c.transportControls.skipToPrevious()
                }
            } catch (e: Exception) { Log.w("CRVLauncher", "Media command failed", e) }
        }
    }
}
