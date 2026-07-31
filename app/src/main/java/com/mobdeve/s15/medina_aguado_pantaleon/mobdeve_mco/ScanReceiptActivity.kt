package com.mobdeve.s15.medina_aguado_pantaleon.mobdeve_mco

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class ScanReceiptActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var tvScanStatus: TextView
    private var imageCapture: ImageCapture? = null

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera()
            } else {
                tvScanStatus.text = "Camera permission is needed to scan receipts."
                Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show()
            }
        }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                openOcrProcessing(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scan_receipt_activity)

        previewView = findViewById(R.id.previewView)
        tvScanStatus = findViewById(R.id.tvScanStatus)

        findViewById<Button>(R.id.btnCaptureReceipt).setOnClickListener {
            captureReceipt()
        }

        findViewById<Button>(R.id.btnUploadReceipt).setOnClickListener {
            pickImage.launch("image/*")
        }

        if (hasCameraPermission()) {
            startCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
                tvScanStatus.text = "Place the receipt inside the frame, then capture or upload an image."
            } catch (exception: Exception) {
                tvScanStatus.text = "Unable to start camera preview."
                Toast.makeText(this, "Camera failed to start.", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureReceipt() {
        val capture = imageCapture
        if (capture == null) {
            Toast.makeText(this, "Camera is still starting.", Toast.LENGTH_SHORT).show()
            return
        }

        tvScanStatus.text = "Capturing receipt..."

        val photoFile = File(
            cacheDir,
            "receipt_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    openOcrProcessing(Uri.fromFile(photoFile))
                }

                override fun onError(exception: ImageCaptureException) {
                    tvScanStatus.text = "Capture failed. Please try again."
                    Toast.makeText(this@ScanReceiptActivity, "Could not capture receipt.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun openOcrProcessing(uri: Uri) {
        val intent = Intent(this, OcrProcessingActivity::class.java).apply {
            putExtra(OcrProcessingActivity.EXTRA_IMAGE_URI, uri.toString())
        }
        startActivity(intent)
    }

    companion object {
    }
}
