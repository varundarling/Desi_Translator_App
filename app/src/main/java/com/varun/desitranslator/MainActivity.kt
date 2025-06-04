package com.varun.desitranslator

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

class MainActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")

    private lateinit var translator: Translator
    private lateinit var mAdView1: AdView
//    private lateinit var mAdView2: AdView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)  // ✅ Enables manual inset handling
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }

        setContentView(R.layout.activity_main)

        MobileAds.initialize(this@MainActivity)
        mAdView1 = findViewById(R.id.bannerAdView1)
        val adRequest = AdRequest.Builder().build()
        mAdView1.loadAd(adRequest)

//        mAdView2 = findViewById(R.id.bannerAdView2)
//        val adRequest2 = AdRequest.Builder().build()
//        mAdView2.loadAd(adRequest2)

        val inputEditText = findViewById<EditText>(R.id.inputEditText)
        val translateBtn = findViewById<Button>(R.id.translateBtn)
        val outputTextView = findViewById<TextView>(R.id.outputTextView2)

        // Create an English - Telugu translator
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.TELUGU)
            .build()

        translator = Translation.getClient(options)

        // Check if connected to Wi-Fi, else allow mobile data
        val conditions = if (isConnectedToWifi(this)) {
            DownloadConditions.Builder()
                .requireWifi()  // Prioritize Wi-Fi
                .build()
        } else {
            DownloadConditions.Builder()
                .build()  // Allow mobile data as fallback
        }

        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                translateBtn.setOnClickListener {
                    val textToTranslate = inputEditText.text.toString()
                    if (textToTranslate.isEmpty()) {
                        Snackbar.make(inputEditText, "Please Enter the Text", Snackbar.LENGTH_SHORT)
                            .show()
                        return@setOnClickListener
                    }
                    // Change button text to "↓" (Down Arrow) after flip
                    translateBtn.text = "↓"

                    // Flip Animation (Y-axis Rotation for Visible Effect)
                    val flipAnim = ObjectAnimator.ofFloat(translateBtn, "rotationX", 0f, 360f)
                    flipAnim.duration = 350 // Make animation slow enough to be visible
                    flipAnim.interpolator = DecelerateInterpolator()

                    flipAnim.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            // Flip back to 0° so it doesn’t look stuck upside down
                            val resetAnim = ObjectAnimator.ofFloat(translateBtn, "rotationX", 360f, 360f)
                            resetAnim.duration = 700
                            resetAnim.interpolator = DecelerateInterpolator()
                            resetAnim.addListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    // Make button un clickable after animation
                                    translateBtn.isEnabled = false
                                    translateText(textToTranslate, outputTextView, translateBtn) // ✅ Modified here
                                }
                            })
                            resetAnim.start()
                        }
                    })

                    flipAnim.start() // Start animation



                    // Re-enable button when user types in EditText
                    inputEditText.addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(s: Editable?) {
                            translateBtn.isEnabled = s?.isNotEmpty() == true // Enable if text is entered
                        }

                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    })
                }
            }
            .addOnFailureListener { exception ->
                outputTextView.text = exception.message
            }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }


    @SuppressLint("SetTextI18n")
    private fun translateText(inputText: String, outputTextView: TextView, translateBtn: Button) {
        translator.translate(inputText)
            .addOnSuccessListener { translatedText ->
                outputTextView.text = translatedText
                translateBtn.text = "Translate"
            }
            .addOnFailureListener { exception ->
                outputTextView.text = exception.message
                translateBtn.text = "Translate"
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        translator.close()
    }

    // Function to check if connected to Wi-Fi
    private fun isConnectedToWifi(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
