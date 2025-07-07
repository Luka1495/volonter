package com.volonterapp.activities

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.volonterapp.R
import com.volonterapp.firebase.FirestoreClass
import com.volonterapp.model.User
import kotlinx.android.synthetic.main.activity_sign_in.*

class SignInActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

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

        btn_sign_in.setOnClickListener {
            signInRegisteredUser()
        }
    }


    private fun setupActionBar() {

        setSupportActionBar(toolbar_sign_in_activity)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_black_color_back_24dp)
        }

        toolbar_sign_in_activity.setNavigationOnClickListener { onBackPressed() }
    }


    private fun signInRegisteredUser() {
        val userEmail = et_email.text.toString().trim()
        val userPassword = et_password.text.toString().trim()

        if (validateForm(userEmail, userPassword)) {
            showProgressDialog(resources.getString(R.string.please_wait))

            FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener { authTask ->
                    authTask.result?.user?.let {
                        FirestoreClass().loadUserData(this@SignInActivity)
                    } ?: run {
                        authTask.exception?.message?.let { errorMessage ->
                            Toast.makeText(this@SignInActivity, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }
    }


    private fun validateForm(email: String, password: String): Boolean {
        return if (TextUtils.isEmpty(email)) {
            showErrorSnackBar("Please enter email.")
            false
        } else if (TextUtils.isEmpty(password)) {
            showErrorSnackBar("Please enter password.")
            false
        } else {
            true
        }
    }


    fun signInSuccess(user: User) {

        hideProgressDialog()

        startActivity(Intent(this@SignInActivity, MainActivity::class.java))
        this.finish()
    }
}