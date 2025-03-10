package com.varun.desitanslator

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
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

    private lateinit var translator : Translator

    private lateinit var mAdView1 :AdView
    private lateinit var mAdView2 : AdView
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        MobileAds.initialize(this@MainActivity)
        mAdView1 = findViewById(R.id.bannerAdView1)
        val adRequest = AdRequest.Builder().build()
        mAdView1.loadAd(adRequest)

        mAdView2 = findViewById(R.id.bannerAdView2)
        val adRequest2 = AdRequest.Builder().build()
        mAdView2.loadAd(adRequest2)
        
        val inputEditText = findViewById<EditText>(R.id.inputEditText)
        val translateBtn = findViewById<Button>(R.id.translateBtn)
        val outputTextView = findViewById<TextView>(R.id.outputText)

        //create an English - Telugu translator
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.TELUGU)
            .build()
        val translator = Translation.getClient(options)

        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                translateBtn.setOnClickListener {
                    if(inputEditText.text.toString().isEmpty()) {
                        Snackbar.make(inputEditText, "Please Enter the Text", Snackbar.LENGTH_SHORT)
                            .show()
                        return@setOnClickListener
                    }

                    //  Change button text to "↓" (Down Arrow) after flip
                    translateBtn.text = "↓"

                    //  Flip Animation (Y-axis Rotation for Visible Effect)
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
                                }
                            })
                            resetAnim.start()
                        }
                    })

                    flipAnim.start() // Start animation

                    translator.translate(inputEditText.text.toString())
                        .addOnSuccessListener { translatedText ->
                            outputTextView.text = translatedText
                        }

                    // Re-enable button when user types in EditText
                    inputEditText.addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(s: Editable?) {
                            translateBtn.isEnabled = s?.isNotEmpty() == true // Enable if text is entered
                        }

                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    })
            } }
            .addOnFailureListener{exception ->
                outputTextView.text = exception.message
            }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        translator.close()
    }
}