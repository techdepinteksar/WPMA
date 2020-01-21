package com.intek.wpma.NewStruct

import com.intek.wpma.Model.Model
import com.intek.wpma.SQL.SQL1S
import java.util.*

class ABaseMode {
    val SS: Model = Model()
    fun Login(EmployerID: String): Boolean
    {
//        if (!SS.UpdateProgram())
//        {
//            return false
//        }
//        if (!SS.SynhDateTime())
//        {
//            return false
//        }
        if (!SS.IBS_Inicialization(EmployerID)){
            return false
        }

        var DataMapWrite: MutableMap<String, Any> = mutableMapOf()
        DataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] = SS.ExtendID(EmployerID, "Спр.Сотрудники")
        DataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = "Android"
        if (!SS.ExecCommandNoFeedback("Login", DataMapWrite))
        {
            return false
        }
        return true
    } // Login
}
