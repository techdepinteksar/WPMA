package com.intek.wpma.SQL

import com.intek.wpma.Helpers.Helper
import net.sourceforge.jtds.jdbc.DateTime
import java.lang.Exception
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*


/// <summary>
/// Класс организующий доступ и синхронизацию с базой данных компании
/// </summary>

open class SQL1S : SQLSynchronizer() {

    val SynhMap: MutableMap<String, String> =
        mutableMapOf()       //хеш-таблица, сопоставляет имена 1С с именами SQL
    val ExclusionFields: MutableList<String> = mutableListOf()
    var helper: Helper? = null

    /*Конструктор класса

     */
    init {
        //стандартные поля, не будем извафлятся типа "Наименование" или "Код", дабы не ломать пальцы переключая раскладку
        ExclusionFields.add("ID")
        ExclusionFields.add("DESCR")
        ExclusionFields.add("CODE")
        ExclusionFields.add("ISMARK")
        ExclusionFields.add("DATE_TIME_IDDOC")
        ExclusionFields.add("IDDOCDEF")
        ExclusionFields.add("DOCNO")

        for (curr in ExclusionFields) {
            SynhMap.put(curr, curr)
        }
    }

    /*Функция по выполнению запроса с возвратом результата
     TextQuery - текст запроса, который надо выполнить
     Если не отработал запрос то возвращается null
     Если отработал то возвращается двумерный массив
     */
    fun ExecuteWithRead(TextQuery: String): Array<Array<String>>? {
        var MyArr: Array<Array<String>> = emptyArray()
        if (!ExecuteQuery(QueryParser(TextQuery))) {
            return null
        }
        if (MyReader == null) {
            return null
        }
        while (MyReader!!.next()) {
            var i = 1
            var ColumnArray: Array<String> = emptyArray()
            var RowsArray: Array<String> = emptyArray()
            while (i <= MyReader!!.metaData.columnCount) {
                //заполним наименования колонок
                ColumnArray += MyReader!!.metaData.getColumnName(i)
                //а теперь значение
                RowsArray += if (MyReader!!.getString(MyReader!!.metaData.getColumnName(i)) == null){
                    "null"
                } else
                    MyReader!!.getString(MyReader!!.metaData.getColumnName(i))
                i++
            }
            if (MyArr.isEmpty()) {
                MyArr += ColumnArray
            }
            MyArr += RowsArray

        }

        MyReader!!.close()

        return MyArr
    }

    /*Функция по выполнению запроса без возвращзаения
         TextQuery - текст запроса, торый надо выполнить
         Если не отработал запрос то возвращается false
         Если отработал то возвращается true
    */
    fun ExecuteWithoutRead(TextQuery: String): Boolean {
        return ExecuteQuery(QueryParser(TextQuery), false)
    }

    /*Возвращает пустой ID

     */
    fun GetVoidID(): String {
        return "     0   "
    }

    /*Пустая дата

     */
    fun GetVoidDate(): String {
        //return "17530101"
        //return "1/1/1753 12:00:00 AM"
        return "17530101 00:00:00.000"
    }

    /*
     reserved words:
         EmptyDate
         EmptyID
         NowDate
         NowTime
    <param name="TextQuery"></param>
    <param name="NameParam"></param>
    <param name="Value"></param>
    Устанавливает значение параметра в запросе
     */
    fun QuerySetParam(TextQuery: String, NameParam: String, Value: Any): String {
        return TextQuery.replace(":$NameParam", ValueToQuery(Value))
    }

    /*
     <param name="Value"></param>

     */
    fun ValueToQuery(Value: Any): String {
        var result = Value.toString()

        if (Value is Int) {
            result = Value.toString()
        } else if (Value is DateTime) {
            result = "'" + DateTimeToSQL((Value)) + "'"
        } else if (Value is String) {
            result = "'$Value'"
        }


        return result
    }

    /// <summary>
    ///
    /// </summary>
    /// <param name="TextQuery"></param>
    /// <returns></returns>
    public fun QueryParser(TextQuery: String): String {
        /*
         QuerySetParam(ref TextQuery, "EmptyDate", GetVoidDate());
         QuerySetParam(ref TextQuery, "EmptyID", GetVoidID());
         QuerySetParam(ref TextQuery, "NowDate", DateTime.Now);
         QuerySetParam(ref TextQuery, "NowTime", APIManager.NowSecond());

         string result = TextQuery;
         int curI = result.IndexOf('$');
         while (curI != -1)
         {
             int endI = result.Substring(curI+1).IndexOf(' ');
             string part = result.Substring(curI+1, endI);
             result = result.Replace("$" + part + " ", GetSynh(part) + " ");
             curI = result.IndexOf('$');
         }
         return result;        */
        return TextQuery

    }

