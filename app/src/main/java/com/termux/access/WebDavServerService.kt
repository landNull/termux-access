package com.termux.access

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import java.io.File
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicReference

class WebDavServerService : Service() {
    private var server: NettyApplicationEngine? = null
    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val currentPort = AtomicReference(8080)
    private val appsRoot = File("/data/data/com.termux/files/home/Apps")

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundWithNotification()
        startServer()
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val channelId = "termux_access_server"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(channelId, "WebDAV Server", NotificationManager.IMPORTANCE_LOW))
        val stopIntent = PendingIntent.getService(this, 0, Intent(this, WebDavServerService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Termux:Access WebDAV")
            .setContentText("Serving Apps on port ${currentPort.get()}")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .build()
        startForeground(1, notification)
    }

    private fun startServer() {
        val port = findFreePort(8080)
        currentPort.set(port)
        server = embeddedServer(Netty, port = port, host = "0.0.0.0") {
            installWebDav(appsRoot)
        }.start(wait = false)
        advertise(port)
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TermuxAccess::WebDav").apply { acquire(10 * 60 * 1000L) }
    }

    private fun advertise(port: Int) {
        nsdManager = getSystemService(NsdManager::class.java)
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "Termux Access"
            serviceType = "_termuxaccess._tcp"
            setPort(port)
        }
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {}
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
            override fun onServiceUnregistered(info: NsdServiceInfo) {}
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
        }
        nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    private fun findFreePort(start: Int): Int {
        for (p in start until start + 20) {
            try { ServerSocket(p).use { return p } } catch (_: Exception) {}
        }
        return start
    }

    override fun onDestroy() {
        registrationListener?.let { nsdManager?.unregisterService(it) }
        server?.stop(1000, 2000)
        wakeLock?.release()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.termux.access.STOP"
    }
}
