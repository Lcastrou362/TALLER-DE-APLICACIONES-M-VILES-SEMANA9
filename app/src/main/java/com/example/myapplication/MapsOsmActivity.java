package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.Locale;

public class MapsOsmActivity extends AppCompatActivity {

    // UI
    private MapView map;
    private EditText edtMonto;
    private TextView txtCoords, txtDistancia, txtCosto;

    // OSM
    private MyLocationNewOverlay myLocationOverlay;

    // Fused Location
    private FusedLocationProviderClient fused;

    // Último fix conocido
    private Location lastFix;

    // Coordenadas de la tienda (ajusta a las reales)
    private static final GeoPoint TIENDA = new GeoPoint(-33.6846, -71.2153);

    // Permisos
    private final ActivityResultLauncher<String[]> permisoLoc =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), res -> {
                boolean ok = Boolean.TRUE.equals(res.get(Manifest.permission.ACCESS_FINE_LOCATION))
                        || Boolean.TRUE.equals(res.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (ok) {
                    enableMyLocation();
                    obtenerUbicacionFresca();
                } else {
                    Toast.makeText(this, "Se requiere permiso de ubicación", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Si no hay sesión, vuelve al login
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        if (current == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Osmdroid: antes del layout
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().load(
                getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
        );

        setContentView(R.layout.activity_maps_osm);

        fused = LocationServices.getFusedLocationProviderClient(this);

        // UI
        map         = findViewById(R.id.osmMap);
        edtMonto    = findViewById(R.id.edtMonto);
        txtCoords   = findViewById(R.id.txtCoords);
        txtDistancia= findViewById(R.id.txtDistancia);
        txtCosto    = findViewById(R.id.txtCosto);

        map.setMultiTouchControls(true);

        // Centro inicial: tienda + marcador fijo
        IMapController controller = map.getController();
        controller.setZoom(14.0);
        controller.setCenter(TIENDA);

        Marker mTienda = new Marker(map);
        mTienda.setPosition(TIENDA);
        mTienda.setTitle("Tienda");
        map.getOverlays().add(mTienda);

        // Punto azul (mi ubicación)
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        myLocationOverlay.enableMyLocation();
        map.getOverlays().add(myLocationOverlay);

        // Calcular
        Button btnCalcular = findViewById(R.id.btnCalcular);
        btnCalcular.setOnClickListener(v -> calcularCostoConUbicacionFresca());

        // Cerrar sesión (NUEVO)
        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MapsOsmActivity.this, MainActivity.class));
            finishAffinity(); // cierra toda la pila
        });

        enableMyLocation();
        obtenerUbicacionFresca();
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void enableMyLocation() {
        if (!hasLocationPermission()) {
            permisoLoc.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void obtenerUbicacionFresca() {
        if (!hasLocationPermission()) {
            enableMyLocation();
            return;
        }
        try {
            CancellationTokenSource cts = new CancellationTokenSource();
            fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                    .addOnSuccessListener(loc -> {
                        if (loc != null) {
                            actualizarUIConUbicacion(loc);
                        } else {
                            fused.getLastLocation().addOnSuccessListener(last -> {
                                if (last != null) actualizarUIConUbicacion(last);
                            });
                        }
                    });
        } catch (SecurityException se) {
            Toast.makeText(this, "Permiso de ubicación requerido", Toast.LENGTH_SHORT).show();
            enableMyLocation();
        }
    }
    private void actualizarUIConUbicacion(@NonNull Location loc) {
        lastFix = loc; // guardar último fix

        GeoPoint me = new GeoPoint(loc.getLatitude(), loc.getLongitude());
        txtCoords.setText(String.format(Locale.getDefault(),
                "Lat: %.5f, Lon: %.5f", me.getLatitude(), me.getLongitude()));

        map.getController().setZoom(16.0);
        map.getController().animateTo(me);

        Marker mYo = new Marker(map);
        mYo.setPosition(me);
        mYo.setTitle("Mi ubicación");
        map.getOverlays().add(mYo);
        map.invalidate();

        double km = haversineKm(me.getLatitude(), me.getLongitude(),
                TIENDA.getLatitude(), TIENDA.getLongitude());
        txtDistancia.setText(String.format(Locale.getDefault(),
                "Distancia: %.2f km", km));
    }

    private void calcularCostoConUbicacionFresca() {
        String sMonto = edtMonto.getText().toString().trim();
        if (sMonto.isEmpty()) {
            Toast.makeText(this, "Ingresa el monto de compra", Toast.LENGTH_SHORT).show();
            return;
        }

        final long monto;
        try {
            monto = Long.parseLong(sMonto);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Monto inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasLocationPermission()) {
            enableMyLocation();
            return;
        }

        // 1) Si ya tenemos un fix, calcula de inmediato
        if (lastFix != null) {
            double kmNow = haversineKm(lastFix.getLatitude(), lastFix.getLongitude(),
                    TIENDA.getLatitude(), TIENDA.getLongitude());
            txtDistancia.setText(String.format(Locale.getDefault(),
                    "Distancia: %.2f km", kmNow));
            double costoNow = tarifaDespacho(monto, kmNow);
            txtCosto.setText(String.format(Locale.getDefault(),
                    "Costo despacho: %,.0f CLP", costoNow));
        } else {
            Toast.makeText(this, "Buscando ubicación…", Toast.LENGTH_SHORT).show();
        }

        // 2) Paralelo: lectura fresca y recalcular
        try {
            CancellationTokenSource cts = new CancellationTokenSource();
            fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                    .addOnSuccessListener(loc -> {
                        if (loc == null) return;
                        lastFix = loc;
                        double km = haversineKm(loc.getLatitude(), loc.getLongitude(),
                                TIENDA.getLatitude(), TIENDA.getLongitude());
                        txtDistancia.setText(String.format(Locale.getDefault(),
                                "Distancia: %.2f km", km));
                        double costo = tarifaDespacho(monto, km);
                        txtCosto.setText(String.format(Locale.getDefault(),
                                "Costo despacho: %,.0f CLP", costo));
                    });
        } catch (SecurityException ignored) {}
    }

    private double tarifaDespacho(long monto, double distanciaKm) {
        if (monto >= 50000 && distanciaKm <= 20.0) return 0.0; // gratis si ≥50k y ≤20km
        if (monto >= 25000 && monto <= 49999)   return distanciaKm * 150.0;
        return distanciaKm * 300.0;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(dLon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Override protected void onResume() { super.onResume(); map.onResume(); }
    @Override protected void onPause()  { super.onPause();  map.onPause();  }
}