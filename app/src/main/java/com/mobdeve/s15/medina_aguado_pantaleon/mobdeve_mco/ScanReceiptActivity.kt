package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ScanReceiptActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scan_receipt_activity)

        val btnCaptureReceipt = findViewById<Button>(R.id.btnCaptureReceipt)

        btnCaptureReceipt.setOnClickListener {
            // TODO: Replace this mock capture flow with CameraX and OCR extraction.
            val intent = Intent(this, ReviewReceiptActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnUploadReceipt).setOnClickListener {
            Toast.makeText(this, "TODO: Add gallery picker and pass selected image to OCR.", Toast.LENGTH_SHORT).show()
        }
    }
}
