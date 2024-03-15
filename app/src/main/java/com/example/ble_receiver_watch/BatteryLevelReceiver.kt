package com.example.ble_receiver_watch


import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi

class BatteryLevelReceiver : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                // Battery plugin connected
                val intentt = Intent(context, FullScreenActivity::class.java)
                context.startActivity(intentt)

                Toast.makeText(context, "Battery plugin connected!", Toast.LENGTH_SHORT).show()
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                // Battery plugin disconnected
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                if (!isCharging) {
                    // Only proceed if the device is not charging
                    val i = Intent(context, NFC_Tag::class.java)
                    (context as Activity).finish()
                    context.startActivity(i)

                    Log.i("Charging", "Not charging")
                    Toast.makeText(context, "Calling NFC_Tag Class", Toast.LENGTH_SHORT).show()


                }
                Toast.makeText(context, "Battery plugin disconnected!", Toast.LENGTH_SHORT).show()
            }
            Intent.ACTION_BATTERY_CHANGED -> {
                // This broadcast is not reliable for determining charging status, so it's better to rely on ACTION_POWER_CONNECTED/DISCONNECTED
                // However, you can still check if the device is charging
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                BatteryManager.ACTION_CHARGING
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                if (isCharging) {
                    // Battery is charging
                    val intentt = Intent(context, FullScreenActivity::class.java)
                    context.startActivity(intentt)
                    Toast.makeText(context, "Docked Successfully", Toast.LENGTH_SHORT).show()

                    NFC_Tag.clearTagData()
                }
            }
        }
    }
}


//class BatteryLevelReceiver : BroadcastReceiver() {
//    @RequiresApi(Build.VERSION_CODES.KITKAT)
//    override fun onReceive(context: Context, intent: Intent) {
//
//        if (intent.action == Intent.ACTION_POWER_CONNECTED) {
//
//            // Battery plugin connected
//            val intentt = Intent(context, FullScreenActivity::class.java)
//            context.startActivity(intentt)
//            Toast.makeText(context, "Battery plugin connected!", Toast.LENGTH_SHORT).show()
//        } else if (intent.action == Intent.ACTION_POWER_DISCONNECTED) {
//            // Battery plugin disconnected
//            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
//            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
//                    status == BatteryManager.BATTERY_STATUS_FULL
//            if (!isCharging) {
//                val i = Intent(context, NFC_Tag::class.java)
//                //i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//
//                context.startActivity(i)
//                Log.i("Charging","Not charging")
//                Toast.makeText(context, "Calling NFC_Tag Class", Toast.LENGTH_SHORT).show()
////                val intentt = Intent(context, NFC_Tag::class.java)
////                context.startActivity(intentt)
//            }
//            Toast.makeText(context, "Battery plugin disconnected!", Toast.LENGTH_SHORT).show()
//        }
//        else if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
//            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
//            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
//                    status == BatteryManager.BATTERY_STATUS_FULL
//            if (isCharging) {
//                val intentt = Intent(context, FullScreenActivity::class.java)
//                context.startActivity(intentt)
//                Toast.makeText(context, "Docked Successfully", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//}