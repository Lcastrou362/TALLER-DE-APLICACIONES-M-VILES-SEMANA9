package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText txtEmail, txtPassword;
    private Button btnIngresar;
    private TextView txtBienvenido;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Insets (márgenes seguros) — fuera de loginUser
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // UI
        txtBienvenido = findViewById(R.id.txtBienvenido);
        txtEmail      = findViewById(R.id.txtemail);
        txtPassword   = findViewById(R.id.txtPassword);
        btnIngresar   = findViewById(R.id.btnIngresar);

        txtBienvenido.setText("Bienvenido");
        btnIngresar.setText("Ingresar");

        // Firebase
        mAuth = FirebaseAuth.getInstance();

        // Click
        btnIngresar.setOnClickListener(v -> loginUser());
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Si ya hay sesión, ir directo al mapa
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            irAlMapa();
        }
    }

    private void loginUser() {
        String email = txtEmail.getText().toString().trim();
        String pass  = txtPassword.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Completa correo y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Correo inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        btnIngresar.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            btnIngresar.setEnabled(true);
            if (task.isSuccessful()) {
                irAlMapa();
            } else {
                String msg = (task.getException() != null)
                        ? task.getException().getLocalizedMessage()
                        : "Error de autenticación";
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void irAlMapa() {
        startActivity(new Intent(MainActivity.this, MapsOsmActivity.class));
        finish(); // evita volver al login con back
    }
}
