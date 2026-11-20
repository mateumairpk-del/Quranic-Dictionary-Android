package com.quranicdictionary.app

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONObject
import java.util.Calendar
import kotlin.random.Random

object ReminderScheduler {
 private const val CHANNEL = "quran_reminders"
 fun ensureChannel(ctx: Context) {
  val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  nm.createNotificationChannel(NotificationChannel(CHANNEL, "Quran learning reminders", NotificationManager.IMPORTANCE_DEFAULT))
 }
 fun notifyNow(ctx: Context) {
  ensureChannel(ctx)
  if (Build.VERSION.SDK_INT >= 33 && ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
  val messages = arrayOf("Aaj kuch Quranic alfaaz yaad karein.", "Rozana thori practice se Quranic vocabulary mazboot hoti hai.", "Aaj flashcards aur quiz se apni practice karein.")
  val intent = Intent(ctx, MainActivity::class.java)
  val pi = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  val n = Notification.Builder(ctx, CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info)
   .setContentTitle("Quranic Dictionary").setContentText(messages.random()).setAutoCancel(true).setContentIntent(pi).build()
  (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(Random.nextInt(1,100000), n)
 }
 fun schedule(ctx: Context) {
  val alarm = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
  for (i in 0..39) {
   val pi = alarmIntent(ctx,i)
   alarm.cancel(pi)
  }
  val obj = try { JSONObject(ctx.getSharedPreferences("reminders",Context.MODE_PRIVATE).getString("settings", "{}")!!) } catch (_: Exception) { JSONObject() }
  if (!obj.optBoolean("enabled",false)) return
  val times = obj.optJSONArray("times") ?: return
  val repeat = obj.optString("repeat","daily")
  for (i in 0 until minOf(times.length(), 32)) {
   val parts = times.optString(i).split(":")
   if (parts.size != 2) continue
   val h=parts[0].toIntOrNull() ?: continue; val m=parts[1].toIntOrNull() ?: continue
   if (h !in 0..23 || m !in 0..59) continue
   val cal=Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY,h);set(Calendar.MINUTE,m);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0) }
   if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DATE,1)
   if (repeat == "weekdays") while (cal.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY,Calendar.SUNDAY)) cal.add(Calendar.DATE,1)
   setAlarm(ctx,alarm,i,cal.timeInMillis)
  }
  // Random reminders are scheduled independently; they are not guaranteed exact under Doze.
  if (obj.optBoolean("random",false)) setAlarm(ctx,alarm,39,System.currentTimeMillis()+ (45+Random.nextInt(166))*60_000L)
 }
 private fun alarmIntent(ctx:Context,id:Int):PendingIntent = PendingIntent.getBroadcast(ctx,id,Intent(ctx,ReminderReceiver::class.java).putExtra("slot",id),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
 private fun setAlarm(ctx:Context,alarm:AlarmManager,id:Int,at:Long) {
  val pi=alarmIntent(ctx,id)
  try {
   if (Build.VERSION.SDK_INT >= 31 && !alarm.canScheduleExactAlarms()) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi)
   else alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi)
  } catch (_:SecurityException) { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi) }
 }
 fun onAlarm(ctx:Context,slot:Int) {
  notifyNow(ctx)
  // Reschedule all slots, but avoid resetting other slots to the next day before their alarms fire.
  val obj=try { JSONObject(ctx.getSharedPreferences("reminders",Context.MODE_PRIVATE).getString("settings","{}")!!) } catch (_:Exception){JSONObject()}
  if (!obj.optBoolean("enabled",false)) return
  val alarm=ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
  if (slot == 39) { setAlarm(ctx,alarm,39,System.currentTimeMillis()+(45+Random.nextInt(166))*60_000L); return }
  val parts=obj.optJSONArray("times")?.optString(slot)?.split(":") ?: return
  if(parts.size!=2) return
  val h=parts[0].toIntOrNull() ?: return;val m=parts[1].toIntOrNull() ?: return
  val cal=Calendar.getInstance().apply {set(Calendar.HOUR_OF_DAY,h);set(Calendar.MINUTE,m);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0);add(Calendar.DATE,1)}
  val repeat=obj.optString("repeat","daily")
  if(repeat=="weekdays") while(cal.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY,Calendar.SUNDAY))cal.add(Calendar.DATE,1)
  if(repeat=="every2h" || repeat=="every4h") cal.timeInMillis=System.currentTimeMillis()+(if(repeat=="every2h")2 else 4)*3600_000L
  setAlarm(ctx,alarm,slot,cal.timeInMillis)
 }
}
