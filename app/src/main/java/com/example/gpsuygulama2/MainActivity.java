package com.example.gpsuygulama2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.FirebaseApp;  // FirebaseApp importunu ekledik

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "FirebaseGPSControl";
    private GoogleMap gMap;
    private Double latitude = 0.0; // Değiştirildi: double -> Double
    private Double longitude = 0.0; // Değiştirildi: double -> Double

    private boolean isLedOn = false;
    private boolean isBuzzerOn = false;

    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Firebase'i başlat
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);  // Firebase başlatma işlemi
        }

        // Harita fragmentini başlat
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.id_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        TextView tvCoordinates = findViewById(R.id.tvCoordinates);
        Button btnFetchData = findViewById(R.id.btnFetchData);
        Button btnToggleLed = findViewById(R.id.btnToggleLed);
        Button btnToggleSound = findViewById(R.id.btnToggleSound);

        // Firebase Realtime Database referansını al
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Verileri Firebase'den izlemek için listener ekleyin
        listenToFirebaseData(tvCoordinates);

        // LED kontrolü için buton işlemleri
        btnToggleLed.setOnClickListener(v -> {
            isLedOn = !isLedOn;
            if (isLedOn) {
                btnToggleLed.setText("LED Kapat");
            } else {
                btnToggleLed.setText("LED Yak");
            }
            Log.d(TAG, "LED Durumu: " + (isLedOn ? "Açık" : "Kapalı"));
            databaseReference.child("Controls").child("LED").setValue(isLedOn ? 1 : 0)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "LED durumu Firebase'e gönderildi.");
                        } else {
                            Log.e(TAG, "LED durumu Firebase'e gönderilemedi: " + task.getException().getMessage());
                        }
                    });
        });

        // Buzzer kontrolü için buton işlemleri
        btnToggleSound.setOnClickListener(v -> {
            isBuzzerOn = !isBuzzerOn;
            if (isBuzzerOn) {
                btnToggleSound.setText("Ses Kapat");
            } else {
                btnToggleSound.setText("Ses Çıkar");
            }
            Log.d(TAG, "Buzzer Durumu: " + (isBuzzerOn ? "Açık" : "Kapalı"));
            databaseReference.child("Controls").child("Buzzer").setValue(isBuzzerOn ? 1 : 0)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Buzzer durumu Firebase'e gönderildi.");
                        } else {
                            Log.e(TAG, "Buzzer durumu Firebase'e gönderilemedi: " + task.getException().getMessage());
                        }
                    });
        });

        // GPS verisi çekme butonu
        btnFetchData.setOnClickListener(v -> fetchGPSDataFromFirebase(tvCoordinates));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
        updateMap(); // İlk veri çekilmeden önce harita başlangıç konumunu günceller
    }

    private void updateMap() {
        if (gMap == null || latitude == 0.0 || longitude == 0.0) {
            return; // Eğer harita veya GPS verisi geçerli değilse, harita güncellenmesin
        }
        // Haritada işaretçi ekleme
        LatLng location = new LatLng(latitude, longitude);
        gMap.clear(); // Önceki işaretçileri temizle
        gMap.addMarker(new MarkerOptions().position(location).title("Konum"));
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 12));
    }

    private void fetchGPSDataFromFirebase(TextView tvCoordinates) {
        databaseReference.child("GPS").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    latitude = snapshot.child("Latitude").getValue(Double.class);
                    longitude = snapshot.child("Longitude").getValue(Double.class);

                    // Null kontrolü ekleyin
                    if (latitude != null && longitude != null) {
                        tvCoordinates.setText("Latitude: " + latitude + "\nLongitude: " + longitude);
                        if (gMap != null) {
                            updateMap();
                        }
                    } else {
                        tvCoordinates.setText("GPS verisi eksik.");
                    }
                } else {
                    tvCoordinates.setText("GPS verisi mevcut değil.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase'den veri alınamadı: " + error.getMessage());
            }
        });
    }

    private void listenToFirebaseData(TextView tvCoordinates) {
        // Firebase'den LED ve Buzzer durumunu dinleyin
        databaseReference.child("Controls").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    isLedOn = snapshot.child("LED").getValue(Integer.class) == 1;
                    isBuzzerOn = snapshot.child("Buzzer").getValue(Integer.class) == 1;

                    Log.d(TAG, "LED Durumu: " + (isLedOn ? "Açık" : "Kapalı"));
                    Log.d(TAG, "Buzzer Durumu: " + (isBuzzerOn ? "Açık" : "Kapalı"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase'den veri dinlenemedi: " + error.getMessage());
            }
        });

        // GPS verisini dinleyin
        databaseReference.child("GPS").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    latitude = snapshot.child("Latitude").getValue(Double.class);
                    longitude = snapshot.child("Longitude").getValue(Double.class);

                    // Null kontrolü ekleyin
                    if (latitude != null && longitude != null) {
                        tvCoordinates.setText("Latitude: " + latitude + "\nLongitude: " + longitude);
                        if (gMap != null) {
                            updateMap();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase'den GPS verisi alınamadı: " + error.getMessage());
            }
        });
    }
}
