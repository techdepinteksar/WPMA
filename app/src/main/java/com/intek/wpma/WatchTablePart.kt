package com.intek.wpma

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.TableRow
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_watch_table_part.*
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import com.intek.wpma.ChoiseWork.Set.SetInitialization
import kotlinx.android.synthetic.main.activity_menu.*
import kotlinx.android.synthetic.main.activity_watch_table_part.PreviousAction
import kotlinx.android.synthetic.main.activity_watch_table_part.terminalView
import java.math.BigDecimal


class WatchTablePart : BarcodeDataReceiver() {

    var iddoc: String = ""
    var addressID: String = ""
    var InvCode: String = ""
    var Employer: String = ""
    var EmployerFlags: String = ""
    var EmployerIDD: String = ""
    var EmployerID: String = ""
    var Barcode: String = ""
    //при принятии маркировок, чтобы не сбились уже отсканированные QR-коды
    var CountFact: Int = 0
    var PrinterPath = ""
    var codeId:String = ""  //показатель по которому можно различать типы штрих-кодов
    var isMobile = false    //флаг мобильного устройства

    val barcodeDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("IntentApiSample: ", "onReceive")
            if (ACTION_BARCODE_DATA == intent.action) {
                val version = intent.getIntExtra("version", 0)
                if (version >= 1) {
                    // ту прописываем что делать при событии сканирования

                    Barcode = intent.getStringExtra("data")
                    codeId = intent.getStringExtra("codeId")
                    reactionBarcode(Barcode)

                }
            }
        }
    }

    private fun reactionBarcode(Barcode: String) {
        val toast = Toast.makeText(applicationContext, "ШК не работают на данном экране!", Toast.LENGTH_SHORT)
        toast.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watch_table_part)

        Employer = intent.extras!!.getString("Employer")!!
        EmployerFlags = intent.extras!!.getString("EmployerFlags")!!
        EmployerIDD = intent.extras!!.getString("EmployerIDD")!!
        EmployerID = intent.extras!!.getString("EmployerID")!!
        iddoc = intent.extras!!.getString("iddoc")!!
        addressID = intent.extras!!.getString("addressID")!!
        InvCode = intent.extras!!.getString("ItemCode")!!
        PreviousAction.text = intent.extras!!.getString("DocView")!!
        terminalView.text = intent.extras!!.getString("terminalView")!!
        CountFact = intent.extras!!.getString("CountFact")!!.toInt()
        PrinterPath = intent.extras!!.getString("PrinterPath")!!
        isMobile = intent.extras!!.getString("isMobile")!!.toBoolean()
        title = Employer

        //строка с шапкой
        val rowTitle = TableRow(this)
        val linearLayout = LinearLayout(this)

        //добавим столбцы
        val number = TextView(this)
        number.text = "№"
        number.typeface = Typeface.SERIF
        number.layoutParams = LinearLayout.LayoutParams(45,ViewGroup.LayoutParams.WRAP_CONTENT)
        number.gravity = Gravity.CENTER
        number.textSize = 22F
        number.setTextColor(-0x1000000)
        val address = TextView(this)
        address.text = "Адрес"
        address.typeface = Typeface.SERIF
        address.layoutParams = LinearLayout.LayoutParams(135,ViewGroup.LayoutParams.WRAP_CONTENT)
        address.textSize = 22F
        address.setTextColor(-0x1000000)
        val code = TextView(this)
        code.text = "Инв.код"
        code.typeface = Typeface.SERIF
        code.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT)
        code.gravity = Gravity.CENTER
        code.textSize = 22F
        code.setTextColor(-0x1000000)
        val count = TextView(this)
        count.text = "Кол."
        count.typeface = Typeface.SERIF
        count.layoutParams = LinearLayout.LayoutParams(75,ViewGroup.LayoutParams.WRAP_CONTENT)
        count.gravity = Gravity.CENTER
        count.textSize = 22F
        count.setTextColor(-0x1000000)
        val sum = TextView(this)
        sum.text = "Сумма"
        sum.typeface = Typeface.SERIF
        sum.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT)
        sum.gravity = Gravity.CENTER
        sum.textSize = 22F
        sum.setTextColor(-0x1000000)

        linearLayout.addView(number)
        linearLayout.addView(address)
        linearLayout.addView(code)
        linearLayout.addView(count)
        linearLayout.addView(sum)

        rowTitle.addView(linearLayout)
        table.addView(rowTitle)
        getTablePart(iddoc)

        //scroll.setOnTouchListener(@this)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        if (keyCode == 21){ //нажали влево; вернемся к документу
            val SetInitialization = Intent(this, SetInitialization::class.java)
            SetInitialization.putExtra("Employer", Employer)
            SetInitialization.putExtra("EmployerIDD",EmployerIDD)
            SetInitialization.putExtra("EmployerFlags",EmployerFlags)
            SetInitialization.putExtra("EmployerID",EmployerID)
            SetInitialization.putExtra("DocSetID",iddoc)
            SetInitialization.putExtra("AddressID",addressID)
            SetInitialization.putExtra("PreviousAction",PreviousAction.text.toString())
            SetInitialization.putExtra("PrinterPath",PrinterPath)
            SetInitialization.putExtra("terminalView",terminalView.text)
            SetInitialization.putExtra("CountFact",CountFact.toString())
            SetInitialization.putExtra("isMobile",isMobile.toString())
            SetInitialization.putExtra("ParentForm","WatchTablePart")
            startActivity(SetInitialization)
            finish()
        }
