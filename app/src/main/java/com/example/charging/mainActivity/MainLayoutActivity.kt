package com.example.charging.mainActivity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.charging.R
import com.example.charging.cityChoice.activity.city_choice


class MainLayoutActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)
        getSupportActionBar()?.hide();
        val city_button: Button=findViewById(R.id.city)

        city_button.setOnClickListener {
            val intent=Intent(this,city_choice::class.java)
            startActivity(intent)
            /*val intent=Intent(this,.cityChoice.city_choice::class.java)
            startActivity(intent)*/
        }

    }


}