package com.intek.wpma.ChoiseWork.Set


import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.util.Printer
import android.view.KeyEvent
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.isVisible
import com.intek.wpma.*
import com.intek.wpma.ChoiseWork.SetComplete
import com.intek.wpma.SQL.SQL1S
import com.intek.wpma.Model.Model
import kotlinx.android.synthetic.main.activity_set.*
import com.intek.wpma.Helpers.Helper
import java.math.BigDecimal
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*


class SetInitialization : BarcodeDataReceiver() {


    val helper = Helper()
    val primordial = Model()

    var ParentForm: String = "" // форма из которой пришли

    var AllSetsRow: Int = 0
    var DocSetSum: BigDecimal = "0.00".toBigDecimal()
    val MainWarehouse = "     D   "
    val ResponceTime: Int = 60 //время ожидания отклика от 1С
    var Employer: String = ""
    var EmployerFlags: String = ""
    var EmployerIDD: String = ""
    var EmployerID: String = ""
    var DocSet: Model.StrictDoc? = null
    var Section: Model.Section? = null
    var CCItem: Model.StructItemSet? = null
    var Barcode: String = ""
    var DocsSet: MutableList<String> = mutableListOf()
    var CurrentAction: Global.ActionSet? = null
    var FCurrentMode: Global.Mode? = null
    // количество принимаемой позиции
    var CountFact: Int = 0
    var CurrLine: Int = 0
    var PrinterPath = ""    //сюда будем запоминать принтер после завершения набора, чтобы постоянно не сканировать

    val barcodeDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("IntentApiSample: ", "onReceive")
            if (ACTION_BARCODE_DATA == intent.action) {
                val version = intent.getIntExtra("version", 0)
                if (version >= 1) {
                    // ту прописываем что делать при событии сканирования

                    Barcode = intent.getStringExtra("data")
                    reactionBarcode(Barcode)

                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.intek.wpma.R.layout.activity_set)
        Employer = intent.extras!!.getString("Employer")!!
        EmployerFlags = intent.extras!!.getString("EmployerFlags")!!
        EmployerIDD = intent.extras!!.getString("EmployerIDD")!!
        EmployerID = intent.extras!!.getString("EmployerID")!!
        ParentForm = intent.extras!!.getString("ParentForm")!!

        if (ParentForm == "Menu"){
            ToModeSetInicialization()
        }
        else if (ParentForm == "Correct" || ParentForm == "WatchTablePart"){
            PreviousAction.text = intent.extras!!.getString("PreviousAction")!!
            PrinterPath = intent.extras!!.getString("PrinterPath")!!
            //получим незаконченные задания по отбору
            GetDocsSet()
            //сообразим с какими параметрами нужно вызвать ToModeSet
            val DocSetID = intent.extras!!.getString("DocSetID")!!
            val AddressID = intent.extras!!.getString("AddressID")!!
            if ((DocSetID != "") && (AddressID == "")){
                ToModeSet(null, DocSetID)
            }
            else if ((DocSetID != "") && (AddressID != "")) {
                ToModeSet(AddressID, DocSetID)
            }
            else ToModeSet(null, null)
        }
        else if (ParentForm == "SetComplete"){
            PrinterPath = intent.extras!!.getString("PrinterPath")!!
            GetDocsSet()
            QuitModesSet()      //разблокируем доки
            ToModeSetInicialization()
        }
    }

    fun ToModeSetInicialization(): Boolean {
        //FEmployer.Refresh();    //Обновим данные сотрудника
        //Const.Refresh();        //Обновим константы

        //PreviousAction = "";

        //получим незаконченные задания по отбору
        GetDocsSet()
        if(DocsSet.isNotEmpty()){
            return ToModeSet(null, null)
        }
        FCurrentMode = Global.Mode.SetInicialization
        return true

    } // ToModeSetInicialization

    fun GetDocsSet(){
        var TextQuery =
            "SELECT " +
                    "journ.iddoc as IDDOC " +
                    "FROM " +
                    "_1sjourn as journ (nolock) " +
                    "INNER JOIN DH2776 as DocCC (nolock) " +
                    "ON DocCC.iddoc = journ.iddoc " +
                    "WHERE " +
                    "DocCC.SP2773 = '" + EmployerID + "'" +
                    "and journ.iddocdef = 2776 " +
                    "and DocCC.SP2767 = :EmptyDate " +
                    "and not DocCC.SP2765 = :EmptyDate " +
                    "and journ.ismark = 0 "

        TextQuery = SS.QuerySetParam(TextQuery, "EmptyDate", SS.GetVoidDate())
        val DataTable = SS.ExecuteWithRead(TextQuery)

        if (DataTable!!.isNotEmpty()) {
            //если есть незаконченные задания по отбору

            for (i in 1 until DataTable.size){
                DocsSet.add(DataTable!![i][0])
            }
        }
    }