    /*


                 /// <summary>
                 /// load Aliases. list of aliases inside this method
                 /// </summary>
                 /// <returns></returns>
                 public bool LoadAliases()
                 {
                     //Начальная загрузка псевдонимов. Лениво делать список...
                     //в принципе метод - нахуй не нужный.
                     List<string> DefaultAlies = new List<string>();
                     #region to chto srazu podsoset v kesh
                     //Таблица синхронизации имен
                     DefaultAlies.Add("Константа.ТоварДляЕдиниц");
                     DefaultAlies.Add("Константа.ОснСклад");
                     #endregion

                     string result = "'";
                     for (int i = 0; i < DefaultAlies.Count; i++)
                     {
                         result += DefaultAlies[i] + "','";
                     }
                     //удаляем последнюю запятую и ковычки "','"
                     result = result.Substring(0, result.Length - 2);

                     string TextQuery =
                     "select Name1C as Name1C, NameSQL as NameSQL from RT_Aliases (nolock) where Name1C in (" + result + ")";
                     DataTable DT;
                     if (!ExecuteWithRead(TextQuery, out DT))
                     {
                         return false;
                     }
                     DataRow[] DR = DT.Select();
                     foreach(DataRow dr in DR)
                     {
                         SynhMap[dr["Name1C"].ToString().Trim()] = dr["NameSQL"].ToString().Trim();
                     }
                     return true;
                 }
                 /// <summary>
                 ///
                 /// </summary>
                 /// <param name="parent"></param>
                 /// <param name="AttributeList"></param>
                 */
    fun AddKnownAttributes(parent: String, AttributeList: MutableList<String>) {
        for (pair in SynhMap) {
            if (pair.key.length >= parent.length) {
                if (pair.key.substring(0, parent.length) == parent) {
                    val part: String = pair.key.substring(parent.length)
                    if (part.length > 0) {
                        AttributeList.add(pair.key)
                    }
                }
            }
        }
    }

    /*
    /// <summary>
    ///
    /// </summary>
    /// <param name="Alias"></param>
    /// <returns></returns>
    public int DebugGetSyng(string Alias)
    {
        int Start = Environment.TickCount;

        //DataRow[] DR = DTAlias.Select("substring(Name1C, 1, 14) = 'Спр.Сотрудники'");

        int result = 0;


        DebugHowLong = Environment.TickCount - Start;
        //return DR.Length;
        return result;
    }
    /// <summary>
    /// Get sql name by 1C Alias
    /// </summary>
    /// <param name="Alias">1C name</param>
    /// <returns>SQL name</returns>
    */
    fun GetSynh(Alias: String): String {
        if (SynhMap.containsKey(Alias)) {
            return SynhMap[Alias].toString()
        }
        var TextQuery: String =
            "select top 1 NameSQL as NameSQL from RT_Aliases (nolock) where Name1C = :Alias"
        TextQuery = QuerySetParam(TextQuery, "Alias", Alias)
        var DT: Array<Array<String>>
        if (ExecuteWithRead(TextQuery) == null) {
            throw Exception("Cant connect for load this KEY $Alias!")
        } else {
            DT = ExecuteWithRead(TextQuery)!!
        }
        if (DT.isEmpty()) {
            throw Exception("Cant find this KEY $Alias!")
        }
        //val result: String = DT[0]["NameSQL"]
        //ОТТЕСТИРОВАТЬ
        val result: String = DT[1][0].trim()
        SynhMap[Alias] = result
        return result
    }

