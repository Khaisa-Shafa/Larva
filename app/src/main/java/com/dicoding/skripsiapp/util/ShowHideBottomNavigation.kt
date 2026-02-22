package com.dicoding.skripsiapp.util

import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.activity.MainActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

fun Fragment.hideBottomNavigationView() {
    val bottomNavigationView =
        (activity as? MainActivity)?.findViewById<BottomNavigationView>(R.id.bottomNavigation)

    if (bottomNavigationView == null) {
        Log.e("BottomNavigationView", "BottomNavigationView not found")
    } else {
        bottomNavigationView.visibility = View.GONE
    }
}

fun Fragment.showBottomNavigationView() {
    val bottomNavigationView =
        (activity as? MainActivity)?.findViewById<BottomNavigationView>(R.id.bottomNavigation)

    if (bottomNavigationView == null) {
        Log.e("BottomNavigationView", "BottomNavigationView not found")
    } else {
        bottomNavigationView.visibility = View.VISIBLE
    }
}