    fun ToModeSet(AdressID: String?, iddoc: String?): Boolean {
        for (id in DocsSet) {
            if (!LockDoc(id)) {
                return false
            }
        }
        var TextQuery =
            "DECLARE @curdate DateTime; " +
                    "SELECT @curdate = DATEADD(DAY, 1 - DAY(curdate), curdate) FROM _1ssystem (nolock); " +
                    "select top 1 " +
                    "DocCC.SP3109 as ID, " +
                    "DocCC.lineno_ as LINENO_, " +
                    "Goods.descr as ItemName, " +
                    "Goods.SP1036 as InvCode, " +
                    "Goods.SP5086 as Details, " +
                    "DocCC.SP3110 as Count, " +
                    "DocCC.SP5508 as Adress, " +
                    "DocCC.SP3112 as Price, " +
                    "Sections.descr as AdressName, " +
                    "ISNULL(AOT.Balance, 0) as Balance, " +
                    //Реквизиты документа
                    "DocCC.iddoc as IDDOC, " +
                    "journForBill.docno as DocNo, " +
                    "CONVERT(char(8), CAST(LEFT(journForBill.date_time_iddoc, 8) as datetime), 4) as DateDoc, " +
                    "journForBill.iddoc as Bill, " +
                    "DocCCHead.SP2814 as Rows, " +
                    "Sector.descr as Sector, " +
                    "DocCCHead.SP3114 as Sum, " +
                    "DocCCHead.SP3595 as Number, " +
                    "DocCCHead.SP2841 as SelfRemovel, " +
                    "Clients.descr as Client, " +
                    "Bill.SP3094 as TypeNakl, " +
                    "isnull(DocCCHead.SP6525 , :EmptyID) as BoxID, " +
                    "AdressBox.descr as Box " +
                    "from " +
                    "DT2776 as DocCC (nolock) " +
                    "LEFT JOIN DH2776 as DocCCHead (nolock) " +
                    "ON DocCCHead.iddoc = DocCC.iddoc " +
                    "LEFT JOIN SC33 as Goods (nolock) " +
                    "ON Goods.id = DocCC.SP3109 " +
                    "LEFT JOIN SC1141 as Sections (nolock) " +
                    "ON Sections.id = DocCC.SP5508 " +
                    "LEFT JOIN ( " +
                    "select " +
                    "RegAOT.SP4342 as item, " +
                    "RegAOT.SP4344 as adress, " +
                    "sum(RegAOT.SP4347 ) as balance " +
                    "from " +
                    "RG4350 as RegAOT (nolock) " +
                    "where " +
                    "period = @curdate " +
                    "and SP4343 = :Warehouse " +
                    "and SP4345 = 2 " +
                    "group by RegAOT.SP4342 , RegAOT.SP4344 " +
                    ") as AOT " +
                    "ON AOT.item = DocCC.SP3109 and AOT.adress = DocCC.SP5508 " +
                    "LEFT JOIN DH2763 as DocCB (nolock) " +
                    "ON DocCB.iddoc = DocCCHead.SP2771 " +
                    "LEFT JOIN DH196 as Bill (nolock) " +
                    "ON Bill.iddoc = DocCB.SP2759 " +
                    "LEFT JOIN _1sjourn as journForBill (nolock) " +
                    "ON journForBill.iddoc = Bill.iddoc " +
                    "LEFT JOIN SC1141 as Sector (nolock) " +
                    "ON Sector.id = DocCCHead.SP2764 " +
                    "LEFT JOIN SC46 as Clients (nolock) " +
                    "ON Bill.SP199 = Clients.id " +
                    "LEFT JOIN SC1141 as AdressBox (nolock) " +
                    "ON AdressBox.id = DocCCHead.SP6525 " +
                    "where " +
                    "DocCC.iddoc in (:Docs) " +
                    "and DocCC.SP5986 = :EmptyDate " +
                    "and DocCC.SP3116 = 0 " +
                    "and DocCC.SP3110 > 0 "

        if (AdressID != null) TextQuery += "and DocCC.SP5508 = :Adress "
        if (iddoc != null) TextQuery += "and DocCC.iddoc = :iddoc "
        TextQuery +=  "order by " +
                        "DocCCHead.SP2764 , Sections.SP5103 , LINENO_"
        TextQuery = SS.QuerySetParam(TextQuery, "EmptyID", SS.GetVoidID())
        TextQuery = TextQuery.replace(":Docs", helper.ListToStringWithQuotes(DocsSet))
        TextQuery = SS.QuerySetParam(TextQuery, "Warehouse", MainWarehouse)
        TextQuery = SS.QuerySetParam(TextQuery, "EmptyDate", SS.GetVoidDate())
        if (iddoc != null) {
            TextQuery = SS.QuerySetParam(TextQuery, "iddoc", iddoc)
        }
        if (AdressID != null) {
            TextQuery = SS.QuerySetParam(TextQuery, "Adress", AdressID)
        }

        val DataTable = SS.ExecuteWithRead(TextQuery)
        //неотобранных строк больше нет
        if (DataTable!!.isEmpty()) {
            if (AdressID == null) {
                // завершение отбора
                return ToModeSetComplete()
            } else {
                FExcStr.text = "Нет такого адреса в сборочном!"
                return false
            }
            FExcStr.text = "Нет доступных команд! Ошибка робота!"
        }

        RefreshRowSum()    //Подтянем циферки

        //представление документа
        val DocView = if (DataTable[1][18].toInt() == 1) "(C) " else {
            ""
        } + DataTable[1][15].trim() + "-" +
                DataTable[1][17] + " Заявка " + DataTable[1][11] + " (" + DataTable[1][12] + ")"

        CCItem = Model.StructItemSet(
            DataTable[1][0],                            //ID
            DataTable[1][3],                            //InvCode
            DataTable[1][2].trim(),                     //Name
            DataTable[1][7].toBigDecimal(),             //Price
            DataTable[1][5].toBigDecimal().toInt(),     //Count
            DataTable[1][5].toBigDecimal().toInt(),     //CountFact
            DataTable[1][6],                            //AdressID
            DataTable[1][8].trim(),                     //AdressName
            DataTable[1][1].toInt(),                    //CurrLine
            DataTable[1][9].toBigDecimal().toInt(),     //Balance
            DataTable[1][4].toBigDecimal().toInt(),     //Details
            DataTable[1][5].toBigDecimal().toInt(),     //OKEI2Count
            "шт",                                //OKEI2
            1                                 //OKEI2Coef
        )
        CurrLine = DataTable[1][5].toBigDecimal().toInt()

        CCItem = MultiplesOKEI2(CCItem!!)

        DocSet = Model.StrictDoc(
            DataTable[1][10],                           //ID
            DataTable[1][18].toInt(),                   //SelfRemovel
            DocView,                                    //View
            DataTable[1][14].toInt(),                   //Rows
            DataTable[1][13],                           //FromWarehouseID
            DataTable[1][19].trim(),                    //Client
            DataTable[1][16].toBigDecimal(),            //Sum
            DataTable[1][20].toInt() == 2,      //Special
            DataTable[1][22],                           //Box
            DataTable[1][21]                            //BoxID
        )

        CurrentAction = Global.ActionSet.ScanAdress
        FExcStr.text = WhatUNeed()

        // заполним форму
        val price: TextView = findViewById(R.id.price)
        price.text = "Цена: " + DataTable!![1][7]
        price.visibility = VISIBLE
        val balance: TextView = findViewById(R.id.balance)
        balance.text = "Ост-ок: " + CCItem!!.Balance
        balance.visibility = VISIBLE
        val address: TextView = findViewById(R.id.address)
        address.text = CCItem!!.AdressName
        address.visibility = VISIBLE
        val invCode: TextView = findViewById(R.id.InvCode)
        invCode.text = CCItem!!.InvCode
        invCode.visibility = VISIBLE
        val header: TextView = findViewById(R.id.header)
        header.text =
            "Строка " + CCItem!!.CurrLine + " из " + DocSet!!.Rows + " (ост " + AllSetsRow + ")"
        header.visibility = VISIBLE
        val item: TextView = findViewById(R.id.item)
        item.text = CCItem!!.Name
        item.visibility = VISIBLE
        val details: TextView = findViewById(R.id.details)
        details.text = "Деталей: " + CCItem!!.Details
        details.visibility = VISIBLE

        val count: TextView = findViewById(R.id.count)
        count.text = CCItem!!.Count.toString() + " шт по 1"
        count.visibility = VISIBLE

        val correct: TextView = findViewById(R.id.correct)
        correct.visibility = VISIBLE
        mainView.text = DocSet!!.View

        FCurrentMode = Global.Mode.Set
        return true
    }