    /*
                 /// <summary>
                 ///
                 /// </summary>
                 /// <param name="TextQuery"></param>
                 /// <param name="result"></param>
                 public void ExecuteWithReadNew(string TextQuery, out DataTable result)
                 {
                     result = new DataTable();
                     if (!ExecuteQuery(QueryParser(TextQuery)))
                     {
                         throw new TransportExcception(ExcStr);
                     }
                     for (int i = 0; i < MyReader.FieldCount; i++)
                     {
                         result.Columns.Add(MyReader.GetName(i), MyReader.GetFieldType(i));
                     }

                     while (MyReader.Read())
                     {
                         DataRow dr = result.NewRow();
                         for (int col = 0; col < MyReader.FieldCount; col++)
                         {
                             dr[col] = MyReader.GetValue(col);
                         }
                         result.Rows.Add(dr);
                     }
                     MyReader.Close();
                 } // Обратная совместимость
                 /// <summary>
                 /// Возвращает просто конкретное значение без всякой таблицы
                 /// </summary>
                 /// <param name="TextQuery"></param>
                 /// <returns></returns>
                 public object ExecuteScalar(string TextQuery)
                 {
                     if (!ExecuteQuery(QueryParser(TextQuery)))
                     {
                         throw new TransportExcception(ExcStr);
                     }
                     object result = null;
                     if (MyReader.Read())
                     {
                         result = MyReader.GetValue(0);
                     }
                     MyReader.Close();
                     return result;
                 } // ExecuteScalar
                 /// <summary>
                 ///
                 /// </summary>
                 /// <param name="TextQuery"></param>
                 public void ExecuteWithoutReadNew(string TextQuery)
                 {
                     if (!ExecuteQuery(QueryParser(TextQuery), false))
                     {
                         throw new TransportExcception(ExcStr);
                     }
                 } // Обратная совместимость
                 /// <summary>
                 /// Преобразует имя поля или таблицы SQL в имя 1С
                 /// </summary>
                 /// <param name="SQLName"></param>
                 /// <returns></returns>
                 protected string To1CName(string SQLName)
                 {
                     string result = "";
                     foreach (KeyValuePair<string, string> pair in SynhMap)
                     {
                         if (pair.Value == SQLName)
                         {
                             result = pair.Key;
                             return result;
                         }
                     }

                     SQL1S tmpSS = new SQL1S(ServerName, DBName);
                     //нихуя не найдено, подсосем из базы!
                     string TextQuery =
                     "select top 1 Name1C as Name1C from RT_Aliases (nolock) where NameSQL = :SQLName";
                     QuerySetParam(ref TextQuery, "SQLName", SQLName);
                     DataTable DT;
                     if (!tmpSS.ExecuteWithRead(TextQuery, out DT))
                     {
                         throw new Exception("Cant connect for load this SQL name! Sheet!");
                     }
                     if (DT.Rows.Count == 0)
                     {
                         throw new Exception("Cant find this SQL name! Sheet!");
                     }
                     tmpSS.MyConnection.Close();
                     result = DT.Rows[0]["Name1C"].ToString().Trim();
                     SynhMap[result] = SQLName;    //add in dictionary
                     return result;
                 }
                 /// <summary>
                 /// Преобразует дату из DateTime в формат в котором будем писать его в SQL, вот он: YYYY-DD-MM 05:20:00.000
                 /// </summary>
                 /// <param name="DateTime"></param>
                 /// <returns></returns>
                 */
    fun DateTimeToSQL(DateTime: DateTime): String { // из-за отсутствия типа DateTime в kotlin функция нуждается в отладке
        //YYYYMMDD hh:mm:ss.nnn
        //return DateTime.Year.ToString() +
        //        DateTime.Month.ToString().PadLeft(2, '0') +
        //        DateTime.Day.ToString().PadLeft(2, '0') + " 00:00:00.000"
        return DateTime.toDate().toString() +
                DateTime.toTime().toString()

    }


