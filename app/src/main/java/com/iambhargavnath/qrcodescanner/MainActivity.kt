package com.iambhargavnath.qrcodescanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var previewView: PreviewView
    private lateinit var qrCodeText: TextView

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.preview_view)
        qrCodeText = findViewById(R.id.qr_code_text)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        viewModel.qrCodeText.observe(this) { text ->
            qrCodeText.text = text
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val barcodeScannerOptions = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()

            val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions)

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetRotation(previewView.display.rotation)
                .build()
                .also {
                    it.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        processImageProxy(barcodeScanner, imageProxy)
                    }
                }

            try {
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (e: Exception) {
                Log.e("MainActivity", "Camera setup failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(barcodeScanner: BarcodeScanner, imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        viewModel.updateQRCodeText(barcode.displayValue ?: "No QR code detected")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "QR code scanning failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }
}