    private fun RefreshRowSum(): Boolean {
        var TextQuery =
            "select " +
                    "sum(DocCC.SP3114) as Sum, " +
                    "count(*) Amount " +
                    "from " +
                    "DT2776 as DocCC (nolock) " +
                    "where " +
                    "DocCC.iddoc in (:Docs ) " +
                    "and DocCC.SP5986 = :EmptyDate " +
                    "and DocCC.SP3116 = 0 " +
                    "and DocCC.SP3110 > 0 "
        TextQuery = TextQuery.replace(":Docs", helper.ListToStringWithQuotes(DocsSet))
        TextQuery = SS.QuerySetParam(TextQuery, "EmptyDate", SS.GetVoidDate())
        val DataTable = SS.ExecuteWithRead(TextQuery) ?: return false
        if (DataTable!!.isNotEmpty()) {
            DocSetSum = DataTable[1][0].toBigDecimal()
            AllSetsRow = DataTable[1][1].toInt()
        } else {
            DocSetSum = "0.00".toBigDecimal()
            AllSetsRow = 0
        }
        return true
    }

    private fun MultiplesOKEI2(CCItem: Model.StructItemSet): Model.StructItemSet {

        var Item: Model.StructItemSet = CCItem
        var TextQuery = "SELECT " +
                "isnull(OKEI2.descr, OKEI.descr) as Name, " +
                "CAST(Units.SP2232 as int) as Coef, " +
                "ceiling(2/SP2232 ) as AmountOKEI2 " +
                "FROM " +
                "SC2237 as Units (nolock) " +
                "inner join SC2229 as OKEI (nolock)" +
                "on OKEI.id = Units.SP2230 " +
                "left join SC2229  as OKEI2 (nolock) " +
                "on OKEI2.id = Units.SP6584 " +
                "WHERE " +
                "Units.parentext = :CurrentItem " +
                "and OKEI.id <> :OKEIKit" +
                "and Units.ismark = 0 " +
                "and ceiling(2/SP2232 )*SP2232 = :amount " +
                "order by AmountOKEI2"
        TextQuery = SS.QuerySetParam(TextQuery, "CurrentItem", CCItem.ID)
        TextQuery = SS.QuerySetParam(TextQuery, "amount", CCItem.Count)
        TextQuery = SS.QuerySetParam(TextQuery, "OKEIKit", primordial.OKEIKit)

        val DataTable = SS.ExecuteWithRead(TextQuery)
        if (DataTable!!.isNotEmpty()) {
            Item = Model.StructItemSet(
                CCItem.ID,
                CCItem.InvCode,
                CCItem.Name,
                CCItem.Price,
                CCItem.Count,
                CCItem.CountFact,
                CCItem.AdressID,
                CCItem.AdressName,
                CCItem.CurrLine,
                CCItem.Balance,
                CCItem.Details,
                DataTable[1][2].toBigDecimal().toInt(),
                DataTable[1][0].trim(),
                DataTable[1][1].toInt()
            )
        }
        return Item
    }

    fun WhatUNeed(currAction: Global.ActionSet?): String {
        var result: String = ""
        when (currAction) {
            Global.ActionSet.ScanAdress -> result = "Отсканируйте адрес!"

            Global.ActionSet.ScanItem -> result = "Отсканируйте товар!"

            Global.ActionSet.EnterCount -> result = "Введите количество! (подтверждать - 'enter')"

            Global.ActionSet.ScanPart -> result =
                "Отсканируйте спец. ШК деталей! " + CCItem!!.InvCode.trim() +
                        (if (CCItem!!.Details == 99) " (особая)" else " (деталей: " + CCItem!!.Details.toString() + ")")

            Global.ActionSet.ScanBox -> result = "Отсканируйте коробку!"

            Global.ActionSet.ScanPallete -> result = "Отсканируйте паллету!"
        }
        return result
    }

    private fun WhatUNeed(): String {
        return WhatUNeed(CurrentAction)
    }  // WhatUNeed

    fun LockDoc(IDDoc: String): Boolean {
        return IBS_Lock("int_doc_$IDDoc")
    }

    fun IBS_Lock(BlockText: String): Boolean {
        var TextQuery =
            "set nocount on; " +
                    "declare @id bigint; " +
                    "exec IBS_Inicialize_with_DeviceID_new :Employer, :HostName, :DeviceID, @id output; " +
                    "select @id as ID;" +
            "set nocount on; " +
                    "declare @result int; " +
                    "exec IBS_Lock :BlockText, @result output; " +
                    "select @result as result;"
        TextQuery = SS.QuerySetParam(TextQuery, "Employer", EmployerID)
        TextQuery = SS.QuerySetParam(TextQuery, "HostName", "Android")
        //пока присвою жесткий id                                       DeviceID.GetDeviceID()
        TextQuery = SS.QuerySetParam(TextQuery, "DeviceID", "Android_ID")
        TextQuery = SS.QuerySetParam(TextQuery, "BlockText", BlockText)
        var DataTable: Array<Array<String>>? = SS.ExecuteWithRead(TextQuery) ?: return false
        if (DataTable!![1][0].toInt() > 0) {
            return true
        } else {
            FExcStr.text = "Объект заблокирован!" //Ответ по умолчанию
            //Покажем кто заблокировал
            TextQuery =
                "SELECT " +
                        "rtrim(Collation.HostName) as HostName, " +
                        "rtrim(Collation.UserName) as UserName, " +
                        "convert(char(8), Block.date_time, 4) as Date, " +
                        "substring(convert(char, Block.date_time, 21), 12, 8) as Time " +
                        "FROM " +
                        "IBS_Block as Block " +
                        "INNER JOIN IBS_Collation as Collation " +
                        "ON Collation.ID = Block.ProcessID " +
                        "WHERE " +
                        "left(Block.BlockText, len(:BlockText)) = :BlockText "
            TextQuery = SS.QuerySetParam(TextQuery, "BlockText", BlockText)
            DataTable = SS.ExecuteWithRead(TextQuery)
            if (DataTable!!.isNotEmpty()) {
                FExcStr.text =
                    "Объект заблокирован! " + DataTable!![1][1] + ", " + DataTable!![1][0] +
                            ", в " + DataTable!![1][3] + " (" + DataTable!![1][2] + ")"
            }
            return false
        }
    }

