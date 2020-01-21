package com.intek.wpma.Helpers

class Translation {
    /// <summary>
    /// возведения целого положительного числа в целую положительную степень
    /// </summary>
    /// <param name="base"></param>
    /// <param name="exp"></param>
    /// <returns></returns>
    /*
    static public long Power(int @base, int exp)
    {
        if (exp == 0) return 1;
        long result = 1;
        for (int i = 1; i <= exp; i++)
        {
            result *= @base;
        }
        return result;
    }

    /// <summary>
    /// Переводит числа из 36-ричной системы в десятиричную
    /// Корректно работает только с прописными латинскими буквами!
    /// </summary>
    /// <param name="Number"></param>
    /// <returns></returns>
    static public long _36ToDec(string Number)
    {
        long result = 0;
        int num;
        for (int i = 0; i < Number.Length; i++)
        {
            char ch = Convert.ToChar(Number.Substring(Number.Length - i - 1, 1));
            if (Char.IsNumber(ch))
            {
                num = (int)ch - 48; //у 0-код 48
            }
            else
            {
                num = (int)ch - 55; //у A-код 65
            }
            result += num * Power(36, i);
        }
        return result;
    }

    /// <summary>
    ///
    /// </summary>
    /// <param name="Number"></param>
    /// <returns></returns>
    static public long _2ToDec(string Number)
    {
        long result = 0;
        for (int i = 0; i < Number.Length; ++i)
        {
            result  += Convert.ToInt32(Number.Substring(Number.Length - i - 1, 1)) * Power(2, i);
        }
        return result;
    }

    /// <summary>
    ///
    /// </summary>
    /// <param name="Number"></param>
    /// <returns></returns>

    fun DecTo2(Number: Long): String
    {
        var result = ""

        //определим наибольший делитель (с чего начать)
        var div: Long = 1
        while ((Number / div) >= 2)
        {
            div *= 2
        }

        //преобразование
        while (div > 0)
        {
            var d: Long = (Number / div)    //целая часть от деления
            if (d < 10)
            {
                result += d.toString()
            }
            else
            {
                result += (d as Char).toString()
            }
            Number %= div  //остаток от деления
            div /= 2      //уменьшаем делитель
        }
        return result
    }

    /// <summary>
    ///
    /// </summary>
    /// <param name="Number"></param>
    /// <returns></returns>
    static public string DecTo36(long Number)
    {
        string result = "";

        //определим наибольший делитель (с чего начать)
        long div = 1;
        while ((long)(Number / div) >= 36)
        {
            div *= 36;
        }

        //преобразование
        while (div > 0)
        {
            long d = (long)(Number / div);    //целая часть от деления
            if (d < 10)
            {
                result += d.ToString();
            }
            else
            {
                result += ((char)(d + 55)).ToString();
            }
            Number %= div;  //остаток от деления
            div /= 36;      //уменьшаем делитель
        }
        return result;
    }

    /// <summary>
    ///
    /// </summary>
    /// <param name="Number"></param>
    /// <returns></returns>
    static public string DecTo36(string Number)
    {
        return DecTo36(Convert.ToInt64(Number));
    }
    */

}