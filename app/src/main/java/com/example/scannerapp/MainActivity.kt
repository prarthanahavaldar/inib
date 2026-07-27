package com.example.scannerapp

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.common.detector.MathUtils.distance


import android.media.MediaActionSound
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

import android.graphics.ImageDecoder
import android.graphics.Bitmap

import android.provider.MediaStore
import androidx.activity.result.PickVisualMediaRequest

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var tvResult: TextView
    private lateinit var btnScanAgain: Button

    private lateinit var cameraExecutor: ExecutorService

    private var scanned = false

    private lateinit var btnTorch: ImageButton

    private var camera: Camera? = null
    private var torchEnabled = false

    private lateinit var btnGallery: ImageButton

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->

            uri?.let {
                scanImageFromGallery(it)
            }

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        tvResult = findViewById(R.id.tvResult)
        btnScanAgain = findViewById(R.id.btnScanAgain)
        val scannerLine = findViewById<View>(R.id.scannerLine)
        val scannerFrame = findViewById<View>(R.id.scannerFrame)
        scannerFrame.post {
            val distance = (scannerFrame.height - scannerLine.height).toFloat()

            val animation = ObjectAnimator.ofFloat(
                scannerLine,
                "translationY",
                0f,
                distance
            )

            animation.duration = 2000
            animation.repeatCount = ValueAnimator.INFINITE
            animation.repeatMode = ValueAnimator.REVERSE
            animation.start()

            cameraExecutor = Executors.newSingleThreadExecutor()

            btnScanAgain.setOnClickListener {
                scanned = false
                tvResult.text = "No QR Code Scanned"
                tvResult.visibility = View.GONE
            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startCamera()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
        btnTorch = findViewById(R.id.btnTorch)

        btnTorch.setOnClickListener {

            torchEnabled = !torchEnabled

            camera?.cameraControl?.enableTorch(torchEnabled)

        }
        btnGallery = findViewById(R.id.btnGallery)

        btnGallery.setOnClickListener {

            pickMedia.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )

        }
        tvResult.setOnClickListener {

            val text = tvResult.text.toString()

            if (text.startsWith("http://") || text.startsWith("https://")) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(text)))
            }
        }
    }
    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()

            preview.surfaceProvider = previewView.surfaceProvider

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val scanner = BarcodeScanning.getClient()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->

                val mediaImage = imageProxy.image

                if (mediaImage != null && !scanned) {

                    val image = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )

                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->

                            for (barcode in barcodes) {

                                if (barcode.valueType == Barcode.TYPE_URL ||
                                    barcode.rawValue != null
                                ) {

                                    scanned = true

                                    val result = barcode.rawValue ?: ""

                                    tvResult.text = result
                                    tvResult.visibility = View.VISIBLE
                                    playBeepAndVibrate()


                                    Toast.makeText(
                                        this,
                                        "QR Code Scanned",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    if (result.startsWith("http://") ||
                                        result.startsWith("https://")
                                    ) {

                                        startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(result)
                                            )
                                        )
                                    }

                                    break
                                }
                            }
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }

                } else {
                    imageProxy.close()
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {

                cameraProvider.unbindAll()

               camera= cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    e.message,
                    Toast.LENGTH_LONG
                ).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }
    private fun scanImageFromGallery(uri: Uri) {

        val bitmap: Bitmap =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

                val source =
                    ImageDecoder.createSource(contentResolver, uri)

                ImageDecoder.decodeBitmap(source)

            } else {

                MediaStore.Images.Media.getBitmap(
                    contentResolver,
                    uri
                )

            }

        val image = InputImage.fromBitmap(bitmap, 0)

        val scanner = BarcodeScanning.getClient()

        scanner.process(image)

            .addOnSuccessListener { barcodes ->

                if (barcodes.isEmpty()) {

                    Toast.makeText(
                        this,
                        "No QR Code Found",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addOnSuccessListener
                }

                val result = barcodes[0].rawValue ?: ""

                tvResult.text = result

                if (result.startsWith("http")) {

                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(result)
                        )
                    )

                }

            }

            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Failed to Scan",
                    Toast.LENGTH_SHORT
                ).show()

            }

    }

    private fun playBeepAndVibrate() {

        // Beep
        val sound = MediaActionSound()
        sound.load(MediaActionSound.SHUTTER_CLICK)
        sound.play(MediaActionSound.SHUTTER_CLICK)

        // Vibrate
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            val vibratorManager =
                getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager

            vibratorManager.defaultVibrator.vibrate(
                VibrationEffect.createOneShot(
                    150,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )

        } else {

            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    150,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}