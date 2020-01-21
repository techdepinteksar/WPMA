package com.intek.wpma.Model

import android.app.DownloadManager
import com.intek.wpma.Global
import com.intek.wpma.SQL.SQL1S
import net.sourceforge.jtds.jdbc.DateTime
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

class Model : SQL1S() {

    private val FOKEIUnit: String       = "     1   "
    private val FOKEIPack: String       = "     2   "
    private val FOKEIPackage: String    = "     E   "
    private val FOKEIKit: String        = "     A   "
    private val FOKEIOthers: String     = "     0   "
    val OKEIUnit: String get() { return FOKEIUnit }
    val OKEIPack: String get() { return FOKEIPack }
    val OKEIPackage: String get() { return FOKEIPackage }
    val OKEIKit: String get() { return FOKEIKit }
    val OKEIOthers: String get() { return FOKEIOthers }

    data class StrictDoc(
        // оставил только то, что используется в отборе
        val ID: String,
        var SelfRemovel: Int,
        var View: String,
        var Rows: Int,
        var FromWarehouseID: String,
        var Client: String,
        var Sum: BigDecimal,
        var Special: Boolean,
        var Box: String,
        var BoxID: String
        //var FromWarehouseName: String,
        //var ToWarehouseID: String,
        //var ToWarehouseName: String,
        //var ToWarehouseSingleAdressMode: Boolean,
        //var FoundDoc: String, // документ основание
        //var Boxes: Int,
        //var AllBoxes: Int,
        //var AdressCollect: String,
        //var Sector: String,
        //var MaxStub: Int,
        //var NumberBill: String,
        //var NumberCC: Int,
        //var MainSectorName: String,
        //var SetterName: String,
        //var IsFirstOrder: Boolean
    )

    data class Section(
        var ID: String,
        var IDD: String,
        var Type: String,
        var Descr: String
    )

    data class StructItemSet(
        // оставил только то, что используется в отборе
        val ID: String,
        var InvCode: String,
        var Name: String,
        var Price: BigDecimal,
        var Count: Int,
        var CountFact: Int,
        var AdressID: String,
        var AdressName: String,
        var CurrLine: Int,
        var Balance: Int,
        var Details: Int,
        var OKEI2Count: Int,
        var OKEI2: String,
        var OKEI2Coef: Int
    )

    fun IBS_Inicialization(EmployerID: String): Boolean
    {
        var TextQuery =
        "set nocount on; " +
                "declare @id bigint; " +
                "exec IBS_Inicialize_with_DeviceID_new :Employer, :HostName, :DeviceID, @id output; " +
                "select @id as ID;"
        TextQuery = QuerySetParam(TextQuery, "Employer", EmployerID)
        TextQuery = QuerySetParam(TextQuery, "HostName", "Android")
        //пока присвою жесткий id                                       DeviceID.GetDeviceID()
        TextQuery = QuerySetParam(TextQuery, "DeviceID", "Android_ID")
        val DT = ExecuteWithRead(TextQuery) ?: return false
        if (DT.isEmpty())
        {
            return false
        }
        return DT!![1][0].toInt() > 0

    }

    /// <summary>
    ///  отсылает команду в 1С и не ждет ответа
    /// </summary>
    /// <param name="Command"></param>
    /// <param name="DataMapWrite"></param>
    /// <returns></returns>
    fun ExecCommandNoFeedback(Command: String, DataMapWrite: MutableMap<String, Any>): Boolean
    {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val currentDate = sdf.format(Date()).substring(0, 10) + " 00:00:00.000"
        val currentTime = timeStrToSeconds(sdf.format(Date()).substring(11, 19))
        val Query =
        "UPDATE " + GetSynh("Спр.СинхронизацияДанных") +
                " SET DESCR='" + Command + "'," + ToSetString(DataMapWrite) + (if(DataMapWrite.isEmpty())  "" else ",") +
        GetSynh("Спр.СинхронизацияДанных.Дата") + " = '" + currentDate + "', " +
                GetSynh("Спр.СинхронизацияДанных.Время") + " = " + currentTime + ", " +
                GetSynh("Спр.СинхронизацияДанных.ФлагРезультата") + " = 1," +
                GetSynh("Спр.СинхронизацияДанных.ИДТерминала") + " = '" + "Android_ID" + "'" +
                " WHERE ID = (SELECT TOP 1 ID FROM " + GetSynh("Спр.СинхронизацияДанных") +
                " WHERE " + GetSynh("Спр.СинхронизацияДанных.ФлагРезультата") + "=0)"
        if (!ExecuteQuery(Query, false))
        {
            return false
        }
        return true
    }

    /// <summary>
    /// формирует строку присвоений для инструкции SET в UPDATE из переданной таблицы
    /// Поддерживает типы - int, DateTime, string
    /// </summary>
    /// <param name="DataMap"></param>
    /// <returns></returns>
    fun ToSetString(DataMap: MutableMap<String, Any>): String {
        var result: String = ""
        for (pair in DataMap) {
            result += GetSynh(pair.key) + "=" + ValueToQuery(pair.value) + ","
        }
        //удаляем последнюю запятую
        if (result.isNotEmpty()) {
            result = result.substring(0, result.length - 1)
        }
        return result
    }

    fun timeStrToSeconds(str: String): Int {
        val parts = str.split(":")
        var result = 0
        for (part in parts) {
            val number = part.toInt()
            result = result * 60 + number
        }
        return result
    }


}