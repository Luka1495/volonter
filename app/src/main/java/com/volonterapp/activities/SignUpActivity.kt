package com.volonterapp.activities

import android.os.Bundle
import android.text.TextUtils
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.volonterapp.R
import com.volonterapp.firebase.FirestoreClass
import com.volonterapp.model.User
import kotlinx.android.synthetic.main.activity_sign_up.*

class SignUpActivity : BaseActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        setupActionBar()

        btn_sign_up.setOnClickListener {
            registerUser()
        }
    }


    private fun setupActionBar() {

        setSupportActionBar(toolbar_sign_up_activity)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_black_color_back_24dp)
        }

        toolbar_sign_up_activity.setNavigationOnClickListener { onBackPressed() }
    }


    private fun registerUser() {
        val userName = et_name.text.toString().trim()
        val userEmail = et_email.text.toString().trim()
        val userPassword = et_password.text.toString().trim()

        if (validateForm(userName, userEmail, userPassword)) {
            showProgressDialog(resources.getString(R.string.please_wait))

            FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener { authTask ->
                    authTask.result?.user?.let { firebaseUser ->
                        User(
                            id = firebaseUser.uid,
                            name = userName,
                            email = firebaseUser.email ?: userEmail
                        ).apply {
                            FirestoreClass().registerUser(this@SignUpActivity, this)
                        }
                    } ?: run {
                        authTask.exception?.message?.let { errorMessage ->
                            Toast.makeText(this@SignUpActivity, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }
    }


    private fun validateForm(name: String, email: String, password: String): Boolean {
        return when {
            TextUtils.isEmpty(name) -> {
                showErrorSnackBar("Please enter name.")
                false
            }
            TextUtils.isEmpty(email) -> {
                showErrorSnackBar("Please enter email.")
                false
            }
            TextUtils.isEmpty(password) -> {
                showErrorSnackBar("Please enter password.")
                false
            }
            else -> {
                true
            }
        }
    }


    fun userRegisteredSuccess() {

        Toast.makeText(
            this@SignUpActivity,
            "You have successfully registered.",
            Toast.LENGTH_SHORT
        ).show()

        hideProgressDialog()


        FirebaseAuth.getInstance().signOut()
        finish()
    }
}