    fun QuitModesSet(): Boolean {
        for (id in DocsSet) {
            if (!LockoutDoc(id)) {
                return false
            }
        }
        return true
    } // QuitModesSet

    fun LockoutDoc(IDDoc: String): Boolean
    {
        return IBS_Lockuot("int_doc_" + IDDoc)
    }

    fun IBS_Lockuot(BlockText: String): Boolean
    {
        var TextQuery = "exec IBS_Lockout :BlockText"
        TextQuery = SS.QuerySetParam(TextQuery, "BlockText", BlockText)
        if (!SS.ExecuteWithoutRead(TextQuery))
        {
            return false
        }
        return true
    }

    private fun reactionBarcode(Barcode: String) {
        val IDD: String = "99990" + Barcode.substring(2, 4) + "00" + Barcode.substring(4, 12)
        if (SS.IsSC(IDD, "Сотрудники")) {
            //
            var intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        //переписать по аналоги со старым тсд
        // на данный момент он не различает сканирование справочника от баркода
//        if (SS.IsSC(IDD, "Секции")) {
//
//            if (FCurrentMode == Global.Mode.SetInicialization) {
//                RSCSetInicialization(IDD)
//            }
//            if (FCurrentMode == Global.Mode.Set) {
//                RSCSet(IDD)
//            }
//        }
//        when (FCurrentMode) {
//            Global.Mode.Set -> RBSet(Barcode)
//        }

        var IsObject: Boolean = true
        var dicBarcode: MutableMap<String,String> = helper.DisassembleBarcode(Barcode)
        if (Barcode.substring(0, 2) == "25" && dicBarcode["Type"] == "113") {

            if (!SS.IsSC(dicBarcode["IDD"]!!, "Сотрудники")) {
                if (!SS.IsSC(dicBarcode["IDD"]!!, "Секции")) {
                    if (!SS.IsSC(dicBarcode["IDD"]!!, "Принтеры")) {
                        IsObject = false
                    }
                }
            }
            if (IsObject)
            {
                if (ReactionSC(dicBarcode["IDD"]!!))
                {

                    if (FCurrentMode == Global.Mode.Set)
                    {
                        //GoodDone();
                        return
                    }
                }
                return
            }
        }
        if (dicBarcode["Type"] == "part" && (FCurrentMode == Global.Mode.Set))
        {
            var bigInteger: BigInteger = EmployerFlags.toBigInteger()
            EmployerFlags = bigInteger.toString(2)
            ScanPartBarcode(dicBarcode["count"]!!.substring(2,3).toInt())

            return
        }
        if (ReactionBarcode(Barcode))
        {

            if (FCurrentMode == Global.Mode.Set)
            {
                //GoodDone()
                return
            }
            else
            {
                FExcStr.text = "Ожидание команды"
            }

        }
        else
        {
            if (dicBarcode["Type"] == "6" && (FCurrentMode == Global.Mode.Set))
            {
                if (ReactionSC(dicBarcode["ID"]!!, true))
                {
                    if (FCurrentMode == Global.Mode.Set)
                    {
                        //GoodDone()
                        return
                    }
                    else
                    {
                        //View()
                    }
                }
            }
            if (SS.ExcStr == null)
            {
                FExcStr.text = "Ожидание команды"
            }
            else
            {
                if (FCurrentMode == Global.Mode.Set)
                {
                    //BadDone();
                    return
                }
                FExcStr.text = SS.ExcStr
            }
        }

    }

    fun ReactionBarcode(Barcode: String): Boolean {
        when (FCurrentMode) {
            Global.Mode.Set -> return RBSet(Barcode)
            else -> {
                FExcStr.text = "Нет действий с этим штирхкодом в данном режиме!"; return false
            }
        }
    }

    fun ReactionSC(IDD: String): Boolean
    {
        return ReactionSC(IDD, false)
    }

    fun ReactionSC(IDD: String, thisID: Boolean): Boolean
    {
        //FExcStr = null;
        return when (FCurrentMode) {

            Global.Mode.SetInicialization -> RSCSetInicialization(IDD)

            Global.Mode.Set -> RSCSet(IDD, thisID)

            //Global.Mode.SetComplete -> RSCSetComplete(IDD)


            else -> {FExcStr.text = "Нет действий с данным справочником в данном режиме!"; false }
        }
    }

    private fun RBSet(Barcode: String): Boolean
    {
        if (CurrentAction != Global.ActionSet.ScanItem)
        {
            FExcStr.text = "Неверно! " + WhatUNeed()
            return false
        }
        var TextQuery: String =
        "SELECT " +
                "Units.parentext as ItemID, " +
                "Goods.SP1036 as InvCode, " +
                "Units.SP2230 as OKEI " +
                "FROM SC2237 as Units (nolock) " +
                "LEFT JOIN SC33 as Goods (nolock) " +
                "ON Goods.id = Units.parentext " +
                "WHERE Units.SP2233 = :Barcode "
        TextQuery = SS.QuerySetParam(TextQuery, "Barcode", Barcode)
        val DT = SS.ExecuteWithRead(TextQuery)?:  return false

        if (DT!!.isEmpty())
        {
            FExcStr.text = "С таким штрихкодом товар не найден! " + WhatUNeed()
            return false
        }
        if (DT!![1][0] != CCItem!!.ID)
        {
            FExcStr.text = "Не тот товар! (отсканирован " + DT!![1][1].trim() + ") " + WhatUNeed()
            return false
        }
        CurrentAction = Global.ActionSet.EnterCount
        val enterCount: EditText = findViewById(R.id.enterCount)
        enterCount.visibility = VISIBLE
        enterCount?.setText("")
        enterCount.setOnKeyListener { v: View, keyCode: Int, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                // сохраняем текст, введенный до нажатия Enter в переменную
                try {
                    val count = enterCount.text.toString().toInt()
                    EnterCountSet(count)
                }
                catch (e: Exception){

                }
            }
            false
        }

        FExcStr.text = WhatUNeed()
        return true
    }

    fun RSCSetInicialization(IDD: String): Boolean {

        val TextQuery: String =
            "SELECT SP3964, descr FROM SC1141 (nolock) WHERE SP1935='$IDD'"
        val result = SS.ExecuteWithRead(TextQuery)
        Section = Model.Section(result!![1][0],IDD, result!!?.get(1)[0], result!![1][1].trim())

        if (Section!!.Type != "12") {
            FExcStr.text = "Неверный тип адреса! Отсканируйте коробку!"
            return false
        } else {
            PreviousAction.text = Section!!.Descr
        }
        return true
    }

