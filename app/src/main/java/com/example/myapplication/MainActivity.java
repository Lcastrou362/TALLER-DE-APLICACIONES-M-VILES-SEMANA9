package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;


public class MainActivity extends AppCompatActivity {

    // Variable para la autenticación de Firebase
    private FirebaseAuth mAuth;
    // Campos de texto para el correo y la contraseña
    private EditText txtemail, txtPassword;
    // Botón para ingresar
    private Button btnIngresar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // Ajusta la interfaz para ocupar toda la pantalla
        setContentView(R.layout.activity_main);

        // Ejemplos de logs (informativo, error y advertencia)
        Log.d("Titulo del mensaje", "contenido");
        Log.e("Tag", "contenido");
        Log.w("Un Mensaje", "contenido");

        // Texto de bienvenida
        TextView bienbenido = findViewById(R.id.txtBienvenido);
        bienbenido.setText("Bienbenido");

        // Vinculamos los elementos del layout con las variables
        txtemail = findViewById(R.id.txtemail);
        txtPassword = findViewById(R.id.txtPassword);
        btnIngresar = findViewById(R.id.btnIngresar);
        btnIngresar.setText("Ingresar");

        // Inicializamos FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Acción al hacer clic en el botón: llamar a loginUser()
        btnIngresar.setOnClickListener(v -> loginUser());
    }

    // Método para iniciar sesión con Firebase Authentication
    private void loginUser() {
        // Obtenemos los valores de correo y contraseña
        String email = txtemail.getText().toString().trim();
        String pass = txtPassword.getText().toString().trim();

        // Validación: no dejar campos vacíos
        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Completa correo y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        // Intentamos autenticar con Firebase
        mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Si el login es correcto, abrimos MenuActivity
                Intent i = new Intent(MainActivity.this, MenuActivity.class);
                startActivity(i);
                finish(); // Cerramos MainActivity para que no se pueda volver atrás
            } else {
                // Si ocurre un error, mostramos el mensaje
                String msg = (task.getException() != null)
                        ? task.getException().getLocalizedMessage()
                        : "Error de autenticacion";
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        });

        // Ajuste de márgenes para la compatibilidad con barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
