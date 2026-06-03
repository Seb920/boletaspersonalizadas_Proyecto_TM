package com.ejemplo.boletaspersonalizadas.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ejemplo.boletaspersonalizadas.databinding.ActivityLoginBinding
import com.ejemplo.boletaspersonalizadas.repositories.FirebaseRepository
import com.ejemplo.boletaspersonalizadas.utils.PreferenciasManager
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseRepo: FirebaseRepository
    private lateinit var preferencias: PreferenciasManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firebaseRepo = FirebaseRepository()
        preferencias = PreferenciasManager(this)

        // Verificar si ya hay sesión activa
        if (preferencias.isLoggedIn()) {
            verificarRolYRedirigir()
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                iniciarSesion(email, password)
            } else {
                Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnRegistro.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val rol = if (binding.radioAdmin.isChecked) "admin" else "empleado"
            val nombre = binding.etNombre.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && nombre.isNotEmpty()) {
                registrarUsuario(email, password, nombre, rol)
            } else {
                Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun iniciarSesion(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""
                    firebaseRepo.obtenerUsuario(userId) { usuario ->
                        if (usuario != null) {
                            preferencias.guardarSesion(userId, usuario.rol, usuario.nombre)
                            verificarRolYRedirigir()
                        }
                    }
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun registrarUsuario(email: String, password: String, nombre: String, rol: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""
                    val usuario = com.ejemplo.boletaspersonalizadas.models.Usuario(
                        id = userId,
                        email = email,
                        nombre = nombre,
                        rol = rol
                    )
                    firebaseRepo.guardarUsuario(usuario) {
                        Toast.makeText(this, "Usuario registrado exitosamente", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun verificarRolYRedirigir() {
        val rol = preferencias.getRol()
        if (rol == "admin") {
            startActivity(Intent(this, AdminActivity::class.java))
        } else {
            startActivity(Intent(this, EmpleadoActivity::class.java))
        }
        finish()
    }
}