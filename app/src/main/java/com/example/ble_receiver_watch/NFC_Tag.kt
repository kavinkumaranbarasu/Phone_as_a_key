package com.example.ble_receiver_watch

import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.BatteryManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import com.example.ble_receiver_watch.databinding.ActivityBinder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.KITKAT)
public class NFC_Tag : AppCompatActivity,NfcAdapter.ReaderCallback {

    companion object {
        private val TAG = NFC_Tag::class.java.getSimpleName()
        fun clearTagData() {
            // Clear NFC tag data here
            // For example:
            // tagData = null
        }
    }
    private var binder : ActivityBinder? = null
    private val viewModel : MainViewModel by viewModels<MainViewModel>()
    private var lastDetectTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            updateNFCStatus()
            handler.postDelayed(this, 5000)
        }
    }


    constructor() {

    }
    private val batteryLevelReceiver = BatteryLevelReceiver()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState : Bundle?) {
        binder = DataBindingUtil.setContentView(this@NFC_Tag, R.layout.activity_nfc_tag)
        binder?.setViewModel(viewModel)
        binder?.setLifecycleOwner(this@NFC_Tag)
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_nfc_tag)
        Coroutines.main(this@NFC_Tag) { scope ->
            scope.launch(block = {
                binder?.getViewModel()?.observeNFCStatus()?.collectLatest(action = { status ->
                    Log.d(TAG, "observeNFCStatus $status")
                    if (status == NFCStatus.NoOperation) NFCManager.disableReaderMode(
                            this@NFC_Tag,
                            this@NFC_Tag
                    )
                    else if (status == NFCStatus.Tap) NFCManager.enableReaderMode(
                            this@NFC_Tag,
                            this@NFC_Tag,
                            this@NFC_Tag,
                            viewModel.getNFCFlags(),
                            viewModel.getExtras()
                    )
                })
            })
            scope.launch(block = {
                binder?.getViewModel()?.observeToast()?.collectLatest(action = { message ->
                    Log.d(TAG, "observeToast $message")
                    Toast.makeText(this@NFC_Tag, message, Toast.LENGTH_LONG).show()
                })
            })
            scope.launch(block = {
                binder?.getViewModel()?.observeTag()?.collectLatest(action = { tag ->
                    Log.d(TAG, "observeTag $tag")
                    binder?.textViewExplanation?.setText(tag)
                    val tagLines = tag?.lines() ?: emptyList()
                    val lastFourLines = tagLines.takeLast(1)
                    val lastFourChars = lastFourLines.joinToString("") { it.takeLast(4) } // Join lines and take last 4 chars from each
                    if (lastFourChars.isNotEmpty()) {
                        if(lastFourChars=="alse") {
                            Log.d(TAG, "Last 4 lines combined: $lastFourChars")

                            // Check if the device is charging
                            val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                    status == BatteryManager.BATTERY_STATUS_FULL
                            // Only proceed with registering the receiver if the device is not charging
                            val intentFilter = IntentFilter()
                            intentFilter.addAction(Intent.ACTION_POWER_CONNECTED)
                            intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED)
                            //intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED)
                            registerReceiver(batteryLevelReceiver, intentFilter)
                            //unregisterReceiver(batteryLevelReceiver)



                            if (isCharging)
                            {
                                val intentt = Intent(this@NFC_Tag, FullScreenActivity::class.java)
                                startActivity(intentt)
                                Toast.makeText(this@NFC_Tag, "Docked Successfully", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                })
            })
        }
        handler.post(runnable)
    }

    override fun onTagDiscovered(tag : Tag?) {
        binder?.getViewModel()?.readTag(tag)
    }
    private fun updateNFCStatus() {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter != null) {
            if (nfcAdapter.isEnabled) {
                viewModel.onCheckNFC(true)
            } else {
                viewModel.onCheckNFC(false)
            }
        } else {
            viewModel.onCheckNFC(false)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
        Toast.makeText(this@NFC_Tag, "Destroyed", Toast.LENGTH_SHORT).show()


    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

}

