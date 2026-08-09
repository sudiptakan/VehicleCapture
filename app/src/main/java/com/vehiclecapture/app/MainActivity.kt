package com.vehiclecapture.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var preview: PreviewView
    private lateinit var plate: EditText
    private lateinit var status: TextView
    private var imageCapture: ImageCapture? = null
    private val records = mutableListOf<Triple<String,String,String>>()
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        preview=findViewById(R.id.preview); plate=findViewById(R.id.plate); status=findViewById(R.id.status)
        findViewById<Button>(R.id.capture).setOnClickListener { captureAndRead() }
        findViewById<Button>(R.id.save).setOnClickListener { saveVehicle() }
        findViewById<Button>(R.id.export).setOnClickListener { exportExcel() }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera()
        else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
    }
    override fun onRequestPermissionsResult(r:Int,p:Array<String>,g:IntArray){ super.onRequestPermissionsResult(r,p,g); if(r==10 && g.isNotEmpty() && g[0]==PackageManager.PERMISSION_GRANTED) startCamera() else status.text="Camera permission is required." }

    private fun startCamera(){
        val future=ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider=future.get()
            val previewUse=Preview.Builder().build().also{it.setSurfaceProvider(preview.surfaceProvider)}
            imageCapture=ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
            val selector=CameraSelector.DEFAULT_BACK_CAMERA
            provider.unbindAll(); provider.bindToLifecycle(this,selector,previewUse,imageCapture)
            status.text="Rear camera ready. Point at the registration plate."
        },ContextCompat.getMainExecutor(this))
    }
    private fun captureAndRead(){
        val capture=imageCapture ?: return
        val file=File.createTempFile("plate_",".jpg",cacheDir)
        capture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(),ContextCompat.getMainExecutor(this),
            object: ImageCapture.OnImageSavedCallback{
                override fun onImageSaved(o:ImageCapture.OutputFileResults){ 
                    status.text="Reading vehicle number…"
                    val img=InputImage.fromFilePath(this@MainActivity,android.net.Uri.fromFile(file))
                    recognizer.process(img).addOnSuccessListener{ result ->
                        var s=result.text.uppercase(Locale.US).replace(Regex("[^A-Z0-9]"),"")
                        if(s.startsWith("INDIA")) s=s.removePrefix("INDIA")
                        plate.setText(s); status.text="OCR complete. Verify the number, then save."
                    }.addOnFailureListener{status.text="OCR failed. Type the number manually."}
                }
                override fun onError(e:ImageCaptureException){status.text="Capture failed: ${e.message}"}
            })
    }
    private fun saveVehicle(){
        val v=plate.text.toString().uppercase(Locale.US).replace(Regex("[^A-Z0-9]"),"")
        if(v.isEmpty()){Toast.makeText(this,"Enter vehicle number",Toast.LENGTH_SHORT).show();return}
        val now=Date(); val d=SimpleDateFormat("dd-MM-yyyy",Locale.US).format(now); val t=SimpleDateFormat("HH:mm:ss",Locale.US).format(now)
        records.add(Triple(d,t,v)); plate.setText(""); status.text="Saved: $v"; Toast.makeText(this,"Vehicle saved",Toast.LENGTH_SHORT).show()
    }
    private fun exportExcel(){
        if(records.isEmpty()){Toast.makeText(this,"No records to export",Toast.LENGTH_SHORT).show();return}
        try{
            val wb=XSSFWorkbook(); val sh=wb.createSheet("Vehicle Data")
            val head=sh.createRow(0); listOf("SL No","Date","Time","Vehicle Registration Number").forEachIndexed{ i,s->head.createCell(i).setCellValue(s)}
            records.forEachIndexed{idx,r-> val row=sh.createRow(idx+1); row.createCell(0).setCellValue((idx+1).toDouble()); row.createCell(1).setCellValue(r.first); row.createCell(2).setCellValue(r.second); row.createCell(3).setCellValue(r.third)}
            val dir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val f=File(dir,"Vehicle_Registration_Data_${SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date())}.xlsx")
            FileOutputStreamCompat.write(wb,f); wb.close()
            Toast.makeText(this,"Excel saved in Downloads",Toast.LENGTH_LONG).show()
        }catch(e:Exception){Toast.makeText(this,"Export failed: ${e.message}",Toast.LENGTH_LONG).show()}
    }
}
object FileOutputStreamCompat {
    fun write(wb:XSSFWorkbook,f:File){ java.io.FileOutputStream(f).use{wb.write(it)} }
}
