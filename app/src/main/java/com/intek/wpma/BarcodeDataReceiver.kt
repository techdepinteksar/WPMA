package com.intek.wpma

import android.content.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.intek.wpma.SQL.SQL1S


abstract class BarcodeDataReceiver: AppCompatActivity() {

    val TAG = "IntentApiSample"
    val ACTION_BARCODE_DATA = "com.honeywell.sample.action.BARCODE_DATA"
    val ACTION_CLAIM_SCANNER = "com.honeywell.aidc.action.ACTION_CLAIM_SCANNER"
    val ACTION_RELEASE_SCANNER = "com.honeywell.aidc.action.ACTION_RELEASE_SCANNER"
    val EXTRA_SCANNER = "com.honeywell.aidc.extra.EXTRA_SCANNER"
    val EXTRA_PROFILE = "com.honeywell.aidc.extra.EXTRA_PROFILE"
    val EXTRA_PROPERTIES = "com.honeywell.aidc.extra.EXTRA_PROPERTIES"
    val EXTRA_CONTROL = "com.honeywell.aidc.action.ACTION_CONTROL_SCANNER"
    val EXTRA_SCAN = "com.honeywell.aidc.extra.EXTRA_SCAN"

    var sdkVersion = 0

    val SS: SQL1S = SQL1S()

    fun sendImplicitBroadcast(ctxt: Context, i: Intent) {
        val pm = ctxt.packageManager
        val matches = pm.queryBroadcastReceivers(i, 0)


        for (resolveInfo in matches) {
            val explicit = Intent(i)
            val cn = ComponentName(
                resolveInfo.activityInfo.applicationInfo.packageName,
                resolveInfo.activityInfo.name
            )

            explicit.component = cn
            ctxt.sendBroadcast(explicit)
        }
    }

    fun mysendBroadcast(intent: Intent) {
        if (sdkVersion < 26) {
            sendBroadcast(intent)
        } else {
            //for Android O above "gives W/BroadcastQueue: Background execution not allowed: receiving Intent"
            //either set targetSDKversion to 25 or use implicit broadcast
            sendImplicitBroadcast(applicationContext, intent)
        }

    }
    fun releaseScanner() {
        Log.d("IntentApiSample: ", "releaseScanner")
        mysendBroadcast(Intent(ACTION_RELEASE_SCANNER))
    }

    fun claimScanner() {
        Log.d("IntentApiSample: ", "claimScanner")
        val properties = Bundle()
        properties.putBoolean("DPR_DATA_INTENT", true)
        properties.putString("DPR_DATA_INTENT_ACTION", ACTION_BARCODE_DATA)

        properties.putInt("TRIG_AUTO_MODE_TIMEOUT", 2)
        properties.putString(
            "TRIG_SCAN_MODE",
            "readOnRelease"
        ) //This works for Hardware Trigger only! If scan is started from code, the code is responsible for a switching off the scanner before a decode

        mysendBroadcast(
            Intent(ACTION_CLAIM_SCANNER)
                .putExtra(EXTRA_SCANNER, "dcs.scanner.imager")
                .putExtra(EXTRA_PROFILE, "DEFAULT")// "MyProfile1")
                .putExtra(EXTRA_PROPERTIES, properties)
        )
    }





}