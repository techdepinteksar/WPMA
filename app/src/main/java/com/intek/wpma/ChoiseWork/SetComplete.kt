package com.intek.wpma.ChoiseWork

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import android.util.Printer
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.ChoiseWork.Set.SetInitialization
import com.intek.wpma.MainActivity
import com.intek.wpma.R
import com.intek.wpma.SQL.SQL1S
import com.intek.wpma.ScanActivity
import kotlinx.android.synthetic.main.activity_correct.*
import kotlinx.android.synthetic.main.activity_set.*
import kotlinx.android.synthetic.main.activity_set.PreviousAction
import kotlinx.android.synthetic.main.activity_set_complete.*
import kotlinx.android.synthetic.main.activity_set_complete.FExcStr
import kotlinx.android.synthetic.main.activity_set_complete.terminalView

class SetComplete : BarcodeDataReceiver() {

    val SetInit: SetInitialization = SetInitialization()
    var DocSet: String = ""
    var Employer: String = ""
    var EmployerFlags: String = ""
    var EmployerIDD: String = ""
    var EmployerID: String = ""
    var Barcode: String = ""
    var codeId: String = ""             //показатель по которому можно различать типы штрих-кодов
    var Places: Int? = null
    var PrinterPath = ""
    var isMobile = false    //флаг мобильного устройства

    val barcodeDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("IntentApiSample: ", "onReceive")
            if (ACTION_BARCODE_DATA == intent.action) {
                val version = intent.getIntExtra("version", 0)
                if (version >= 1) {
                    // ту прописываем что делать при событии сканирования
                    try {
                        Barcode = intent.getStringExtra("data")
                        reactionBarcode(Barcode)
                    }
                    catch(e: Exception) {
                        val toast = Toast.makeText(applicationContext, "Не удалось отсканировать штрихкод!", Toast.LENGTH_LONG)
                        toast.show()
                    }

                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_complete)

        Employer = intent.extras!!.getString("Employer")!!
        EmployerFlags = intent.extras!!.getString("EmployerFlags")!!
        EmployerIDD = intent.extras!!.getString("EmployerIDD")!!
        EmployerID = intent.extras!!.getString("EmployerID")!!
        PrinterPath = intent.extras!!.getString("PrinterPath")!!
        terminalView.text = intent.extras!!.getString("terminalView")!!
        isMobile = intent.extras!!.getString("isMobile")!!.toBoolean()
        ANDROID_ID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        title = Employer

        if (PrinterPath != "") {
            printer.text = PrinterPath
            FExcStr.text = "Введите колво мест"
            enterCountPlace.visibility = View.VISIBLE
        }
        DocSet = intent.extras!!.getString("iddoc")!!
        val TextQuery =
            "SELECT " +
                    "journForBill.docno as DocNo, " +
                    "CONVERT(char(8), CAST(LEFT(journForBill.date_time_iddoc, 8) as datetime), 4) as DateDoc, " +
                    "journForBill.iddoc as Bill, " +
                    "Sector.descr as Sector, " +
                    "DocCCHead.SP3595 as Number, " +
                    "DocCCHead.SP2841 as SelfRemovel " +
                    "FROM " +
                    "DH2776 as DocCCHead (nolock) " +
                    "LEFT JOIN SC1141 as Sector (nolock) " +
                    "ON Sector.id = DocCCHead.SP2764 " +
                    "LEFT JOIN DH2763 as DocCB (nolock) " +
                    "ON DocCB.iddoc = DocCCHead.SP2771 " +
                    "LEFT JOIN DH196 as Bill (nolock) " +
                    "ON Bill.iddoc = DocCB.SP2759 " +
                    "LEFT JOIN _1sjourn as journForBill (nolock) " +
                    "ON journForBill.iddoc = Bill.iddoc " +
                    "WHERE DocCCHead.iddoc = '$DocSet'"
        val DataTable = SS.ExecuteWithRead(TextQuery)
        previousAction.text = if (DataTable!![1][5].toInt() == 1) "(C) " else {
            ""
        } + DataTable[1][3].trim() + "-" +
                DataTable[1][4] + " Заявка " + DataTable[1][0] + " (" + DataTable[1][1] + ")"

        if (DataTable[1][5].toInt() == 1) DocView.text = "САМОВЫВОЗ" else DocView.text = "ДОСТАВКА"
        //тут этот код дублирую, чтобы поймать нажатие на enter после ввода колва с уже установленным принтером
        enterCountPlace.setOnKeyListener { v: View, keyCode: Int, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                // сохраняем текст, введенный до нажатия Enter в переменную
                try {
                    val count = enterCountPlace.text.toString().toInt()
                    Places = count
                    enterCountPlace.visibility = View.INVISIBLE
                    countPlace.text = "Колво мест: $Places"
                    countPlace.visibility = View.VISIBLE
                    FExcStr.text = "Ожидание команды"
                } catch (e: Exception) {

                }
            }
            false
        }

