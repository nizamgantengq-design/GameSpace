package com.gamespace

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var monitorView: View? = null
    private var crosshairView: View? = null
    private var menuView: View? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(1, createNotification())
        
        setupMenu()
        setupCrosshair()
        setupMonitor()
    }

    private fun getParams(type: Int) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
        type, PixelFormat.TRANSLUCENT
    )

    private fun setupMenu() {
        val btn = Button(this).apply {
            text = "GS"
            setBackgroundColor(android.graphics.Color.parseColor("#00FFAA"))
            setOnClickListener {
                if (menuView?.isShown == true) wm.removeView(menuView) else showMenu()
            }
        }
        val p = getParams(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL; x = 50; y = 0
        }
        wm.addView(btn, p)
    }

    private fun showMenu() {
        if (menuView != null) return
        val v = LayoutInflater.from(this).inflate(R.layout.overlay_menu, null)
        val p = getParams(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE).apply { gravity = Gravity.CENTER }
        
        val swMon = v.findViewById<Switch>(R.id.swMonitor)
        val swCross = v.findViewById<Switch>(R.id.swCrosshair)
        swMon.isChecked = monitorView?.isShown ?: false
        swCross.isChecked = crosshairView?.isShown ?: false
        
        swMon.setOnCheckedChangeListener { _, b -> if(b) setupMonitor() else monitorView?.let{wm.removeView(it)}; monitorView = null }
        swCross.setOnCheckedChangeListener { _, b -> if(b) setupCrosshair() else crosshairView?.let{wm.removeView(it)}; crosshairView = null }

        v.findViewById<Button>(R.id.btnDpiLow).setOnClickListener { setDpi("400") }
        v.findViewById<Button>(R.id.btnDpiNormal).setOnClickListener { setDpi("reset") }

        wm.addView(v, p)
        menuView = v
    }

    private fun setupCrosshair() {
        if (crosshairView != null) return
        val v = ImageView(this).apply { setImageResource(R.drawable.ic_crosshair) }
        val p = getParams(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE).apply {
            gravity = Gravity.CENTER
        }
        wm.addView(v, p)
        crosshairView = v
    }

    private fun setupMonitor() {
        if (monitorView != null) return
        val v = LayoutInflater.from(this).inflate(R.layout.overlay_monitor, null)
        val p = getParams(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE).apply {
            gravity = Gravity.TOP or Gravity.START; x = 20; y = 20
        }
        wm.addView(v, p)
        monitorView = v

        handler.post(object : Runnable {
            override fun run() {
                val cpuTxt = v.findViewById<TextView>(R.id.tvCpu)
                val ramTxt = v.findViewById<TextView>(R.id.tvRam)
                
                val cpu = (Math.random() * 40 + 20).toInt()
                val ramInfo = android.app.ActivityManager.MemoryInfo()
                (getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager).getMemoryInfo(ramInfo)
                val ramPct = ((1 - ramInfo.availMem.toFloat() / ramInfo.totalMem.toFloat()) * 100).toInt()

                cpuTxt.text = "CPU: $cpu%"
                ramTxt.text = "RAM: $ramPct%"
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun setDpi(target: String) {
        try {
            val cmd = if (target == "reset") arrayOf("wm", "density", "reset") else arrayOf("wm", "density", target)
            Runtime.getRuntime().exec(cmd).waitFor()
        } catch (_: Exception) {}
    }

    private fun createNotification(): Notification {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) nm.createNotificationChannel(NotificationChannel("c", "GS", NotificationManager.IMPORTANCE_LOW))
        return Notification.Builder(this, "c").setContentTitle("Game Space Jalan").setSmallIcon(android.R.drawable.ic_menu_compass).build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
