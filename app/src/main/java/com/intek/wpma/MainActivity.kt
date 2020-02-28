package com.intek.wpma

import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import com.intek.wpma.Model.Model
import com.intek.wpma.SQL.SQL1S
import kotlinx.android.synthetic.main.activity_menu.*
import java.math.BigInteger
import java.sql.Connection
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.android.synthetic.main.activity_main.terminalView

class MainActivity :  BarcodeDataReceiver() {

    var Barcode: String = ""
    var codeId:String = ""  //показатель по которому можно различать типы штрих-кодов


    private var textView: TextView? = null

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
                        val toast = Toast.makeText(
                            applicationContext,
                            "Отсутствует соединение с базой!",
                            Toast.LENGTH_LONG
                        )
                        toast.show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sdkVersion = Build.VERSION.SDK_INT
        ANDROID_ID =  Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        tsdNumVers.text = tsdVers
        // получим номер терминала
        val TextQuery =
            "SELECT " +
                    "SC5096.code " +
                    "FROM " +
                    "SC5096 " +
                    "WHERE " +
                    "descr = '${ANDROID_ID}'"
        var DataTable: Array<Array<String>>? = SS.ExecuteWithRead(TextQuery)
        if (DataTable!!.isEmpty()){
            val toast = Toast.makeText(applicationContext, "Терминал не опознан!", Toast.LENGTH_SHORT)
            toast.show()
            return
        }
        terminal = DataTable!![1][0]
        terminalView.text = terminal
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
        if(!Login(EmployerID)) {
            actionLbl.text = "Ошибка входа в систему!"
            return
        }
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
            Menu.putExtra("terminalView",terminal)
            Menu.putExtra("ParentForm","MainActivity")
            startActivity(Menu)
        } else
            actionLbl.text = "Нет действий с этим ШК в данном режиме"
    }
    fun Login(EmployerID: String): Boolean {
//        if (!SS.UpdateProgram())
//        {
//            return false
//        }
//        if (!SS.SynhDateTime())
//        {
//            return false
//        }
        if (!IBS_Inicialization(EmployerID)) {
            return false
        }

        var DataMapWrite: MutableMap<String, Any> = mutableMapOf()
        DataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] = SS.ExtendID(EmployerID, "Спр.Сотрудники")
        DataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = ANDROID_ID
        if (!ExecCommandNoFeedback("Login", DataMapWrite)) {
            return false
        }
        return true
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
}
