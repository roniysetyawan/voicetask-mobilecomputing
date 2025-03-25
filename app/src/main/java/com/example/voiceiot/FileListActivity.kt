package com.example.voiceiot

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.LinearLayout
import android.webkit.MimeTypeMap
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class FileListActivity : Activity() {
    private lateinit var fileListView: ListView
    private lateinit var noFilesMessage: TextView
    private lateinit var fileList: ArrayList<String>

    private val REQUEST_CODE_PERMISSION = 100
    private val REQUEST_CODE_OPEN_DOCUMENT = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request permissions if needed
        checkPermissions()

        // Create the main layout programmatically
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(16, 16, 16, 16)

        // Create and configure the "No files available" message
        noFilesMessage = TextView(this)
        noFilesMessage.text = "No files available"
        noFilesMessage.textSize = 18f
        noFilesMessage.setTextColor(0xFFFF0000.toInt())  // Red color
        noFilesMessage.gravity = Gravity.CENTER
        noFilesMessage.visibility = TextView.GONE  // Initially hidden
        layout.addView(noFilesMessage)

        // Create and configure the ListView to display the files
        fileListView = ListView(this)
        layout.addView(fileListView)

        // Set the layout as the content view for this activity
        setContentView(layout)

        // Initialize the list to store the file names
        fileList = ArrayList()

        // Load files and update UI
        loadFiles()

        // Add item click listener to open the file
        fileListView.setOnItemClickListener { _, _, position, _ ->
            val fileName = fileList[position]
            openFile(fileName)
        }
    }

    // Check and request permissions
    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE_PERMISSION)
        } else {
            // Permission is already granted, load files
            loadFiles()
        }
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, reload files
                loadFiles()
            } else {
                noFilesMessage.text = "Permission denied to read storage"
                noFilesMessage.visibility = TextView.VISIBLE
            }
        }
    }

    // Load files from internal or external storage
    private fun loadFiles() {
        val directory: File = filesDir  // Internal storage
        val files = directory.listFiles()

        if (files != null && files.isNotEmpty()) {
            for (file in files) {
                if (file.isFile) {
                    fileList.add(file.name)
                }
            }

            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, fileList)
            fileListView.adapter = adapter
            noFilesMessage.visibility = TextView.GONE  // Hide "No files" message
        } else {
            noFilesMessage.text = "No files found"
            noFilesMessage.visibility = TextView.VISIBLE  // Show "No files" message
        }
    }

    // Get MIME type based on file extension
    private fun getMimeType(file: File): String {
        val extension = file.extension
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"  // Default to */* if not recognized
    }

    // Open the file based on MIME type
    private fun openFile(fileName: String) {
        val directory: File = filesDir  // Internal storage
        val file = File(directory, fileName)

        if (file.exists()) {
            try {
                val mimeType = getMimeType(file)
                val uri: Uri = Uri.fromFile(file)

                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(uri, mimeType)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                noFilesMessage.text = "Unable to open file: ${e.message}"
                noFilesMessage.visibility = TextView.VISIBLE
                Log.e("FileListActivity", "Error opening file: ${e.message}")
            }
        } else {
            noFilesMessage.text = "File not found"
            noFilesMessage.visibility = TextView.VISIBLE
        }
    }

    // Open document picker for Android 10 and above
    private fun openDocument() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"  // Adjust MIME type as needed (e.g., "image/*", "application/pdf")
        startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT)
    }

    // Handle the result of the document picker
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OPEN_DOCUMENT && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                // Now you have the URI of the selected file
                openFileFromUri(uri)
            }
        }
    }

    // Open the selected file from URI
    private fun openFileFromUri(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, contentResolver.getType(uri))  // Use the correct MIME type
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
}
