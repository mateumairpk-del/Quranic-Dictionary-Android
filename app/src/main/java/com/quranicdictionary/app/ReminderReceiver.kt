package com.quranicdictionary.app
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
class ReminderReceiver: BroadcastReceiver() {
 override fun onReceive(context:Context,intent:Intent) { ReminderScheduler.onAlarm(context,intent.getIntExtra("slot",0)) }
}
