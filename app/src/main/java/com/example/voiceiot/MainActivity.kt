package com.example.voiceiot

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : Activity() {
    private var editText: EditText? = null
    private var btnVoice: Button? = null
    private var btnStop: Button? = null
    private var btnSave: Button? = null
    private var btnTaskList: Button? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestAudioPermission()
        setupUI()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        if (speechRecognizer != null) {
            speechRecognizer!!.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray) {}
                override fun onEndOfSpeech() {
                    isListening = false
                    updateButtonState()
                }

                override fun onError(error: Int) {
                    editText!!.setText("❌ " + getErrorMessage(error))
                    isListening = false
                    updateButtonState()
                }

                override fun onResults(results: Bundle) {
                    if (results != null) {
                        editText!!.setText(
                            results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)!![0]
                        )
                    }
                    isListening = false
                    updateButtonState()
                }

                override fun onPartialResults(partialResults: Bundle) {}
                override fun onEvent(eventType: Int, params: Bundle) {}
            })
        }

        btnVoice!!.setOnClickListener { v: View? ->
            if (!isListening) {
                startListening()
            }
        }

        btnStop!!.setOnClickListener { v: View? ->
            if (isListening) {
                stopListening()
            }
        }

        btnSave!!.setOnClickListener { v: View? -> saveTextToFile() }

        btnTaskList!!.setOnClickListener { v: View? ->
            // Launch FileListActivity when the button is clicked
            val intent = Intent(this@MainActivity, FileListActivity::class.java)
            startActivity(intent)
        }
    }

    private fun requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    private fun setupUI() {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setBackgroundColor(Color.parseColor("#E3F2FD"))
        layout.setPadding(32, 32, 32, 32)

        val titleText = TextView(this)
        titleText.text = "Voice Command App"
        titleText.textSize = 28f
        titleText.setTextColor(Color.parseColor("#1565C0"))
        titleText.typeface = Typeface.DEFAULT_BOLD
        titleText.gravity = Gravity.CENTER
        layout.addView(titleText)

        editText = EditText(this)
        editText!!.hint = "Hasil suara akan muncul di sini"
        editText!!.textSize = 20f
        editText!!.setTextColor(Color.BLACK)
        editText!!.setBackgroundColor(Color.WHITE)
        layout.addView(editText)

        btnVoice = Button(this)
        btnVoice!!.text = "🎤 Start"
        btnVoice!!.setBackgroundColor(Color.parseColor("#4CAF50"))
        layout.addView(btnVoice)

        btnStop = Button(this)
        btnStop!!.text = "⏹ Stop"
        btnStop!!.setBackgroundColor(Color.parseColor("#D32F2F"))
        layout.addView(btnStop)

        btnSave = Button(this)
        btnSave!!.text = "💾 Simpan"
        btnSave!!.setBackgroundColor(Color.parseColor("#FF9800"))
        layout.addView(btnSave)

        btnTaskList = Button(this)
        btnTaskList!!.text = "📋 Daftar Tugas"
        btnTaskList!!.setBackgroundColor(Color.parseColor("#3F51B5"))
        layout.addView(btnTaskList)

        setContentView(layout)
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Mulai berbicara...")
        speechRecognizer!!.startListening(intent)
        isListening = true
        updateButtonState()
    }

    private fun stopListening() {
        speechRecognizer!!.stopListening()
        isListening = false
        updateButtonState()
    }

    private fun saveTextToFile() {val text = editText!!.text.toString()
        if (text.isEmpty()) {
            Toast.makeText(this, "Tidak ada teks untuk disimpan", Toast.LENGTH_SHORT).show()
            return
        }

        // Get the current time and format it as "HHmm_ddMMMyyyy" (HourMinute_DayMonthYear)
        val currentTime = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("HHmm_ddMMMyyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(currentTime))

        // Use the formatted date in the file name
        val fileName = "saved_text_$formattedDate.txt"
        val file = File(filesDir, fileName)

        try {
            FileOutputStream(file, true).use { fos ->
                fos.write((text + "\n").toByteArray())
                Toast.makeText(this, "Teks disimpan sebagai $fileName!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Gagal menyimpan teks", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun updateButtonState() {
        btnVoice!!.text = if (isListening) "⏳ Listening..." else "🎤 Start"
    }

    private fun getErrorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_NETWORK -> "Kesalahan jaringan"
            SpeechRecognizer.ERROR_NO_MATCH -> "Tidak ada hasil yang cocok"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Waktu habis, coba lagi"
            else -> "Terjadi kesalahan"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (speechRecognizer != null) {
            speechRecognizer!!.destroy()
        }
    }
}
