package com.volonterapp.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.volonterapp.R
import com.volonterapp.firebase.FirestoreClass
import com.volonterapp.model.Board
import com.volonterapp.utils.Constants
import kotlinx.android.synthetic.main.activity_create_board.*
import kotlinx.android.synthetic.main.activity_my_profile.*
import java.io.IOException

class CreateBoardActivity : BaseActivity() {

    private var mSelectedImageFileUri: Uri? = null

    private lateinit var mUserName: String

    private var mBoardImageURL: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_board)

        setupActionBar()

        if (intent.hasExtra(Constants.NAME)) {
            mUserName = intent.getStringExtra(Constants.NAME)!!
        }

        iv_board_image.setOnClickListener { view ->

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED
            ) {
                Constants.showImageChooser(this@CreateBoardActivity)
            } else {

                ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        Constants.READ_STORAGE_PERMISSION_CODE
                )
            }
        }

        btn_create.setOnClickListener {

                if (mSelectedImageFileUri != null) {

                    uploadBoardImage()
                } else {

                    showProgressDialog(resources.getString(R.string.please_wait))

                    createBoard()
                }
        }
    }


    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.READ_STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Constants.showImageChooser(this@CreateBoardActivity)
            } else {
                Toast.makeText(
                        this,
                        "Oops, you just denied the permission for storage. You can also allow it from settings.",
                        Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK
                && requestCode == Constants.PICK_IMAGE_REQUEST_CODE
                && data!!.data != null
        ) {
            mSelectedImageFileUri = data.data

            try {
                Glide
                        .with(this@CreateBoardActivity)
                        .load(Uri.parse(mSelectedImageFileUri.toString()))
                        .centerCrop()
                        .placeholder(R.drawable.ic_user_place_holder)
                        .into(iv_board_image)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    private fun setupActionBar() {

        setSupportActionBar(toolbar_create_board_activity)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
        }

        toolbar_create_board_activity.setNavigationOnClickListener { onBackPressed() }
    }


    private fun uploadBoardImage() {
        showProgressDialog(resources.getString(R.string.please_wait))

        mSelectedImageFileUri?.let { imageUri ->
            val timestamp = System.currentTimeMillis()
            val fileExtension = Constants.getFileExtension(this@CreateBoardActivity, imageUri)
            val imagePath = "BOARD_COVER_$timestamp.$fileExtension"

            FirebaseStorage.getInstance().reference
                .child(imagePath)
                .putFile(imageUri)
                .addOnSuccessListener { uploadResult ->
                    uploadResult.metadata?.reference?.downloadUrl
                        ?.addOnSuccessListener { downloadUri ->
                            mBoardImageURL = downloadUri.toString()
                            createBoard()
                        }
                }
                .addOnFailureListener { error ->
                    error.message?.let { errorMessage ->
                        Toast.makeText(this@CreateBoardActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                    hideProgressDialog()
                }
        }
    }


    private fun createBoard() {
        val boardMembers = mutableListOf<String>().apply {
            add(getCurrentUserID())
        }

        val newBoard = Board(
            name = et_board_name.text.toString(),
            image = mBoardImageURL,
            createdBy = mUserName,
            assignedTo = ArrayList(boardMembers)
        )

        FirestoreClass().createBoard(this@CreateBoardActivity, newBoard)
    }


    fun boardCreatedSuccessfully() {

        hideProgressDialog()

        setResult(Activity.RESULT_OK)
        finish()
    }
}