    fun RSCSet(IDD: String,thisID: Boolean): Boolean {

        if (CurrentAction == Global.ActionSet.ScanAdress) {
            val TextQuery: String =
                "SELECT ID, SP3964, descr FROM SC1141 (nolock) WHERE SP1935='$IDD'"
            val result = SS.ExecuteWithRead(TextQuery)?:  return false
            Section = Model.Section(result!![1][0], IDD, result!!?.get(1)[0], result!![1][1].trim())

            if (Section!!.Type == "12") {
                FExcStr.text = "Неверно! " + WhatUNeed()
                return false
            }
            if (Section!!.ID != CCItem!!.AdressID) {
                //Переход на другую строку
                return ToModeSet(Section!!.ID, null)
            }
            //&& Const.ImageOn
            if (CCItem!!.Details > 0) {
                CurrentAction = Global.ActionSet.ScanPart
            } else {
                CurrentAction = Global.ActionSet.ScanItem
            }
            FExcStr.text = WhatUNeed()
            return true
        }
        if (CurrentAction == Global.ActionSet.ScanBox) {
            //СКАНИРОВАНИЕ КОРОБКИ
            if (Section!!.Type.toInt() != 12) {
                FExcStr.text = "Неверно! " + WhatUNeed()
                return false
            }
            if (Section!!.ID != DocSet!!.BoxID) {
                FExcStr.text = "Неверная коробка! " + WhatUNeed()
                return false
            }

        } else {
            //Какой-то другой режим вероятно?
            FExcStr.text = "Неверно! " + WhatUNeed()
            return false
        }

//        else if (IsSC(IDDorID, "Принтеры"))
//        {
//            if (!Printer.FoundIDD(IDDorID))
//            {
//                return false;
//            }
//        }
//        else {
//            FExcStr.text = "Неверно! " + WhatUNeed()
//            return false
//        }
        return true
    }

    fun EnterCountSet(Count: Int):Boolean {
        //занулим.дабы не было ошибки с колвом
        CountFact = 0
        if (CurrentAction != Global.ActionSet.EnterCount)
        {
            FExcStr.text = "Неверно! " + WhatUNeed()
            return false
        }
        if (Count <= 0 || (CCItem!!.Count < Count))
        {
            FExcStr.text = "Количество указано неверно! (максимум " + CCItem!!.Count.toString() + ")"
            return false
        }
        CountFact = Count
        return CompleteLineSet()
    }

    fun ScanPartBarcode(CountPart: Int): Boolean
    {
        if (CurrentAction != Global.ActionSet.ScanPart)
        {
            FExcStr.text = "Неверно! " + WhatUNeed()
            return false
        }
        else if (CountPart != CCItem!!.Details)
        {
            FExcStr.text = "Количество деталей неверно! " + WhatUNeed()
            return false
        }
        CurrentAction = Global.ActionSet.ScanItem
        FExcStr.text = WhatUNeed()
        return true
    } // ScanPartBarcode

    fun CompleteLineSet(): Boolean{
        //Заглушка, рефрешим позицию, чтобы не было проблем, если оборвется связь
//        if (!ToModeSet(CCItem!!.AdressID, DocSet!!.ID))
//        {
//            //CCItem!!.CountFact = CountFact;
//            CurrentAction = Global.ActionSet.ScanAdress   //Отключение по константе
//            return false
//        }
//        CurrentAction = Global.ActionSet.ScanAdress   //Отключение по константе
        //конец заглушки
        if (CCItem!!.Count > CountFact) {
//            if (Const.StopCorrect)
//            {
//                FExcStr = "Возможность дробить строку отключена!";
//                return false;
//            }
            //добавить строчку надо
            var TextQuery =
                    "begin tran; " +
                    "update DT2776 " +
                    "set SP3110 = :count, " +
                    "SP3114 =  :count * SP3112 " +
                    "where DT2776.iddoc = :iddoc and DT2776.lineno_ = :currline; " +
                    "if @@rowcount > 0 begin " +
                    "insert into DT2776 (SP3108 , SP3109 , SP3110 ," +
                    "SP3111 , SP3112 , SP3113 , SP3114 ," +
                    "SP3115 , SP3116 , SP3117 , SP4977 ," +
                    "SP5507 , SP5508 , SP5509 , SP5510 ," +
                    "SP5673 , SP5986 , SP5987 , SP5988 , " +
                    "lineno_, iddoc, SP6447 ) " +
                    "select SP3108 , SP3109 , :remaincount ," +
                    "SP3111 , SP3112 , SP3113 , :count * SP3112 ," +
                    "SP3115 , SP3116 , SP3117 , SP4977 ," +
                    "SP5507 , SP5508 , SP5509 , SP5510 ," +
                    "SP5673 , SP5986 , SP5987 , SP5988 , " +
                    "(select max(lineno_) + 1 from DT2776 where iddoc = :iddoc), iddoc, 0 " +
                    "from DT2776 as ForInst where ForInst.iddoc = :iddoc and ForInst.lineno_ = :currline; " +
                    "select max(lineno_) as newline from DT2776 where iddoc = :iddoc; " +
                    "if @@rowcount = 0 rollback tran else commit tran " +
                    "end " +
                    "else rollback"
            TextQuery = SS.QuerySetParam(TextQuery, "count", CountFact)
            TextQuery = SS.QuerySetParam(TextQuery, "remaincount", CCItem!!.Count - CountFact)
            TextQuery = SS.QuerySetParam(TextQuery, "iddoc", DocSet!!.ID)
            TextQuery = SS.QuerySetParam(TextQuery, "currline", CCItem!!.CurrLine)

            val DT = SS.ExecuteWithRead(TextQuery) ?: return false
            //Писать будем в добавленную, так лучше! Поэтому обновляем корректную строчку
            CurrLine = DT[1][0].toInt()
        }
        //фиксируем строку
        var TextQuery = "UPDATE DT2776 WITH (rowlock) " +
                "SET SP5986 = :Date5, " +
                "SP5987 = :Time5, " +
                "SP5988 = :id " +
                "WHERE DT2776.iddoc = :DocCC and DT2776.lineno_ = :lineno_; "


        val sdf = SimpleDateFormat("yyyyMMdd HH:mm:ss")
        val currentDate = sdf.format(Date()).substring(0, 8) + " 00:00:00.000"
        val currentTime = primordial.timeStrToSeconds(sdf.format(Date()).substring(9, 17))

        TextQuery = SS.QuerySetParam(TextQuery, "id", SS.GetVoidID())
        TextQuery = SS.QuerySetParam(TextQuery, "DocCC", DocSet!!.ID)
        TextQuery = SS.QuerySetParam(TextQuery, "Date5", currentDate)
        TextQuery = SS.QuerySetParam(TextQuery, "Time5", currentTime)
        TextQuery = SS.QuerySetParam(TextQuery, "lineno_", CCItem!!.CurrLine)

        if (!SS.ExecuteWithoutRead(TextQuery)){
            return false
        }
        //Запись прошла успешно
        CurrentAction = Global.ActionSet.ScanAdress   //на всякий случай, если там что-нибудь накроется, то во вьюхе по крайней мере нельзя будет повторно ввести количество

        PreviousAction.text= "Отобрано " + CCItem!!.InvCode.trim() + " - " + CountFact.toString() + " шт. (строка " + CCItem!!.CurrLine + ")"

        enterCount.visibility = INVISIBLE
        return ToModeSet(null, null)
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        ReactionKey(keyCode,event)
        return super.onKeyDown(keyCode, event)
    }


