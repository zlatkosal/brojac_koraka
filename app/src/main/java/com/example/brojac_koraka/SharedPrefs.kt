package com.example.brojac_koraka

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences

object SharedPrefs {

    fun saveStepsInPrefs(context: Context, stepsCount: Float) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MySharedPref", MODE_PRIVATE)
        val myEdit: SharedPreferences.Editor = sharedPreferences.edit()
        myEdit.putFloat("steps", stepsCount)
        myEdit.apply()
    }

    fun getStepsFromPrefs(context: Context): Float {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MySharedPref", MODE_PRIVATE)
        return sharedPreferences.getFloat("steps", 0f)
    }
}