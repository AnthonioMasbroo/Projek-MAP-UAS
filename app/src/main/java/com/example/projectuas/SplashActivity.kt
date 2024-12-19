package com.example.projectuas

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity


class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set full screen dengan menangani notch
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        setContentView(R.layout.activity_splash)

        val splashImage: ImageView = findViewById(R.id.splash_image)
        adjustImageAspectRatio(splashImage)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 3000)
    }

    private fun adjustImageAspectRatio(imageView: ImageView) {
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val screenWidth = displayMetrics.widthPixels

        // Sesuaikan ukuran gambar berdasarkan aspek ratio layar
        imageView.post {
            val drawable = imageView.drawable
            if (drawable != null) {
                val imageRatio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight.toFloat()
                val screenRatio = screenWidth.toFloat() / screenHeight.toFloat()

                if (imageRatio > screenRatio) {
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                } else {
                    imageView.scaleType = ImageView.ScaleType.FIT_XY
                }
            }
        }
    }
}
