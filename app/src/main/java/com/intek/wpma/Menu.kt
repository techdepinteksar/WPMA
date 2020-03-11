package com.intek.wpma

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import com.intek.wpma.ChoiseWork.Set.SetInitialization
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_menu.*
import kotlinx.android.synthetic.main.activity_menu.terminalView
import java.math.BigInteger


class Menu : BarcodeDataReceiver() {

    var Barcode: String = ""
    var codeId:String = ""  //показатель по которому можно различать типы штрих-кодов
    var Employer: String = ""
    var EmployerFlags: String = ""
    var EmployerIDD: String = ""
    var EmployerID: String = ""
    var ParentForm: String = ""
    var isMobile = false    //флаг мобильного устройства

    val barcodeDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("IntentApiSample: ", "onReceive")
            if (ACTION_BARCODE_DATA == intent.action) {
                val version = intent.getIntExtra("version", 0)
                if (version >= 1) {
                    try {
                        Barcode = intent.getStringExtra("data")
                        codeId = intent.getStringExtra("codeId")
                        reactionBarcode(Barcode)
                    }
                    catch (e: Exception){
                        val toast = Toast.makeText(applicationContext, "Отсутствует соединение с базой!", Toast.LENGTH_LONG)
                        toast.show()
                    }
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        Employer = intent.extras!!.getString("Employer")!!
        EmployerFlags = intent.extras!!.getString("EmployerFlags")!!
        EmployerIDD = intent.extras!!.getString("EmployerIDD")!!
        EmployerID = intent.extras!!.getString("EmployerID")!!
        ParentForm = intent.extras!!.getString("ParentForm")!!
        terminalView.text = intent.extras!!.getString("terminalView")!!
        isMobile = intent.extras!!.getString("isMobile")!!.toBoolean()
        ANDROID_ID =  Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        title = Employer

        btnSet.setOnClickListener {
            val SetInit = Intent(this, SetInitialization::class.java)
            SetInit.putExtra("Employer", Employer)
            SetInit.putExtra("EmployerIDD",EmployerIDD)
            SetInit.putExtra("EmployerFlags",EmployerFlags)
            SetInit.putExtra("EmployerID",EmployerID)
            SetInit.putExtra("terminalView",terminalView.text.trim())
            SetInit.putExtra("isMobile",isMobile.toString())
            SetInit.putExtra("ParentForm","Menu")
            startActivity(SetInit)
        }
    }

    private fun reactionBarcode(Barcode: String){
        //выход из сессии
        if(EmployerIDD == "99990" + Barcode.substring(2, 4) + "00" + Barcode.substring(4, 12)){
            if(!Logout(EmployerID)){
                Lbl.text = "Ошибка выхода из системы!"
                return
            }
            val Main = Intent(this, MainActivity::class.java)
            startActivity(Main)
            finish()
            return
        }
        else  {
            Lbl.text = "Нет действий с ШК в данном режиме!"
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        if (keyCode == 7)
        {
            //нажали 0
            startActivity(7 )
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun startActivity(num: Int) {
        var intent: Intent

        if (num == 7) {     // режим отбора
            val SetInit = Intent(this, SetInitialization::class.java)
            SetInit.putExtra("Employer", Employer)
            SetInit.putExtra("EmployerIDD",EmployerIDD)
            SetInit.putExtra("EmployerFlags",EmployerFlags)
            SetInit.putExtra("EmployerID",EmployerID)
            SetInit.putExtra("terminalView",terminalView.text.trim())
            SetInit.putExtra("isMobile",isMobile.toString())
            SetInit.putExtra("ParentForm","Menu")
            startActivity(SetInit)
        }
    }

    override fun onResume() {
        super.onResume()
        //        IntentFilter intentFilter = new IntentFilter("hsm.RECVRBI");
        registerReceiver(barcodeDataReceiver, IntentFilter(ACTION_BARCODE_DATA))
        claimScanner()
        Log.d("IntentApiSample: ", "onResume")
        if(MainActivity.scanRes != null){
            try {
                reactionBarcode(MainActivity.scanRes.toString())
            }
            catch (e: Exception){
                val toast = Toast.makeText(applicationContext, "Отсутствует соединение с базой!", Toast.LENGTH_LONG)
                toast.show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(barcodeDataReceiver)
        releaseScanner()
        Log.d("IntentApiSample: ", "onPause")
    }


}