    fun ReactionKey(keyCode: Int, event: KeyEvent?){

        when(FCurrentMode) {

            Global.Mode.Set -> {if (!enterCount.isVisible) { RKSet(keyCode, event)} }

            else -> RKSetInicialization(keyCode, event)
        }
        // нажали назад, выйдем и разблокируем доки
        if (keyCode == 4){
            QuitModesSet()
        }
    }

    fun RKSet(keyCode: Int, event: KeyEvent?)
    {

        if (keyCode == 22) //нажали вправо; просмотр табл. части
        {
//            if (!SS.Employer.CanRoute)
//            {
//                string appendix = " (нельзя посмотреть маршрут)";
//                if (lblAction.Text.IndexOf(appendix) < 0)
//                {
//                    lblAction.Text = lblAction.Text + appendix;
//                }
//                return;
//            }

            FExcStr.text = "Подгружаю список..."
            //перейдем на форму просмотра
            val WatchForm = Intent(this, WatchTablePart::class.java)
            WatchForm.putExtra("Employer", Employer)
            WatchForm.putExtra("EmployerIDD",EmployerIDD)
            WatchForm.putExtra("EmployerFlags",EmployerFlags)
            WatchForm.putExtra("EmployerID",EmployerID)
            WatchForm.putExtra("iddoc",DocSet!!.ID)
            WatchForm.putExtra("ItemCode",CCItem!!.InvCode)
            WatchForm.putExtra("addressID",CCItem!!.AdressID)
            WatchForm.putExtra("DocView",DocSet!!.View)
            WatchForm.putExtra("PrinterPath",PrinterPath)
            startActivity(WatchForm)
            finish()


        }
//        else if (Key == Keys.Left && Screan == -1)
//        {
//            DataGrid dgGoodsCC = pnlCurrent.GetDataGridByName("dgGoodsCC");
//            CurrLineSet = dgGoodsCC.CurrentRowIndex;
//            pnlCurrent.Sweep(1);
//            Screan += 1;
//            TextBox tbCount = pnlCurrent.GetTextBoxByName("tbCount");
//            if (tbCount.Visible)
//            {
//                pnlCurrent.GetControlByName("tbCount").Focus();
//            }
//        }

        if ( keyCode == 16 &&CurrentAction != Global.ActionSet.EnterCount && !DocSet!!.Special)
        {
//            if (SS.Const.StopCorrect)
//            {
//                //StopCorrect - ВРЕМЕНННАЯ ЗАГЛУШКА
//                lblAction.Text = "Возможность корректировать отключена!";
//                return;
//            }
            // перейдем на форму корректировки
            val Correct = Intent(this, Correct::class.java)
            Correct.putExtra("Employer", Employer)
            Correct.putExtra("EmployerIDD",EmployerIDD)
            Correct.putExtra("EmployerFlags",EmployerFlags)
            Correct.putExtra("EmployerID",EmployerID)
            Correct.putExtra("iddoc",DocSet!!.ID)
            Correct.putExtra("AddressID",CCItem!!.AdressID)
            Correct.putExtra("PrinterPath",PrinterPath)
            startActivity(Correct)
            finish()
        }

        /*
        if (Screan == 0 && (Key == Keys.Enter || Key == Keys.F14 || Key == Keys.F2 || Key == Keys.F1 || Key.GetHashCode() == 189) && SS.CurrentAction == ActionSet.EnterCount)
        {
            int tmpCount;
            try
            {
                string tmpTxt = pnlCurrent.GetTextBoxByName("tbCount").Text;
                if (tmpTxt.Substring(tmpTxt.Length - 1, 1) == "-")
                {
                    tmpTxt = tmpTxt.Substring(0, tmpTxt.Length - 1);
                }
                tmpCount = Convert.ToInt32(tmpTxt);
            }
            catch
            {
                tmpCount = 0;
            }
            if (SS.EnterCountSet(tmpCount))
            {
                View();
                GoodDone();
                pnlCurrent.GetTextBoxByName("tbCount").Text = ""
            }
            else
            {
                View();
                lblAction.Text = SS.ExcStr;
                BadDone();
            }
        }
        */

    }

    fun RKSetInicialization(keyCode: Int, event: KeyEvent?) {
        // нажали 1
        if (keyCode.toString() == "8") {
            CompleteSetInicialization()
        }
    }

    fun CompleteSetInicialization(): Boolean {
        // решили отказаться
//        if ( BoxSetView.text != "99-00-00-01")
//        {
//            val toast = Toast.makeText(applicationContext, "Коробка не указана!", Toast.LENGTH_SHORT)
//            toast.show()
//            return false
//        }
        // если коробка не указана. будем ставить ее по умолчанию

        var DataMapWrite: MutableMap<String, Any> = mutableMapOf()
        DataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] =
            SS.ExtendID(EmployerID, "Спр.Сотрудники")
        //ставим ID коробки по умолчанию
        DataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход2"] =
            SS.ExtendID("  1IKX   ", "Спр.Секции")
        var DataMapRead: MutableMap<String, Any> = mutableMapOf()
        var FieldList: MutableList<String> = mutableListOf("Спр.СинхронизацияДанных.ДатаРез1")

        DataMapRead = ExecCommand("QuestPicing", DataMapWrite, FieldList, DataMapRead, "")

