package ba.sum.fpmoz.aplikacijazaupravljanjeosobnimfinacijama

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth



class LoginActivity : AppCompatActivity() {




    private lateinit var auth: FirebaseAuth
    private lateinit var emailTxt: EditText
    private lateinit var lozinkaTxt: EditText
    private lateinit var loginSubmitBtn: Button
    private lateinit var registerBtn: Button
    private lateinit var googleSignInBtn: Button
    private lateinit var forgotPasswordBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login) // layout sa samo prijavom, registracijom itd.

        auth = FirebaseAuth.getInstance()

        emailTxt = findViewById(R.id.emailTxt)
        lozinkaTxt = findViewById(R.id.lozinkaTxt)
        loginSubmitBtn = findViewById(R.id.loginSubmitBtn)
        registerBtn = findViewById(R.id.registerLink)
        googleSignInBtn = findViewById(R.id.googleSignInBtn)
        forgotPasswordBtn = findViewById(R.id.btnForgotPassword)

        loginSubmitBtn.setOnClickListener {
            val email = emailTxt.text.toString().trim()
            val lozinka = lozinkaTxt.text.toString().trim()

            if (email.isNotEmpty() && lozinka.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, lozinka)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val intent = Intent(this, HomeActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(
                                this,
                                "Greška: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    }
            } else {
                Toast.makeText(this, "Unesite e-mail i lozinku", Toast.LENGTH_SHORT).show()
            }
        }
    }
        override fun onStart() {
            super.onStart()
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                val intent = Intent(this, HomeActivity::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }






        registerBtn.setOnClickListener {
                startActivity(Intent(this, RegisterActivity::class.java))
            }

            googleSignInBtn.setOnClickListener {
                // Pokreni Google prijavu - možeš ovdje staviti kod za Google sign-in ili pozvati funkciju
                Toast.makeText(this, "Google prijava (implementirati)", Toast.LENGTH_SHORT).show()
            }

            forgotPasswordBtn.setOnClickListener {
                startActivity(Intent(this, ForgotPasswordActivity::class.java))
            }


        }
    }

