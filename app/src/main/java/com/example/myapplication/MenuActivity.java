package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

// === imports de Firebase ===
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MenuActivity extends AppCompatActivity implements LocationListener {

    private static final int REQ_LOC = 1001;

    TextView mensaje;
    LocationManager locationManager;
    FusedLocationProviderClient fused;

    // Firebase
    DatabaseReference db;

    // contador de intentos para el primer fix
    int firstFixTries = 0;
    Handler handler = new Handler(Looper.getMainLooper());

    // para no escribir mil veces (guardamos solo una vez al conseguir fix)
    boolean guardadoUnaVez = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        mensaje = findViewById(R.id.txtmensaje);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        fused = LocationServices.getFusedLocationProviderClient(this);

        // Firebase DB
        db = FirebaseDatabase.getInstance().getReference();

        // pedir permiso si falta
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOC);
            return;
        }

        // si ya hay permiso, arrancar flujo
        startLocationFlow();
    }

    // revisar permiso
    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // configuración de ubicación
    private void startLocationFlow() {
        if (!hasLocationPermission()) return;

        // seed con lastKnown para mostrar algo si existe
        Location last = null;
        try { last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER); } catch (SecurityException ignored) {}
        if (last == null) {
            try { last = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER); } catch (SecurityException ignored) {}
        }
        if (last != null) {
            mensaje.setText(String.format(Locale.US, "%.6f, %.6f", last.getLatitude(), last.getLongitude()));
        } else {
            mensaje.setText("Obteniendo ubicación…");
        }

        // empezar a escuchar cambios
        try { locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this); } catch (SecurityException ignored) {}
        try { locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this); } catch (SecurityException ignored) {}

        // forzar primer fix con reintentos
        firstFixTries = 0;
        guardadoUnaVez = false;
        fetchFirstFix();
    }

    // intenta obtener una lectura puntual; si viene null, reintenta hasta 3 veces
    private void fetchFirstFix() {
        if (!hasLocationPermission()) return;

        try {
            fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(cur -> {
                        if (cur != null) {
                            mensaje.setText(String.format(Locale.US, "%.6f, %.6f", cur.getLatitude(), cur.getLongitude()));
                            // guardar en BD (una sola vez)
                            guardarUbicacionEnBD(cur);
                        } else {
                            // si aún no hay fix, reintentar en 1500 ms hasta 3 veces
                            if (firstFixTries < 3) {
                                firstFixTries++;
                                handler.postDelayed(this::fetchFirstFix, 1500);
                            }
                        }
                    });
        } catch (SecurityException ignored) { }
    }

    // cuando llega una nueva ubicación, actualizo el texto y guardo (si aún no guardamos)
    @Override
    public void onLocationChanged(@NonNull Location location) {
        mensaje.setText(String.format(Locale.US, "%.6f, %.6f", location.getLatitude(), location.getLongitude()));
        guardarUbicacionEnBD(location);
    }

    // guardar en Realtime Database bajo /users/{uid}/lastLocation
    private void guardarUbicacionEnBD(@NonNull Location loc) {
        if (guardadoUnaVez) return; // solo una vez para cumplir el enunciado
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> data = new HashMap<>();
        data.put("lat", loc.getLatitude());
        data.put("lng", loc.getLongitude());
        data.put("timestamp", System.currentTimeMillis());

        db.child("users")
                .child(uid)
                .child("lastLocation")
                .setValue(data)
                .addOnSuccessListener(a -> {
                    guardadoUnaVez = true; // marcamos que ya guardamos
                    Toast.makeText(this, "Ubicación guardada", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    // cuando el usuario responde al permiso
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOC && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationFlow(); // ahora sí arranca sin necesidad de loguear otra vez
        } else {
            Toast.makeText(this, "Permiso de ubicación requerido", Toast.LENGTH_SHORT).show();
        }
    }

    @Override public void onProviderDisabled(@NonNull String provider) { }
    @Override public void onProviderEnabled(@NonNull String provider) { }
}
