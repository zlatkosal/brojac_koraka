package com.example.brojac_koraka

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import android.view.View

class MainActivity : AppCompatActivity() {

    private lateinit var btn:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn = findViewById(R.id.button)

        btn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val i  = Intent(this,LoginActivity::class.java)
            startActivity(i)

        }
    }

    fun loadFragmentDay(view: View) {
        loadFragment(FragmentDay())
    }

    fun loadFragmentWeek(view: View) {
        loadFragment(FragmentWeek())
    }

    fun loadFragmentMounth(view: View) {
        loadFragment(FragmentMounth())
    }

    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}