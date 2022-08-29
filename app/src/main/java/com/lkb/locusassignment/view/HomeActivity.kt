package com.lkb.locusassignment.view

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
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.lkb.locusassignment.BuildConfig
import com.lkb.locusassignment.view.adapter.NestedAdapter
import com.lkb.locusassignment.data.PageItem
import com.lkb.locusassignment.R
import com.lkb.locusassignment.databinding.ActivityMainBinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
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


class HomeActivity : AppCompatActivity() {
    private var currentIndex = 0
    private var fileSize = 0
    private var imageFilePath: String? = null
    private var encodedImage: String? = null
    private var timeStamp: String? = null
    private var imageName: String? = null
    private var imageSize: String? = null
    private var directoty: File? = null
    private var imageFilename: File? = null
    private var numberFormat: NumberFormat? = null
    private lateinit var imageBytes: ByteArray
    private lateinit var binding: ActivityMainBinding
    private var pageData: List<PageItem> = listOf()
    private lateinit var viewModel: HomeViewModel
    lateinit var cameraActivityResultListener: ActivityResultLauncher<Intent>
    lateinit var galleryLauncherResultListener: ActivityResultLauncher<Intent>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this@HomeActivity)[HomeViewModel::class.java]
        readDataFromAssets()
        initRecyclerView()
        initListener()
        val verifyPermission: Int = Build.VERSION.SDK_INT
        if (verifyPermission > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (checkPermission()) {
                requestPermission()
            }
        }
    }

    private fun readDataFromAssets() {
        val data = readJsonFromAssets(
            application,
            "data.json"
        )
        val reviewType: Type? = object : TypeToken<List<PageItem>?>() {}.type
        if (data != null) {
            pageData = Gson().fromJson(data, reviewType)
        }

    }

    private fun initListener() {
        galleryLauncherResultListener =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult())
            { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val selectedImage: Uri? = result.data?.data
                    val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
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
        cameraActivityResultListener =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult())
            { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    convertImage(imageFilePath)
                }
            }
    }

    private fun initRecyclerView() {
        with(binding.rvHomePage) {
            setHasFixedSize(true)
            adapter = NestedAdapter(pageData) { s, i ->
                currentIndex = i
                showPictureDialog()
            }
            val manager = LinearLayoutManager(context)
            layoutManager = manager
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
            viewModel.logData(pageData)
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
                0 -> loadImageFromGallery()
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
                                    this@HomeActivity,
                                    BuildConfig.APPLICATION_ID + ".fileprovider",
                                    createImageFile()
                                )
                            )
                            cameraActivityResultListener.launch(intent)
                            //startActivityForResult(intent, REQ_CAMERA)
                        } catch (ex: java.io.IOException) {
                            Toast.makeText(this@HomeActivity, "Error", Toast.LENGTH_SHORT).show()
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

    private fun loadImageFromGallery() {
        Dexter.withContext(this)
            .withPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        val galleryIntent = Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        )
                        galleryLauncherResultListener.launch(galleryIntent)
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
        directoty =
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "")
        imageFilename = File.createTempFile(imageName, ".jpg", directoty)
        imageFilePath = imageFilename?.absolutePath
        return imageFilename!!
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
}