    /*
     /// <summary>
     /// Преобразует текстовое представление дыты как она есть в полях DATE_TIME таблиц (они текстовые) в тип DateTime
     /// </summary>
     /// <param name="StrDateTime"></param>
     /// <returns></returns>
     static protected DateTime SQLToDateTime(string StrDateTime)
     {
         //Пока что без времени
         return Convert.ToDateTime(StrDateTime.Substring(0, 4) + "." +
                 StrDateTime.Substring(4, 2) + "." +
                 StrDateTime.Substring(6, 2));
     }
     */
     /// <summary>
     /// Get extend ID, include ID and 4 symbols determining the type (in 36-dimension system)
     /// </summary>
     /// <param name="ID"></param>
     /// <param name="Type"></param>
     /// <returns>Extend ID (13 symbols)</returns>
     fun ExtendID(ID: String, Type: String): String
     {
         var bigInteger: BigInteger
         if (GetSynh(Type).substring(0, 2) == "SC")
         {
             bigInteger = GetSynh(Type).substring(2, GetSynh(Type).length).toBigInteger()
             var result = bigInteger.toString(36).toUpperCase().padStart(4) + ID
             return result
         }
         else
         {
             bigInteger = GetSynh(Type).toBigInteger()
             return bigInteger.toString(36).toUpperCase().padStart(4) + ID
         }
     }
    /*
     public bool GetColumns(string table_name, out string columns, string SQLfunc)
     {
         string separator = ",";
         string tail = "";   //В конце что добавим
         columns = "";
         if (SQLfunc != null)
         {
             separator = ")," + SQLfunc + "(";
             columns = SQLfunc + "(";
             tail = ")";
         }
         string TextQuery =
         "declare @ColumnList varchar(1000); " +
                 "select @ColumnList = COALESCE(@ColumnList + '" + separator + "', '') + column_name " +
                 "from INFORMATION_SCHEMA.Columns " +
                 "where table_name = :table_name; " +
                 "select @ColumnList as ColumnList";
         SQL1S.QuerySetParam(ref TextQuery, "table_name", table_name + " "); //Пробел в конце, чтобы парсер нормально отработал
         DataTable DT;
         ExecuteWithReadNew(TextQuery, out DT);
         if (DT.Rows.Count == 0)
         {
             return false;
         }

         columns += DT.Rows[0]["ColumnList"].ToString();
         columns += tail;
         return true;
     } // GetColumns
     public bool GetCollumns(string table_name, out string columns, string SQLfunc)
     {
         return GetColumns(table_name, out columns, null);
     } // GetCollumns


     /// <summary>
     /// Check DateTime on concept 1C
     /// </summary>
     /// <param name="DateTime">Verifiable DateTime</param>
     /// <returns>true - if DateTime is void on concept 1C</returns>
     static public bool IsVoidDate(DateTime DateTime)
     {
         //Тут можно и по красивей написать...
         if (DateTime.Year == 1753)
         {
             return true;
         }
         else
         {
             return false;
         }
     }

     /// <summary>
     ///
     /// </summary>
     /// <param name="DateTime"></param>
     /// <returns></returns>
     static public string GetPeriodSQL(DateTime DateTime)
     {
         return "{d '" + DateTime.Year.ToString() + "-" + DateTime.Month.ToString().PadLeft(2, '0') + "-01'}";
     }
     /// <summary>
     /// Приводит передаваемый список строк в строку разделенную запятыми
     /// </summary>
     /// <param name="DataList"></param>
     /// <returns></returns>
     */
    fun ToFieldString(DataList: MutableList<String>): String {
        var result: String = ""
        for (item in DataList) {
            result += GetSynh(item) + " as " + if("." in item) item.replace(".","") else ""  + ","
        }
        //удаляем последнюю запятую
        if (result.isNotEmpty()) {
            result = result.substring(0, result.length - 1)
        }
        return result
    }

    /*
                 /// <summary>
                 /// возвращает список данных элемента справочника по его IDD или же ID (регулируется параметром ThisID)
                 /// </summary>
                 /// <param name="IDDorID"></param>
                 /// <param name="SCType"></param>
                 /// <param name="FieldList"></param>
                 /// <param name="DataMap"></param>
                 /// <param name="ThisID"></param>
                 /// <returns></returns>
                 */
    //пока переписал эту функцию принимающую FieldList: String вместо FieldList: MutableList<String>
    // чтобы это исправить нужно поднять функцию StringToList из класса Helper
    fun GetSCData(
        IDDorID: String,
        SCType: String,
        FieldList: MutableList<String>,
        DataMap: MutableMap<String, Any>,
        ThisID: Boolean
    ): MutableMap<String, Any>? {
        val SCType = "Спр." + SCType

        if (!ExecuteQuery(
                "SELECT " + ToFieldString(FieldList) + " FROM " + GetSynh(SCType) + " (nolock)" +
                        " WHERE " + (if (ThisID) {
                    "ID"
                } else {
                    GetSynh("$SCType.IDD")
                } + "='" + IDDorID + "'")
            )
        ) {
            return null
        }

        if (MyReader!!.next()) {
            var i = 1
            while (i <= MyReader!!.metaData.columnCount) {
                DataMap.put(
                    FieldList.get(i),
                    MyReader!!.getString(MyReader!!.metaData.getColumnName(i))
                )
                i++
            }
            MyReader!!.close()
            return DataMap
        } else {
            MyReader!!.close()
            FExcStr = "Элемент справочника не найден!"
            return null
        }
    }

