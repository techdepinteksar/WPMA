package com.intek.wpma

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.Result
import com.intek.wpma.ChoiseWork.Set.SetInitialization
import me.dm7.barcodescanner.zxing.ZXingScannerView

    var ParentForm: String = ""

class ScanActivity: AppCompatActivity(), ZXingScannerView.ResultHandler {
    private var mScannerView: ZXingScannerView? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        // Programmatically initialize the scanner view
        mScannerView = ZXingScannerView(this)
        // Set the scanner view as the content view
        setContentView(mScannerView)
        ParentForm = intent.extras!!.getString("ParentForm")!!
    }

    override fun onResume() {
        super.onResume()
        // Register ourselves as a handler for scan results.
        mScannerView!!.setResultHandler(this)
        // Start camera on resume
        mScannerView!!.startCamera()

    }

    override fun onPause() {
        super.onPause()
        // Stop camera on pause
        mScannerView!!.stopCamera()
    }

    override fun handleResult(rawResult: Result) { // Do something with the result here
        // Do something with the result here
        // Log.v("tag", rawResult.getText()); // Prints scan results
        // Log.v("tag", rawResult.getBarcodeFormat().toString()); // Prints the scan format (qrcode, pdf417 etc.)

        when(ParentForm){
            "MainActivity" -> MainActivity.scanRes = rawResult.text
            "SetInitialization" -> SetInitialization.scanRes = rawResult.text
        }


        onBackPressed()

        // If you would like to resume scanning, call this method below:
        //mScannerView.resumeCameraPreview(this);
    }
}