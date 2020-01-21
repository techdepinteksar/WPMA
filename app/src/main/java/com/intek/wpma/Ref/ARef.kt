package com.intek.wpma.Ref

import android.os.Build.ID
import com.intek.wpma.SQL.SQL1S
import java.util.*
import java.util.jar.Attributes

abstract class ARef() {

    protected var SS: SQL1S = SQL1S()
    protected var FID:String = ""
    protected var FName:String = ""
    protected var FCode:String = ""
    protected var FIsMark:Boolean = false

    protected var HaveName:Boolean = false
    protected var HaveCode:Boolean = false

    private var Attributes: MutableMap<String,Any> = mutableMapOf()

    protected abstract val TypeObj:String get

    val ID:String get() = FID
    val Name:String get() = FName
    val Code:String get() = FCode
    val Selected:Boolean get() = FID != null
    val IsMark:Boolean get() = FIsMark

    /*
   private fun FoundIDDorID (IDDorID: String,ThisID: Boolean): Boolean
    {
        FID = null
        val prefix = "Спр." + TypeObj + "."
        var DataMap: MutableMap<String, String>
        var FieldList: MutableList<String>
        FieldList = emptyList<String>() as MutableList<String>
        FieldList.add("ID")
        FieldList.add("ISMARK")
        if (HaveName)
        {
            FieldList.add("DESCR");
        }
        if (HaveCode)
        {
            FieldList.add("CODE");
        }
        if (!ThisID)
        {
            FieldList.add(prefix + "IDD");
        }
        val ServCount: Int = FieldList.count()    //Количество сервисных полей
        SS.AddKnownAttributes(prefix, FieldList)
        DataMap = SS.GetSCData(IDDorID,TypeObj,FieldList, DataMap,true)!!
        if (//!SS.GetSCData(IDDorID, TypeObj, FieldList, out DataMap, ThisID))
        {
            return false;
        }
        FID = DataMap["ID"].toString()
        FIsMark = (bool)DataMap["ISMARK"];
        FCode = HaveCode ? DataMap["CODE"].ToString() : null;
        FName = HaveName ? DataMap["DESCR"].ToString().Trim() : null;
        //Добавляем оставшиеся поля в словарик
        for (int i = ServCount + 1; i < FieldList.Count; i++)
        {
            Attributes[FieldList[i].Substring(prefix.Length)] = DataMap[FieldList[i]];
        }
        return true
    }

    public bool FoundIDD(string IDD)
    {
        return FoundIDDorID(IDD, false);
    }
    public bool FoundID(string ID)
    {
        return FoundIDDorID(ID, true);

    public object GetAttribute(string Name)
    {
        //Классная штука ниже, но опасная, может что-то наебнуться. Неохота думать
        //if (!Selected)
        //{
        //    throw new NullReferenceException("Reference element not selected");
        //}
        if (Attributes.ContainsKey(Name))
        {
            return Attributes[Attributes.Name];
        }
        //Подгружаем недостающий атрибут (он добавится в карту соответствия и будет в дальнейшем подгружаться сразу)
        Dictionary<string, object> DataMap;
        if (!SS.GetSCData(FID, TypeObj, Name, out DataMap, true))
        {
            return "";    //не срослось
        }
        object result = DataMap["Спр." + TypeObj + "." + Name];
        Attributes[Name] = result;
        return result;
    }
    public virtual void Refresh()
    {
        if (Selected)
        {
            FoundIDDorID(ID, true);
        }
    }


    //---------------
    //Две процедуры ниже нужно переработать как-то в одну, я хз как пока

    /// <summary>
    /// write in you property: %Prop% get { return GetSectionProperty(%name%, ref %FProp%); }
    /// </summary>
    /// <param name="name">1C name of prop</param>
    /// <param name="val">field stored object</param>
    /// <returns>value of property</returns>
    protected RefSection GetSectionProperty(string name, ref RefSection val)
    {
        if (val == null)
        {
            val = new RefSection(SS);
        }
        string currId = GetAttribute(name).ToString();
        if (val.ID != currId)
        {
            val.FoundID(currId);
        }
        return val;
    } // GetGatesProperty
    /// <summary>
    /// write in you property: %Prop% get { return GetSectionProperty(%name%, ref %FProp%); }
    /// </summary>
    /// <param name="name">1C name of prop</param>
    /// <param name="val">field stored object</param>
    /// <returns>value of property</returns>
    protected RefGates GetGatesProperty(string name, ref RefGates val)
    {
        if (val == null)
        {
            val = new RefGates(SS);
        }
        string currId = GetAttribute(name).ToString();
        if (val.ID != currId)
        {
            val.FoundID(currId);
        }
        return val;
    } // GetGatesProperty
    */

}