    fun IsSC(IDD: String, SCType: String): Boolean {
        var tmpID: String
        if (SCType == "Сотрудники") {
            val TextQuery: String =
                "SELECT ID FROM SC838 (nolock) WHERE SP1933='$IDD'"
            val DataTable = ExecuteWithRead(TextQuery)
            return DataTable!!.isNotEmpty()
        }
        if (SCType == "Секции") {
            val TextQuery: String =
                "SELECT ID FROM SC1141 (nolock) WHERE SP1935='$IDD'"
            val DataTable = ExecuteWithRead(TextQuery)
            return DataTable!!.isNotEmpty()
        }
        if (SCType == "Принтеры"){
            val TextQuery: String =
                "SELECT ID FROM SC2459 (nolock) WHERE SP2465='$IDD'"
            val DataTable = ExecuteWithRead(TextQuery)
            return DataTable!!.isNotEmpty()
        }
        return true
    }
    /// <summary>
    /// возвращает список данных элемента справочника по его IDD
    /// </summary>
    /// <param name="IDD"></param>
    /// <param name="SCType"></param>
    /// <param name="FieldList"></param>
    /// <param name="DataMap"></param>
    /// <returns></returns>
    /*
                 fun GetSCData(IDD: String,SCType: String, FieldList: MutableList<String>, DataMap: MutableMap<String,Any>): MutableMap<String, Any>?
                 {
                     return GetSCData(IDD,SCType,FieldList,DataMap,false)
                 }

                 fun GetSCData(IDD: String, SCType: String, ListStr: String, DataMap: MutableMap<String,Any>, ThisID: Boolean)
                 {
                     val FieldList:  MutableList<String>
                     FieldList = helper!!.StringToList(ListStr)
                     for (int i = 0; i < FieldList.Count; i++)
                     {
                         string curr = FieldList[i];
                         if (!ExclusionFields.Contains(curr))
                         {
                             curr = "Спр." + SCType + "." + curr;
                             FieldList.RemoveAt(i);
                             FieldList.Insert(i, curr);
                         }
                     }
                     return GetSCData(IDD, SCType, FieldList,  DataMap, ThisID);
                 }

                 public bool GetDoc(string IDDorID, out Dictionary<string, object> DataDoc, bool ThisID)
                 {
                     DataDoc = new Dictionary<string, object>();
                     if (ThisID)
                     {
                         //Если ID - расширенный, то переведем его в обычный, 9-и символьный
                         if (IDDorID.Length > 9)
                         {
                             IDDorID = IDDorID.Substring(4);
                         }
                     }
                     if (!ExecuteQuery("SELECT IDDOC, IDDOCDEF, DATE_TIME_IDDOC, DOCNO, ISMARK, " + GetSynh("IDD").ToString() +
                                 " FROM _1SJOURN (nolock) WHERE " + (ThisID ? "IDDOC" : GetSynh("IDD")) + "='" + IDDorID + "'"))
                     {
                         return false;
                     }
                     bool result = false;
                     if (MyReader.Read())
                     {
                         DataDoc["ID"] = MyReader[0].ToString();
                         DataDoc["IDDOCDEF"] = To1CName(MyReader[1].ToString());
                         DataDoc["DATE_TIME_IDDOC"] = SQLToDateTime((MyReader[2].ToString()).Substring(0, 14));
                         DataDoc["DOCNO"] = MyReader[3].ToString();
                         DataDoc["ISMARK"] = MyReader[4];
                         DataDoc["IDD"] = MyReader[5];
                         result = true;
                     }
                     else
                     {
                         FExcStr = "Не найден документ!";
                         result = false;
                     }
                     MyReader.Close();
                     return result;
                 } // GetDoc
                 public bool GetDocData(string IDDoc, string DocType, List<string> FieldList, out Dictionary<string, object> DataMap)
                 {
                     bool result = false;
                     DataMap = new Dictionary<string, object>();

                     if (!ExecuteQuery("SELECT " + ToFieldString(FieldList) + " FROM DH" + GetSynh(DocType) + " (nolock) WHERE IDDOC='" + IDDoc + "'"))
                     {
                         return false;
                     }
                     if (MyReader.Read())
                     {
                         for (int i = 0; i < MyReader.FieldCount; i++)
                         {
                             DataMap[FieldList[i]] = MyReader[i];
                         }
                         result = true;
                     }
                     MyReader.Close();
                     return result;
                 } // GetDocData

             }//class SQLSynhronizer
         }
     */
}