        if ((DataMapRead["Спр.СинхронизацияДанных.ФлагРезультата"] as String).toInt() == -3) {
            FExcStr.text = DataMapRead["Спр.СинхронизацияДанных.ДатаРез1"].toString()
            return false
        }
        if ((DataMapRead["Спр.СинхронизацияДанных.ФлагРезультата"] as String).toInt() != 3) {
            FExcStr.text = "Не известный ответ робота... я озадачен..."
            return false
        }
        FExcStr.text = DataMapRead["Спр.СинхронизацияДанных.ДатаРез1"].toString()
        return ToModeSetInicialization()
    }

    private fun ToModeSetComplete(): Boolean
    {
        var EmpbtyBox = false
        //Проверим нет ли сборочного с пустой коробкой, его в первую очередь будем закрывать
        var TextQuery =
        "SELECT " +
                "journ.iddoc as IDDOC " +
                "FROM " +
                "_1sjourn as journ (nolock) " +
                "INNER JOIN DH2776 as DocCC (nolock) " +
                "ON DocCC.iddoc = journ.iddoc " +
                "WHERE " +
                "DocCC.SP2773 = :Employer " +
                "and journ.iddocdef = 2776 " +
                "and DocCC.SP2767 = :EmptyDate " +
                "and not DocCC.SP2765 = :EmptyDate " +
                "and DocCC.SP6525 = :EmptyID " +
                "and journ.ismark = 0 "
        TextQuery = SS.QuerySetParam(TextQuery, "Employer", EmployerID)
        TextQuery = SS.QuerySetParam(TextQuery, "EmptyDate", SS.GetVoidDate())
        TextQuery = SS.QuerySetParam(TextQuery, "EmptyID", SS.GetVoidID())
        val DT = SS.ExecuteWithRead(TextQuery) ?: return false
        // Пока закомментирую, тк как делаю отбор в ускоренном режиме без проверки коробки и тд
//        if (DT.Rows.Count > 0)
//        {
//            if (!ToModeSetCompleteAfrerBox(null))
//            {
//                return false;
//            }
//            OnChangeMode(new ChangeModeEventArgs(Mode.SetComplete));
//            return true;
//        }
//        BoxOk = false;
//        DocSet.ID = null;   //Нет конкретного документа
//        FExcStr = "Отсканируйте коробку!";
//        OnChangeMode(new ChangeModeEventArgs(Mode.SetComplete));
//        return true;

        FCurrentMode = Global.Mode.SetComplete
        //перейдем на форму завершения набора
        val SetComplete = Intent(this, SetComplete::class.java)
        SetComplete.putExtra("Employer", Employer)
        SetComplete.putExtra("EmployerIDD",EmployerIDD)
        SetComplete.putExtra("EmployerFlags",EmployerFlags)
        SetComplete.putExtra("EmployerID",EmployerID)
        SetComplete.putExtra("PrinterPath",PrinterPath)
        //закроем доки, висящие на сотруднике с уже набранными строчками
       for (id in DocsSet) {
            SetComplete.putExtra("iddoc",id)
            startActivity(SetComplete)
        }
        finish()
        return true
    }

