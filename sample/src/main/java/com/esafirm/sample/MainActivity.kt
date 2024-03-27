package com.esafirm.sample

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.esafirm.imagepicker.features.*
import com.esafirm.imagepicker.features.cameraonly.CameraOnlyConfig
import com.esafirm.imagepicker.model.Image
import com.esafirm.sample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val images = arrayListOf<Image>()

    private val imagePickerLauncher = registerImagePicker {
        images.clear()
        images.addAll(it)
        printImages(images)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }
        setupButtons()
    }

    private fun setupButtons() = binding.run {
        buttonPickImage.setOnClickListener { start() }
        buttonIntent.setOnClickListener { startWithIntent() }
        buttonCamera.setOnClickListener { captureImage() }
        buttonCustomUi.setOnClickListener { startCustomUI() }
        buttonLaunchFragment.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MainFragment())
                .commitAllowingStateLoss()
        }
    }

    private fun captureImage() {
        imagePickerLauncher.launch(CameraOnlyConfig(requestCameraPermission = true))
    }

    private fun createConfig(): ImagePickerConfig {
        val returnAfterCapture = binding.switchReturnAfterCapture.isChecked
        val isSingleMode = binding.switchSingle.isChecked
        val useCustomImageLoader = binding.switchImageloader.isChecked
        val folderMode = binding.switchFolderMode.isChecked
        val includeVideo = binding.switchIncludeVideo.isChecked
        val onlyVideo = binding.switchOnlyVideo.isChecked
        val isExclude = binding.switchIncludeExclude.isChecked
        val autoSelectCameraImage = binding.switchAutoSelectCameraImage.isChecked

        ImagePickerComponentsHolder.setInternalComponent(
            CustomImagePickerComponents(this, useCustomImageLoader)
        )

        return ImagePickerConfig {

            mode = if (isSingleMode) {
                ImagePickerMode.SINGLE
            } else {
                ImagePickerMode.MULTIPLE // multi mode (default mode)
            }

            language = "in" // Set image picker language
            theme = R.style.ImagePickerTheme

            // set whether pick action or camera action should return immediate result or not. Only works in single mode for image picker
            returnMode = if (returnAfterCapture) ReturnMode.ALL else ReturnMode.NONE

            isFolderMode = folderMode // set folder mode (false by default)
            isIncludeVideo = includeVideo // include video (false by default)
            isAutoSelectCameraImage = autoSelectCameraImage
            isOnlyVideo = onlyVideo // include video (false by default)
            arrowColor = Color.RED // set toolbar arrow up color
            folderTitle = "Folder" // folder selection title
            imageTitle = "Tap to select" // image selection title
            doneButtonText = "DONE" // done button text
            showDoneButtonAlways = true // Show done button always or not
            limit = 10 // max images can be selected (99 by default)
            isShowCamera = true // show camera or not (true by default)
            isShowVideoCam = true
            savePath = ImagePickerSavePath("Camera") // captured image directory name ("Camera" folder by default)
            savePath = ImagePickerSavePath(Environment.getExternalStorageDirectory().path, isRelative = false) // can be a full path
            requestCameraPermission = true

            if (isExclude) {
                excludedImages = images.toFiles() // don't show anything on this selected images
            } else {
                selectedImages = images  // original selected images, used in multi mode
            }
        }
    }

    private val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val intent = createImagePickerIntent(this, createConfig())
        startActivityForResult(intent, IpCons.RC_IMAGE_PICKER)
    }

    private fun startWithIntent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requestPermissions.launch(arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_VISUAL_USER_SELECTED))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions.launch(arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO))
        } else {
            requestPermissions.launch(arrayOf(READ_EXTERNAL_STORAGE))
        }
    }

    private fun start() {
        imagePickerLauncher.launch(createConfig())
    }

    private fun startCustomUI() {
        val intent = Intent(this, CustomUIActivity::class.java)
        intent.putExtra(ImagePickerConfig::class.java.simpleName, createConfig())
        startActivityForResult(intent, IpCons.RC_IMAGE_PICKER)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == IpCons.RC_IMAGE_PICKER && data != null) {
            images.clear()
            images.addAll(ImagePicker.getImages(data) ?: emptyList())
            printImages(images)
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun printImages(images: List<Image>?) {
        if (images == null) return
        binding.textView.text = images.joinToString("\n")
        binding.textView.setOnClickListener {
            ImageViewerActivity.start(this@MainActivity, images)
        }
    }
}