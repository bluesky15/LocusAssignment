package com.lkb.locusassignment

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.lkb.locusassignment.databinding.ActivityMainBinding
import okio.IOException
import okio.buffer
import okio.source
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.reflect.Type
import java.nio.charset.Charset
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    var currentIndex = 0
    var REQ_CAMERA = 100
    var fileSize = 0
    var imageFilePath: String? = null
    var encodedImage: String? = null
    var timeStamp: String? = null
    var imageName: String? = null
    var imageSize: String? = null
    var fileDirectoty: File? = null
    var imageFilename: File? = null
    var numberFormat: NumberFormat? = null
    lateinit var imageBytes: ByteArray
    private lateinit var binding: ActivityMainBinding
    private var pageData: List<PageItem> = listOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val data = readJsonFromAssets(
            this@MainActivity,
            "data.json"
        )
        val reviewType: Type? = object : TypeToken<List<PageItem>?>() {}.type
        if (data != null) {
            pageData = Gson().fromJson(data, reviewType)
        }

        with(binding.rvHomePage) {
            setHasFixedSize(true)
            adapter = NestedAdapter(pageData) { s, i ->
                currentIndex = i
                showPictureDialog()
            }
            val manager = LinearLayoutManager(context)
            layoutManager = manager
        }

        val verifyPermission: Int = Build.VERSION.SDK_INT
        if (verifyPermission > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (checkPermission()) {
                requestPermission()
            }
        }
    }

    private fun readJsonFromAssets(context: Context, filePath: String): String? {
        try {
            val source = context.assets.open(filePath).source().buffer()
            return source.readByteString().string(Charset.forName("utf-8"))

        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.mymenu, menu);
        return super.onCreateOptionsMenu(menu);

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.itemId
        if (id == R.id.mybutton) {
            Toast.makeText(this@MainActivity, "submit clicked", Toast.LENGTH_SHORT).show()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showPictureDialog() {
        val pictureDialog = AlertDialog.Builder(this)
        pictureDialog.setTitle("Select Action")

        val pictureDialogItems = arrayOf(
            "Select photo from gallery",
            "Capture photo from camera"
        )
        pictureDialog.setItems(
            pictureDialogItems
        ) { dialog, which ->
            when (which) {
                0 -> loadImageFromGallary()
                1 -> takeCameraImage()
            }
        }
        pictureDialog.show()
    }

    private fun takeCameraImage() {
        Dexter.withContext(this)
            .withPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        try {
                            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                            intent.putExtra(
                                MediaStore.EXTRA_OUTPUT,
                                FileProvider.getUriForFile(
                                    this@MainActivity,
                                    BuildConfig.APPLICATION_ID.toString() + ".fileprovider",
                                    createImageFile()
                                )
                            )
                            startActivityForResult(intent, REQ_CAMERA)
                        } catch (ex: java.io.IOException) {
                            Toast.makeText(this@MainActivity, "Error", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()
    }

    private fun loadImageFromGallary() {
        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        val galleryIntent = Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        )
                        startActivityForResult(galleryIntent, REQUEST_PICK_PHOTO)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()
    }

    @Throws(java.io.IOException::class)
    private fun createImageFile(): File {
        timeStamp = SimpleDateFormat("dd MMMM yyyy HH:mm").format(Date())
        imageName = "JPEG_"
        fileDirectoty =
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "")
        imageFilename = File.createTempFile(imageName, ".jpg", fileDirectoty)
        imageFilePath = imageFilename?.getAbsolutePath()
        return imageFilename!!
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CAMERA && resultCode == Activity.RESULT_OK) {
            convertImage(imageFilePath)
        } else if (requestCode == REQUEST_PICK_PHOTO && resultCode == Activity.RESULT_OK) {
            val selectedImage: Uri? = data?.data
            val filePathColumn = arrayOf<String>(MediaStore.Images.Media.DATA)
            assert(selectedImage != null)

            val cursor: Cursor = selectedImage?.let {
                contentResolver.query(
                    it, filePathColumn,
                    null, null, null
                )
            }!!
            cursor.moveToFirst()

            val columnIndex = cursor.getColumnIndex(filePathColumn[0])
            val mediaPath = cursor.getString(columnIndex)

            cursor.close()
            imageFilePath = mediaPath
            convertImage(mediaPath)
        }
    }

    private fun convertImage(urlImg: String?) {
        val imgFile = File(urlImg)
        if (imgFile.exists()) {
            val options: BitmapFactory.Options = BitmapFactory.Options()
            val bitmap: Bitmap = BitmapFactory.decodeFile(imageFilePath, options)
            pageData.indices.forEach { i ->
                if (i == currentIndex) {
                    pageData[i].bitmap = bitmap
                    (binding.rvHomePage.adapter as NestedAdapter).addItems(pageData, true)
                }
            }
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            imageBytes = baos.toByteArray()
            encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT)

            numberFormat = DecimalFormat()
            (numberFormat)?.maximumFractionDigits = 2
            fileSize = (imgFile.length() / 1024).toString().toInt()
            imageSize = (numberFormat)?.format(fileSize.toLong())
        }
    }

    private fun checkPermission(): Boolean {
        val result: Int =
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), 101
        )
    }

    companion object {
        private const val REQUEST_PICK_PHOTO = 1
    }
}