//    private fun ToModeSetCompleteAfrerBox(Adress: String): Boolean
//    {
//        var EmpbtyBox = false
//        var TextQuery = "select iddoc from DHDH2776 where iddoc in (:Docs) and DHDH2776.SP6525 = :box"
//        TextQuery = TextQuery.replace(":Docs", helper.ListToStringWithQuotes(DocsSet))
//        if (Adress == null)
//        {
//            EmpbtyBox = true
//            TextQuery = SS.QuerySetParam(TextQuery, "box", SS.GetVoidID())
//        }
//        else
//        {                                                               //Adress.ID
//            TextQuery = SS.QuerySetParam(TextQuery, "box", Adress)
//        }
//        val DT = SS.ExecuteWithRead(TextQuery) ?: return false
//        if (DT!!.isEmpty())
//        {
//            FExcStr.text = "Сборочный с такой коробкой не найдет!"
//            return false
//        }               //iddoc
//        if (!LoadDocSet(DT[1][0]))
//        {
//            return false
//        }
//
//        var RepeatCountBox = false
//        //BoxOk = false;
//        //DocSet.Boxes = 0
//        TextQuery =
//            "Select " +
//                    "count(*) as boxes " +
//                    "from $Спр.МестаПогрузки (nolock) " +
//                    "where $Спр.МестаПогрузки.КонтрольНабора = :iddoc"
//        QuerySetParam(ref TextQuery, "iddoc", DocSet!!.ID)
//        DT.Clear();
//        if (ExecuteWithRead(TextQuery, out DT))
//        {
//            if (DT.Rows.Count > 0)
//            {
//                DocSet.Boxes = (int)DT.Rows[0]["boxes"];
//            }
//        }
//
//        TextQuery =
//            "Select " +
//                    "ISNULL(RefSection.$Спр.Секции.МаксКорешков , 0) as MaxStub " +
//                    "from DH$КонтрольНабора as DocCC (nolock) " +
//                    "left join $Спр.Секции as RefSection (nolock) " +
//                    " on DocCC.$КонтрольНабора.Сектор = RefSection.id " +
//                    "where DocCC.iddoc = :iddoc";
//        QuerySetParam(ref TextQuery, "iddoc", DocSet.ID);
//        DT.Clear();
//        if (!ExecuteWithRead(TextQuery, out DT))
//        {
//            return false;
//        }
//        DocSet.MaxStub = (int)(decimal)DT.Rows[0]["MaxStub"];
//
//        BoxForSet = Adress;
//        if (Employer.SelfControl)
//        {
//            TextQuery =
//                "select top 1 iddoc " +
//                        "from DT$КонтрольНабора as DocT (nolock) " +
//                        "where " +
//                        "iddoc = :iddoc " +
//                        "and DocT.$КонтрольНабора.Состояние0 = 2 " +
//                        "and DocT.$КонтрольНабора.Контроль <= 0 " +
//                        "and DocT.$КонтрольНабора.СостояниеКорр = 0 " +
//                        "and DocT.$КонтрольНабора.Количество > 0 ";
//            QuerySetParam(ref TextQuery, "iddoc", DocSet.ID);
//            DT.Clear();
//            if (!ExecuteWithRead(TextQuery, out DT))
//            {
//                return false;
//            }
//            if (DT.Rows.Count > 0)
//            {
//                //Включен самоконтроль и осталось что-то "поконтролить"
//                return ToModeSetSelfControl();
//            }
//        }
//        return true;
//    } // ToModeSetCompleteAfterBox

    private fun LoadDocSet(iddoc: String): Boolean
    {
        var TextQuery =
        "SELECT top 1 " +
                "journ.iddoc as IDDOC, " +
                "DocCC.SP2841 as SelfRemovel, " +
                "DocCC.SP2814 as Rows, " +
                "journForBill.iddoc as Bill, " +
                "Clients.descr as Client, DocCC.SP3114 as Sum, " +
                "Bill.SP3094 as TypeNakl, AdressBox.descr as Box, isnull(DocCC.SP6525 , '     0   ') as BoxID " +
                "FROM _1sjourn as journ (nolock) " +
                "INNER JOIN DH2776 as DocCC (nolock) ON DocCC.iddoc = journ.iddoc" +
                "LEFT JOIN DH2763 as DocCB (nolock) ON DocCB.iddoc = DocCC.SP2771 " +
                "LEFT JOIN DH196 as Bill (nolock) ON Bill.iddoc = DocCB.SP2759 " +
                "LEFT JOIN _1sjourn as journForBill (nolock) ON journForBill.iddoc = Bill.iddoc " +
                "LEFT JOIN SC1141 as Section (nolock) ON Section.id = DocCC.SP2764" +
                "LEFT JOIN SC46 as Clients (nolock) ON Bill.SP199 = Clients.id " +
                "LEFT JOIN SC1141 as AdressBox (nolock) ON AdressBox.id = DocCC.SP6525" +
                "WHERE " +
                "journ.iddoc = :iddoc "
        TextQuery = SS.QuerySetParam(TextQuery, "iddoc", iddoc)
        val DataTable = SS.ExecuteWithRead(TextQuery) ?: return false

        if (DataTable!!.isNotEmpty())
        {
            DocSet = Model.StrictDoc(
                DataTable[1][0],
                DataTable[1][1].toInt(),
                "",
                DataTable[1][2].toInt(),
                DataTable[1][3],
                DataTable[1][4].trim(),
                DataTable[1][5].toBigDecimal(),
                DataTable[1][6].toInt() == 2,
                DataTable[1][7],
                DataTable[1][8]
            )

            return true
        }

        return false
    }

    fun ExecCommand(
        Command: String,
        DataMapWrite: MutableMap<String, Any>,
        FieldList: MutableList<String>,
        DataMapRead: MutableMap<String, Any>,
        commandID: String
    ): MutableMap<String, Any> {
        //тк в котлине нельзя переприсвоить значение переданному в фун параметру, создаю еще 1 перем
        var CommandID: String = ""
        var beda: Int = 0

        if (commandID == "") {
            CommandID = SendCommand(Command, DataMapWrite, FieldList)
        }
        //Ждем выполнения или отказа
        val query =
            "SELECT " + SS.GetSynh("Спр.СинхронизацияДанных.ФлагРезультата") + " as Flag" + (if (FieldList.size == 0) "" else "," + SS.ToFieldString(
                FieldList
            )) +
                    " FROM " + SS.GetSynh("Спр.СинхронизацияДанных") + " (nolock)" +
                    " WHERE ID='" + CommandID + "'"

        var WaitRobotWork: Boolean = false
        val sdf = SimpleDateFormat("HH:mm:ss")
        var TimeBegin: Int = primordial.timeStrToSeconds(sdf.format(Date()))
        while (kotlin.math.abs(TimeBegin - primordial.timeStrToSeconds(sdf.format(Date()))) < ResponceTime) {

            val DataTable = SS.ExecuteWithRead(query)
            //Ждем выполнения или отказа
            if (DataTable == null) {
                FExcStr.text = "Нет доступных команд! Ошибка робота!"
            }
            DataMapRead["Спр.СинхронизацияДанных.ФлагРезультата"] = DataTable!![1][0]
            if ((DataMapRead["Спр.СинхронизацияДанных.ФлагРезультата"] as String).toInt() != 1) {
                if ((DataMapRead["Спр.СинхронизацияДанных.ФлагРезультата"] as String).toInt() == 2) {
                    if (!WaitRobotWork) {
                        //1C получила команду, сбросим время ожидания
                        TimeBegin = primordial.timeStrToSeconds(sdf.format(Date()))
                        WaitRobotWork = true
                    }
                    continue
                }
                var i = 1
                while (i < DataTable!!.size) {
                    DataMapRead[FieldList[i - 1]] = DataTable!![1][i]
                    i++
                }
                return DataMapRead
            } else {
                beda++
                continue   //Бред какой-то, попробуем еще раз
            }
            if (TimeBegin + 1 < primordial.timeStrToSeconds(sdf.format(Date()))) {
                //Пауза в 1, после первой секунды беспрерывной долбежки!
                val tb: Int = primordial.timeStrToSeconds(sdf.format(Date()))
                while (kotlin.math.abs(tb - primordial.timeStrToSeconds(sdf.format(Date()))) < 1) {

                }
            }
        }
        FExcStr.text = "1C не ответила! " + (if (beda == 0) "" else " Испарений: $beda")
        return DataMapRead

    }

    fun SendCommand(
        Command: String,
        DataMapWrite: MutableMap<String, Any>,
        FieldList: MutableList<String>
    ): String {
        var CommandID: String
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val currentDate = sdf.format(Date()).substring(0, 10) + " 00:00:00.000"
        val currentTime = primordial.timeStrToSeconds(sdf.format(Date()).substring(11, 19))

        val TextQuery: String =
            "BEGIN TRAN; " +
                    "DECLARE @CommandID as varchar(9); " +
                    "SELECT TOP 1 @CommandID = ID FROM " + SS.GetSynh("Спр.СинхронизацияДанных") + " (tablockx) " +
                    "WHERE " + SS.GetSynh("Спр.СинхронизацияДанных.ФлагРезультата") + "=0; " +
                    "UPDATE " + SS.GetSynh("Спр.СинхронизацияДанных") +
                    " SET DESCR='" + Command + "'," + primordial.ToSetString(DataMapWrite) + (if (DataMapWrite.isEmpty()) "" else ",") +
                    SS.GetSynh("Спр.СинхронизацияДанных.Дата") + " = '" + currentDate + "', " +
                    SS.GetSynh("Спр.СинхронизацияДанных.Время") + " = " + currentTime + ", " +
                    SS.GetSynh("Спр.СинхронизацияДанных.ФлагРезультата") + " = 1," +
                    SS.GetSynh("Спр.СинхронизацияДанных.ИДТерминала") + " = 'Android'" +
                    " WHERE ID=@CommandID; " +
                    " SELECT @@rowcount as Rows, @CommandID as CommandID; " +
                    "COMMIT TRAN;"
        val DataTable = SS.ExecuteWithRead(TextQuery)
        if (DataTable == null) {
            FExcStr.text = "Нет доступных команд! Ошибка робота!"
        }
        CommandID = DataTable!![1][1]
        return CommandID
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
