package com.giuseppesorce.scanml

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.otaliastudios.cameraview.CameraListener
import kotlinx.android.synthetic.main.activity_main.*

class BarcodeScannerActivity : AppCompatActivity() {


    val sheetBehavior: BottomSheetBehavior<LinearLayout> by lazy {
        BottomSheetBehavior.from(layout_bottom_sheet)
    }

    var timer: CountDownTimer? = null

    private val qrList = arrayListOf<QrCode>()
    val adapter = QrCodeAdapter(qrList)

    val CAMERA_SHOOT: Int = 1
    val CAMERA_SCAN: Int = 2
    var CAMERA_ACTION: Int = CAMERA_SCAN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)



        cameraView.setLifecycleOwner(this)

        sheetBehavior.peekHeight = 224
        sheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {}
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
        rvQrCode.layoutManager = LinearLayoutManager(this)
        rvQrCode.adapter = adapter

        cameraView.addCameraListener(object : CameraListener() {
            override fun onPictureTaken(jpeg: ByteArray?) {
                val bitmap = jpeg?.size?.let { BitmapFactory.decodeByteArray(jpeg, 0, it) }
                bitmap?.let {
                    when (CAMERA_ACTION) {
                        CAMERA_SCAN -> runBarcodeScanner(it)
                        CAMERA_SHOOT -> {
                            timer?.cancel()
                            imagePreview.setImageBitmap(bitmap)

                        }
                        else -> {

                        }
                    }


                }

            }

        })



        timer = object : CountDownTimer(500000, 700) {

            override fun onTick(millisUntilFinished: Long) {
                cameraView.captureSnapshot()
            }

            override fun onFinish() {
                try {

                } catch (e: Exception) {
                    Log.e("Error", "Error: $e")
                }

            }
        }
        timer?.start()
        fab_take_photo.setOnClickListener {

            when (CAMERA_ACTION) {
                CAMERA_SHOOT -> {
                    imagePreview.setImageBitmap(null)
                    CAMERA_ACTION = CAMERA_SCAN
                    fab_take_photo.alpha = 1.0f
                    fab_take_photo.scaleX = 1.0f
                    fab_take_photo.scaleY = 1.0f
                    timer?.start()
                }
                else -> {
                    CAMERA_ACTION = CAMERA_SHOOT
                    timer?.cancel()
                    fab_take_photo.alpha = 0.9f
                    fab_take_photo.scaleX = 0.8f
                    fab_take_photo.scaleY = 0.8f
                    cameraView.captureSnapshot()
                }
            }
        }


    }

    private fun runBarcodeScanner(bitmap: Bitmap) {
        //Create a FirebaseVisionImage
        val image = FirebaseVisionImage.fromBitmap(bitmap)

        //Optional : Define what kind of barcodes you want to scan
        val options = FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(
                //Detect all kind of barcodes
                FirebaseVisionBarcode.FORMAT_ALL_FORMATS,
                FirebaseVisionBarcode.FORMAT_QR_CODE

            )
            .build()

        //Get access to an instance of FirebaseBarcodeDetector
        val detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options)

        //Use the detector to detect the labels inside the image
        detector.detectInImage(image)
            .addOnSuccessListener {
                qrList.clear()
                adapter.notifyDataSetChanged()
                // Task completed successfully
                for (firebaseBarcode in it) {
                    when (firebaseBarcode.valueType) {
                        //Handle the URL here
                        FirebaseVisionBarcode.TYPE_URL ->
                            qrList.add(QrCode("URL", firebaseBarcode.displayValue))
                        // Handle the contact info here, i.e. address, name, phone, etc.
                        FirebaseVisionBarcode.TYPE_CONTACT_INFO ->
                            qrList.add(QrCode("Contact", firebaseBarcode.contactInfo?.title))
                        // Handle the wifi here, i.e. firebaseBarcode.wifi.ssid, etc.
                        FirebaseVisionBarcode.TYPE_WIFI ->
                            qrList.add(QrCode("WiFi", firebaseBarcode.wifi?.ssid))
                        // Handle the driver license barcode here, i.e. City, Name, Expiry, etc.
                        FirebaseVisionBarcode.TYPE_DRIVER_LICENSE ->
                            qrList.add(QrCode("Driver License", firebaseBarcode.driverLicense?.licenseNumber))
                        //Handle more types
                        else ->
                            qrList.add(QrCode("Generic", firebaseBarcode.displayValue))
                        //None of the above type was detected, so extract the value from the barcode
                    }
                }
                adapter.notifyDataSetChanged()
                sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED)
            }
            .addOnFailureListener {
                // Task failed with an exception

                Toast.makeText(baseContext, "Sorry, something went wrong!", Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener {

            }
    }


}