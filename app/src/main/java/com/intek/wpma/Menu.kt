package com.intek.wpma

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import com.intek.wpma.ChoiseWork.Set.SetInitialization
import kotlinx.android.synthetic.main.activity_menu.*

class Menu : AppCompatActivity() {

    var Employer: String = ""
    var EmployerFlags: String = ""
    var EmployerIDD: String = ""
    var EmployerID: String = ""
    var ParentForm: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        Employer = intent.extras!!.getString("Employer")!!
        EmployerFlags = intent.extras!!.getString("EmployerFlags")!!
        EmployerIDD = intent.extras!!.getString("EmployerIDD")!!
        EmployerID = intent.extras!!.getString("EmployerID")!!
        ParentForm = intent.extras!!.getString("ParentForm")!!

        employer.text = Employer
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        if (keyCode.toString() == "7")
        {
            //нажали 0
            startActivity(7 )
        }
        return super.onKeyDown(keyCode, event)
    }
    fun startActivity(num: Int) {
        var intent: Intent

        if (num == 7) {     // режим отбора
            intent = Intent(this, SetInitialization::class.java)
            intent.putExtra("Employer", Employer)
            intent.putExtra("EmployerIDD",EmployerIDD)
            intent.putExtra("EmployerFlags",EmployerFlags)
            intent.putExtra("EmployerID",EmployerID)
            intent.putExtra("ParentForm","Menu")
            startActivity(intent)
        }
    }
}
