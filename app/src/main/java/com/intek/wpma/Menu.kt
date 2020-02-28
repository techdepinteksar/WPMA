package com.intek.wpma

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import com.intek.wpma.ChoiseWork.Set.SetInitialization
import kotlinx.android.synthetic.main.activity_menu.*
import kotlinx.android.synthetic.main.activity_menu.terminalView
import kotlinx.android.synthetic.main.activity_watch_table_part.*

class Menu : BarcodeDataReceiver() {

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
        terminalView.text = intent.extras!!.getString("terminalView")!!

        title = Employer
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
            val SetInit = Intent(this, SetInitialization::class.java)
            SetInit.putExtra("Employer", Employer)
            SetInit.putExtra("EmployerIDD",EmployerIDD)
            SetInit.putExtra("EmployerFlags",EmployerFlags)
            SetInit.putExtra("EmployerID",EmployerID)
            SetInit.putExtra("terminalView",terminalView.text.trim())
            SetInit.putExtra("ParentForm","Menu")
            startActivity(SetInit)
        }
    }
}