//        else if (keyCode == 20){    //вниз
//
//        }
        return super.onKeyDown(keyCode, event)
    }


    private fun getTablePart(iddoc: String): Boolean{
        var TextQuery =
            "select " +
                    "DocCC.lineno_ as Number, " +
                    "Sections.descr as Adress, " +
                    "Goods.SP1036 as InvCode, " +
                    "DocCC.SP3110 as Count, " +
                    "DocCC.SP3114 as Sum, " +
                    "DocCCHead.SP3114 as totalSum " +
            "from " +
                    "DT2776 as DocCC (nolock) " +
                    "LEFT JOIN DH2776 as DocCCHead (nolock) ON DocCCHead.iddoc = DocCC.iddoc " +
                    "LEFT JOIN SC33 as Goods (nolock) ON Goods.id = DocCC.SP3109 " +
                    "LEFT JOIN SC1141 as Sections (nolock) ON Sections.id = DocCC.SP5508 " +
            "where " +
                    "DocCC.iddoc = :iddoc " +
                    "and DocCC.SP5986 = :EmptyDate " +
                    "and DocCC.SP3116 = 0 " +
                    "and DocCC.SP3110 > 0 "+
            "order by " +
                    "DocCCHead.SP2764 , Sections.SP5103 , Number"
        TextQuery = SS.QuerySetParam(TextQuery, "EmptyDate", SS.GetVoidDate())
        TextQuery = SS.QuerySetParam(TextQuery, "iddoc", iddoc)
        TextQuery = SS.QuerySetParam(TextQuery, "addressID", addressID)
        val DataTable = SS.ExecuteWithRead(TextQuery) ?: return false

        if(DataTable.isNotEmpty()){
            for (i in 1 until DataTable.size){
                val row = TableRow(this)
                val number = TextView(this)
                val linearLayout = LinearLayout(this)
                number.text = DataTable!![i][0]
                number.layoutParams = LinearLayout.LayoutParams(45,ViewGroup.LayoutParams.WRAP_CONTENT)
                number.gravity = Gravity.CENTER
                number.textSize = 16F
                number.setTextColor(-0x1000000)
                val address = TextView(this)
                address.text = DataTable!![i][1]
                address.layoutParams = LinearLayout.LayoutParams(135,ViewGroup.LayoutParams.WRAP_CONTENT)
                address.textSize = 16F
                address.setTextColor(-0x1000000)
                val code = TextView(this)
                code.text = DataTable!![i][2]
                code.layoutParams = LinearLayout.LayoutParams(135,ViewGroup.LayoutParams.WRAP_CONTENT)
                code.gravity = Gravity.CENTER
                code.textSize = 16F
                code.setTextColor(-0x1000000)
                val count = TextView(this)
                count.text = DataTable!![i][3]
                count.layoutParams = LinearLayout.LayoutParams(40,ViewGroup.LayoutParams.WRAP_CONTENT)
                count.gravity = Gravity.CENTER
                count.textSize = 16F
                count.setTextColor(-0x1000000)
                val sum = TextView(this)
                sum.text = DataTable!![i][4]
                sum.layoutParams = LinearLayout.LayoutParams(120,ViewGroup.LayoutParams.WRAP_CONTENT)
                sum.gravity = Gravity.CENTER
                sum.textSize = 16F
                sum.setTextColor(-0x1000000)


                linearLayout.addView(number)
                linearLayout.addView(address)
                linearLayout.addView(code)
                linearLayout.addView(count)
                linearLayout.addView(sum)

                row.addView(linearLayout)
                if (DataTable!![i][2] == InvCode){
                    row.setBackgroundColor(Color.YELLOW)
                }
                table.addView(row)
            }
            sum.text = DataTable[1][5]
        }
        return true
    }

    override fun onResume() {
        super.onResume()
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
