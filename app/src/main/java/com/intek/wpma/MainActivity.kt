package com.intek.wpma

import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import com.intek.wpma.NewStruct.ABaseMode
import com.intek.wpma.SQL.SQL1S
import java.math.BigInteger
import java.sql.Connection


class MainActivity :  BarcodeDataReceiver() {

    var conn: Connection? = null

    //val SS: SQL1S = SQL1S()
    val MM: ABaseMode = ABaseMode()
    var Barcode: String = ""


    private var textView: TextView? = null

    val barcodeDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("IntentApiSample: ", "onReceive")
            if (ACTION_BARCODE_DATA == intent.action) {
                val version = intent.getIntExtra("version", 0)
                if (version >= 1) {
                    Barcode = intent.getStringExtra("data")
                    reactionBarcode(Barcode)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sdkVersion = Build.VERSION.SDK_INT
    }

    override fun onResume() {
        super.onResume()
        //        IntentFilter intentFilter = new IntentFilter("hsm.RECVRBI");
        registerReceiver(barcodeDataReceiver, IntentFilter(ACTION_BARCODE_DATA))
        claimScanner()
        Log.d("IntentApiSample: ", "onResume")
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(barcodeDataReceiver)
        releaseScanner()
        Log.d("IntentApiSample: ", "onPause")
    }

    private fun setText(text: String) {
        if (textView != null) {
            runOnUiThread { textView!!.text = text }
        }
    }

    private fun reactionBarcode(Barcode: String) {
        //расшифруем IDD
        //ИДД = "99990" + СокрЛП(Сред(ШК,3,2)) + "00" + Сред(ШК,5,8);
        //99990010010982023
        val EmployerIDD: String = "99990" + Barcode.substring(2, 4) + "00" + Barcode.substring(4, 12)
        var EmployerID: String
        var Employer: String
        var EmployerFlags: String

        val TextQuery: String =
            "Select top 1 descr, SC838.SP4209, ID From SC838 (nolock) WHERE SP1933 = '" + EmployerIDD + "'"
        val DataTable = SS.ExecuteWithRead(TextQuery)
        if (DataTable== null) {
            actionLbl.text = SS.ExcStr
            return
        }
        else if (DataTable.isEmpty()){
            actionLbl.text = "Нет действий с ШК в данном режиме!"
            return
        }

        Employer = DataTable!!?.get(1)[0].trim()
        EmployerFlags = DataTable[1][1]
        EmployerID = DataTable[1][2]
        //инициализация входа
//        if(!MM.Login(EmployerID)) {
//            actionLbl.text = "Ошибка входа в систему!"
//            return
//        }
        //конвертнем настройки сотрудника в 2ую сс
        var bigInteger: BigInteger = EmployerFlags.toBigInteger()
        EmployerFlags = bigInteger.toString(2)

        if (Employer != "") {
            actionLbl.text = Employer
            val Menu = Intent(this, Menu::class.java)
            Menu.putExtra("Employer", Employer)
            Menu.putExtra("EmployerIDD",EmployerIDD)
            Menu.putExtra("EmployerFlags",EmployerFlags)
            Menu.putExtra("EmployerID",EmployerID)
            Menu.putExtra("ParentForm","MainActivity")
            startActivity(Menu)
        } else
            actionLbl.text = "Нет действий с этим ШК в данном режиме"
    }
}