        if (isMobile){
            btnScanSetComplete.visibility = View.VISIBLE
            btnScanSetComplete!!.setOnClickListener {
                val ScanAct = Intent(this@SetComplete, ScanActivity::class.java)
                ScanAct.putExtra("ParentForm","SetComplete")
                startActivity(ScanAct)
            }
        }
    }

    companion object {
        var scanRes: String? = null
        var scanCodeId: String? = null
    }

    private fun reactionBarcode(Barcode: String): Boolean {
        val IDD: String = "99990" + Barcode.substring(2, 4) + "00" + Barcode.substring(4, 12)


        if (SS.IsSC(IDD, "Принтеры")) {
            //получим путь принтера
            val TextQuery =
                "select descr, SP2461 " +
                        "from SC2459 " +
                        "where SP2465 = '$IDD'"
            val DataTable = SS.ExecuteWithRead(TextQuery) ?: return false

            PrinterPath = DataTable!![1][1]
            printer.text = PrinterPath
            FExcStr.text = "Введите колво мест"
            enterCountPlace.visibility = View.VISIBLE

            return true
        } else if (!SS.IsSC(IDD, "Секции")) {
            FExcStr.text = "Нужен принтер и адрес предкомплектации, а не это!"
            return false
        }
        if (PrinterPath.isEmpty()) {
            FExcStr.text = "Не выбран принтер!"
            return false
        }
        if (Places == null){
            FExcStr.text = "Количество мест не указано!"
            return false
        }
        //подтянем адрес комплектации
        val TextQuery =
            "SELECT ID, SP3964, descr FROM SC1141 (nolock) WHERE SP1935= '$IDD'"
        val DataTable = SS.ExecuteWithRead(TextQuery) ?: return false
        val AddressType = DataTable!![1][1]
        val AddressID = DataTable!![1][0]
        if (AddressType == "12") {
            FExcStr.text = "Отсканируйте адрес предкопмплектации!"
            return false
        }
        var DataMapWrite: MutableMap<String, Any> = mutableMapOf()
        DataMapWrite["Спр.СинхронизацияДанных.ДокументВход"] = SS.ExtendID(DocSet, "КонтрольНабора")
        DataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] = SS.ExtendID(EmployerID, "Спр.Сотрудники")
        DataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход2"] = SS.ExtendID(AddressID, "Спр.Секции")
        DataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = Places!!
        DataMapWrite["Спр.СинхронизацияДанных.ДатаВход2"] = PrinterPath

        var DataMapRead: MutableMap<String, Any> = mutableMapOf()
        var FieldList: MutableList<String> = mutableListOf("Спр.СинхронизацияДанных.ДатаРез1")

        DataMapRead = ExecCommand("PicingComplete", DataMapWrite, FieldList, DataMapRead, "")

        if ((DataMapRead["Спр.СинхронизацияДанных.ФлагРезультата"] as String).toInt() == -3) {
            FExcStr.text = DataMapRead["Спр.СинхронизацияДанных.ДатаРез1"].toString()
            //сборочный уже закрыт, уйдем с формы завершения набора
            val SetInitialization = Intent(this, SetInitialization::class.java)
            SetInitialization.putExtra("Employer", Employer)
            SetInitialization.putExtra("EmployerIDD", EmployerIDD)
            SetInitialization.putExtra("EmployerFlags", EmployerFlags)
            SetInitialization.putExtra("EmployerID", EmployerID)
            SetInitialization.putExtra("PrinterPath", PrinterPath)
            SetInitialization.putExtra("terminalView",terminalView.text.trim())
            SetInitialization.putExtra("isMobile",isMobile.toString())
            SetInitialization.putExtra("ParentForm", "SetComplete")
            startActivity(SetInitialization)
            finish()
            return false
        }
        if ((DataMapRead["Спр.СинхронизацияДанных.ФлагРезультата"] as String).toInt() != 3) {
            FExcStr.text = "Не известный ответ робота... я озадачен..."
            return false
        }
        FExcStr.text = DataMapRead["Спр.СинхронизацияДанных.ДатаРез1"].toString()

        LockoutDoc(DocSet)      //разблокируем доки

        //вернемся обратно в SetInitialization
        val SetInitialization = Intent(this, SetInitialization::class.java)
        SetInitialization.putExtra("Employer", Employer)
        SetInitialization.putExtra("EmployerIDD", EmployerIDD)
        SetInitialization.putExtra("EmployerFlags", EmployerFlags)
        SetInitialization.putExtra("EmployerID", EmployerID)
        SetInitialization.putExtra("PrinterPath", PrinterPath)
        SetInitialization.putExtra("terminalView",terminalView.text.trim())
        SetInitialization.putExtra("isMobile",isMobile.toString())
        SetInitialization.putExtra("ParentForm", "SetComplete")
        startActivity(SetInitialization)
        finish()


        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        ReactionKey(keyCode, event)
        return super.onKeyDown(keyCode, event)
    }

    private fun ReactionKey(keyCode: Int, event: KeyEvent?) {

        // нажали назад, выйдем и разблокируем доки
        if (keyCode == 4){

        }

        enterCountPlace.setOnKeyListener { v: View, keyCode: Int, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                // сохраняем текст, введенный до нажатия Enter в переменную
                try {
                    val count = enterCountPlace.text.toString().toInt()
                    Places = count
                    enterCountPlace.visibility = View.INVISIBLE
                    countPlace.text = "Колво мест: $Places"
                    countPlace.visibility = View.VISIBLE
                    FExcStr.text = "Ожидание команды"
                } catch (e: Exception) {

                }
            }
            false
        }

    }

    override fun onResume() {
        super.onResume()
        registerReceiver(barcodeDataReceiver, IntentFilter(ACTION_BARCODE_DATA))
        claimScanner()
        Log.d("IntentApiSample: ", "onResume")
        if(scanRes != null){
            try {
                Barcode = scanRes.toString()
                codeId = scanCodeId.toString()
                reactionBarcode(Barcode)
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
