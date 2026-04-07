package com.example.wildrunning

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.jotajotavm.wildrunning.ValidateEmail
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class LoginActivity : AppCompatActivity() {
    companion object {
        lateinit var useremail: String
        lateinit var providerSession: String
    }

    private var email by Delegates.notNull<String>()
    private var password by Delegates.notNull<String>()
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var lyTerms: LinearLayout
    private lateinit var mAuth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        lyTerms = findViewById(R.id.lyTerms)
        lyTerms.visibility = View.INVISIBLE

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        mAuth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        manageButtonLogin()
        etEmail.doOnTextChanged { _, _, _, _ -> manageButtonLogin() }
        etPassword.doOnTextChanged { _, _, _, _ -> manageButtonLogin() }

        onBackPressedDispatcher.addCallback(this) {
            val startMain = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(startMain)
        }
    }

    override fun onStart() {
        super.onStart()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            goHome(currentUser.email.toString(), currentUser.providerId)
        }
    }

    private fun manageButtonLogin() {
        val tvLogin = findViewById<TextView>(R.id.tvLogin)
        email = etEmail.text.toString()
        password = etPassword.text.toString()

        if (TextUtils.isEmpty(password) || !ValidateEmail.isEmail(email)) {
            tvLogin.setBackgroundColor(ContextCompat.getColor(this, R.color.gray))
            tvLogin.isEnabled = false
        } else {
            tvLogin.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
            tvLogin.isEnabled = true
        }
    }

    fun login(view: View) {
        loginUser()
    }

    private fun loginUser() {
        email = etEmail.text.toString()
        password = etPassword.text.toString()

        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    goHome(email, "email")
                } else {
                    if (lyTerms.visibility == View.INVISIBLE) {
                        lyTerms.visibility = View.VISIBLE
                    } else {
                        val cbAcept = findViewById<CheckBox>(R.id.cbAcept)
                        if (cbAcept.isChecked) {
                            register()
                        }
                    }
                }
            }
    }

    private fun goHome(email: String, provider: String) {
        useremail = email
        providerSession = provider
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun register() {
        email = etEmail.text.toString()
        password = etPassword.text.toString()

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val dateRegister = SimpleDateFormat("dd/MM/yyyy").format(Date())
                    val dbRegister = FirebaseFirestore.getInstance()
                    dbRegister.collection("users").document(email).set(
                        hashMapOf(
                            "user" to email,
                            "dateRegister" to dateRegister
                        )
                    )

                    goHome(email, "email")
                } else {
                    Toast.makeText(this, "Error, algo ha ido mal :(", Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun goTerms(v: View) {
        startActivity(Intent(this, TermsActivity::class.java))
    }

    fun forgotPassword(view: View) {
        resetPassword()
    }

    private fun resetPassword() {
        val userEmail = etEmail.text.toString()
        if (!TextUtils.isEmpty(userEmail)) {
            mAuth.sendPasswordResetEmail(userEmail)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Email enviado a $userEmail", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(
                            this,
                            "No se encontró el usuario con este correo",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        } else {
            Toast.makeText(this, "Indica un email", Toast.LENGTH_SHORT).show()
        }
    }

    fun callSignInGoogle(view: View) {
        signInGoogle()
    }

    private fun signInGoogle() {
        val googleIdOption = GetSignInWithGoogleOption.Builder(
            getString(R.string.default_web_client_id)
        ).build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@LoginActivity,
                    request = request
                )
                handleGoogleCredential(result.credential)
            } catch (_: GetCredentialCancellationException) {
                Toast.makeText(this@LoginActivity, "Login cancelado", Toast.LENGTH_SHORT).show()
            } catch (e: GetCredentialException) {
                Log.e("GOOGLE_SIGN_IN", "Credential Manager error", e)
                Toast.makeText(this@LoginActivity, "Login Google falló", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun handleGoogleCredential(credential: androidx.credentials.Credential) {
        if (
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
            } catch (e: GoogleIdTokenParsingException) {
                Log.e("GOOGLE_SIGN_IN", "Invalid Google ID token", e)
                Toast.makeText(this, "No se pudo leer la cuenta de Google", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            Log.w("GOOGLE_SIGN_IN", "Credential is not a Google ID token")
            Toast.makeText(this, "No se recibió una credencial de Google válida", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        mAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    goHome(mAuth.currentUser!!.email!!, "Google")
                } else {
                    Toast.makeText(this, "Error Firebase", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
