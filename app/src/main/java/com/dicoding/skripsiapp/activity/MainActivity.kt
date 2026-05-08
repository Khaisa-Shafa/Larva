package com.dicoding.skripsiapp.activity

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.dicoding.skripsiapp.R
import com.dicoding.skripsiapp.databinding.ActivityMainBinding
import com.dicoding.skripsiapp.ml.YoloSegHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import android.util.Log
import com.dicoding.skripsiapp.util.Constants
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomNavigation: BottomNavigationView

    private var isDialogShowing = false

    private var lastClickTime: Long = 0
    private val clickDebounceTime = 500L // Waktu debounce dalam milidetik


    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        FirebaseApp.initializeApp(this)
            
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainHostFragment)) { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, 0) // Tambahkan inset atas
            insets
        }

        bottomNavigation = findViewById(R.id.bottomNavigation)


        val navController = findNavController(R.id.mainHostFragment)
        bottomNavigation.setupWithNavController(navController)

        // Set ikon pertama kali diatur ke "filled"
        bottomNavigation.selectedItemId = R.id.homeFragment
        menuItemIconSelected(R.id.homeFragment)

        // Navigasi ke homeFragment secara eksplisit saat pertama kali
        navController.navigate(R.id.homeFragment)

        // Menambahkan listener untuk BottomNavigationView
        setupBottomNavigation(navController)

        // Callback untuk tombol back
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentDestination = navController.currentDestination?.id
                when (currentDestination) {
                    R.id.profileFragment -> {
                        navController.popBackStack(R.id.homeFragment, false)
                    }

                    R.id.homeFragment -> {
                        finish()
                    }

                    else -> {
                        navController.popBackStack()
                    }
                }
            }
        })
    }

    private fun showButtonDialog() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < clickDebounceTime) return
        lastClickTime = currentTime
        isDialogShowing = true

        // Langsung tampilkan Choose Model dulu
        showModelDialog()
    }

    private fun showModelDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottomsheet_layout_2)

        val linearBottomSheet = dialog.findViewById<LinearLayout>(R.id.linearBottomSheet)
        val cancelButton = dialog.findViewById<ImageView>(R.id.cancelButton)
        val layoutModel1 = dialog.findViewById<LinearLayout>(R.id.layoutUploadImageDetection)
        val layoutModel2 = dialog.findViewById<LinearLayout>(R.id.layoutLiveDetection)

        val gestureDetector =
            GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (e1 != null) {
                        val deltaY = e2.y - e1.y
                        val deltaX = e2.x - e1.x
                        if (deltaY > 100 && Math.abs(deltaX) < 100 && velocityY > 500) {
                            dialog.dismiss()
                            return true
                        }
                    }
                    return false
                }
            })

        linearBottomSheet?.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        layoutModel1.setOnClickListener {
            dialog.dismiss()
            showActionDialog(Constants.MODEL_YOLOV8_MOBILENETV3)
        }

        layoutModel2.setOnClickListener {
            dialog.dismiss()
            showActionDialog(Constants.MODEL_YOLO11_MOBILEVIT)
        }

        dialog.setOnDismissListener { isDialogShowing = false }

        dialog.show()
        dialog.window?.let { window ->
            window.setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.attributes.windowAnimations = R.style.DialogAnimation
            window.setGravity(Gravity.BOTTOM)
        }
    }

    private fun showActionDialog(selectedModel: Int) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottomsheet_layout)

        val linearBottomSheet = dialog.findViewById<LinearLayout>(R.id.linearBottomSheet)
        val cancelButton = dialog.findViewById<ImageView>(R.id.cancelButton)
        val layoutUploadImage = dialog.findViewById<LinearLayout>(R.id.layoutUploadImageDetection)
        val layoutLiveDetection = dialog.findViewById<LinearLayout>(R.id.layoutLiveDetection)

        val gestureDetector =
            GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (e1 != null) {
                        val deltaY = e2.y - e1.y
                        val deltaX = e2.x - e1.x
                        if (deltaY > 100 && Math.abs(deltaX) < 100 && velocityY > 500) {
                            dialog.dismiss()
                            return true
                        }
                    }
                    return false
                }
            })

        linearBottomSheet?.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        // Upload Image → bawa selectedModel ke PageDetectionFragment via Bundle
        layoutUploadImage.setOnClickListener {
            dialog.dismiss()
            val bundle = Bundle().apply {
                putInt("selected_model", selectedModel)
            }
            findNavController(R.id.mainHostFragment).navigate(R.id.pageDetectionFragment, bundle)
        }

        // Live Detection → bawa selectedModel ke LiveDetectionActivity via Intent
        layoutLiveDetection.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, LiveDetectionActivity::class.java)
            intent.putExtra("selected_model", selectedModel)
            startActivity(intent)
        }

        dialog.setOnDismissListener { isDialogShowing = false }

        dialog.show()
        dialog.window?.let { window ->
            window.setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.attributes.windowAnimations = R.style.DialogAnimation
            window.setGravity(Gravity.BOTTOM)
        }
    }

    private fun setupBottomNavigation(navController: NavController) {
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            resetIconsToDefault() // Reset ikon
            menuItemIconSelected(menuItem.itemId) // Set ikon fragment aktif

            if (menuItem.itemId == R.id.addFragment) {
                showButtonDialog()
                true // Izinkan navigasi
            } else {
                // Menggunakan NavOptions untuk membersihkan back stack
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(navController.graph.startDestinationId, false)
                    .build()
                navController.navigate(menuItem.itemId, null, navOptions)
                true
            }
        }

        // Listener untuk perubahan fragment
        navController.addOnDestinationChangedListener { _, destination, _ ->
            resetIconsToDefault()
            when (destination.id) {
                R.id.homeFragment -> menuItemIconSelected(R.id.homeFragment)
                R.id.favoriteFragment -> menuItemIconSelected(R.id.favoriteFragment)
                R.id.bookmarkedFragment -> menuItemIconSelected(R.id.bookmarkedFragment)
                R.id.profileFragment,
                R.id.aboutFragment,
                R.id.editProfileFragment,
                R.id.settingsFragment -> menuItemIconSelected(R.id.profileFragment)
            }
        }
    }

    private fun menuItemIconSelected(itemId: Int) {
        when (itemId) {
            R.id.homeFragment -> bottomNavigation.menu.findItem(R.id.homeFragment).icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_home_filled, theme)
            R.id.favoriteFragment -> bottomNavigation.menu.findItem(R.id.favoriteFragment).icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_favorite_filled, theme)
            R.id.bookmarkedFragment -> bottomNavigation.menu.findItem(R.id.bookmarkedFragment).icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_bookmarked_filled, theme)
            R.id.profileFragment -> bottomNavigation.menu.findItem(R.id.profileFragment).icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_profile_filled, theme)
        }
    }

    private fun resetIconsToDefault() {
        bottomNavigation.menu.findItem(R.id.homeFragment).icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_home, theme)
        bottomNavigation.menu.findItem(R.id.favoriteFragment).icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_favorite, theme)
        bottomNavigation.menu.findItem(R.id.bookmarkedFragment).icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_bookmarked, theme)
        bottomNavigation.menu.findItem(R.id.profileFragment).icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_profile, theme)
    }
}
