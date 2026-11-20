package com.quranicdictionary.app

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.speech.tts.TextToSpeech
import android.widget.Toast
import java.util.Locale
import org.json.JSONObject

class MainActivity : Activity() {
 private lateinit var web: WebView
 private var tts: TextToSpeech? = null
 private var ttsReady = false
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  tts = TextToSpeech(this) { status ->
   if (status == TextToSpeech.SUCCESS) {
    val result = tts?.setLanguage(Locale.forLanguageTag("ar-SA"))
    ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
   }
  }
  web = WebView(this)
  setContentView(web)
  web.settings.javaScriptEnabled = true
  web.settings.domStorageEnabled = true
  web.settings.allowFileAccess = true
  web.settings.cacheMode = WebSettings.LOAD_DEFAULT
  web.webViewClient = object : WebViewClient() {
   override fun onReceivedError(
    view: WebView?,
    request: android.webkit.WebResourceRequest?,
    error: android.webkit.WebResourceError?
   ) {
    super.onReceivedError(view, request, error)
    android.util.Log.e("QuranApp", "WebView error: ${request?.url} - ${error?.description}")
   }
  }
  web.webChromeClient = WebChromeClient()
  web.addJavascriptInterface(Bridge(), "AndroidBridge")
  web.settings.useWideViewPort = true
  web.settings.textZoom = 100
  web.settings.loadWithOverviewMode = true
  web.settings.useWideViewPort = true
  web.settings.loadWithOverviewMode = true
  web.loadUrl("file:///android_asset/index.html")
  ReminderScheduler.ensureChannel(this)
 }
 inner class Bridge {
  @JavascriptInterface fun speakArabic(text: String) {
   runOnUiThread {
    if (ttsReady) {
     tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "quran-arabic")
    } else {
     Toast.makeText(this@MainActivity, "Arabic voice not installed. Phone Settings > Text-to-speech > Install Arabic voice.", Toast.LENGTH_LONG).show()
    }
   }
  }

  @JavascriptInterface fun updateReminders(json: String) {
   try {
    JSONObject(json) // Validate before persisting.
    getSharedPreferences("reminders", MODE_PRIVATE).edit().putString("settings", json).apply()
    ReminderScheduler.schedule(this@MainActivity)
   } catch (_: Exception) {}
  }
  @JavascriptInterface fun requestNotificationPermission() {
   runOnUiThread {
    if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
     requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
    if (Build.VERSION.SDK_INT >= 31) {
     val alarm = getSystemService(ALARM_SERVICE) as AlarmManager
     if (!alarm.canScheduleExactAlarms()) {
      // Alarms fall back to inexact delivery; user can grant exact alarms in system settings.
     }
    }
   }
  }
  @JavascriptInterface fun testNotification() { ReminderScheduler.notifyNow(this@MainActivity) }
 }
 override fun onBackPressed() {
  if (web.canGoBack()) web.goBack() else super.onBackPressed()
 }
 override fun onDestroy() { tts?.stop(); tts?.shutdown(); web.destroy(); super.onDestroy() }
}
