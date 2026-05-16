package com.example.clase4;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileActivity extends AppCompatActivity {

    private TextView txtDatosPerfil;
    private TextView txtEstadoPerfil;
    private Button btnActualizarPerfil;
    private Button btnCerrarSesion;
    private Button btnVolverPerfil;

    private int userId;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        txtDatosPerfil = findViewById(R.id.txtDatosPerfil);
        txtEstadoPerfil = findViewById(R.id.txtEstadoPerfil);
        btnActualizarPerfil = findViewById(R.id.btnActualizarPerfil);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        btnVolverPerfil = findViewById(R.id.btnVolverPerfil);

        SharedPreferences preferences = getSharedPreferences("sesion", MODE_PRIVATE);
        userId = preferences.getInt("userId", 0);

        btnActualizarPerfil.setOnClickListener(v -> cargarPerfil());
        btnVolverPerfil.setOnClickListener(v -> finish());

        btnCerrarSesion.setOnClickListener(v -> cerrarSesion());

        cargarPerfil();
    }

    private void cargarPerfil() {
        txtDatosPerfil.setText("Cargando perfil...");
        txtEstadoPerfil.setText("");

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/users/" + userId);
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");

                int statusCode = connection.getResponseCode();

                InputStream inputStream;

                if (statusCode >= 200 && statusCode < 300) {
                    inputStream = connection.getInputStream();
                } else {
                    inputStream = connection.getErrorStream();
                }

                String respuesta = leerRespuesta(inputStream);

                if (statusCode == 200) {
                    JSONObject usuario = new JSONObject(respuesta);
                    mainHandler.post(() -> mostrarPerfil(usuario));
                } else {
                    JSONObject errorJson = new JSONObject(respuesta);
                    String error = errorJson.optString("error", "Error al cargar perfil");
                    mainHandler.post(() -> txtDatosPerfil.setText(error));
                }

            } catch (Exception e) {
                mainHandler.post(() -> txtDatosPerfil.setText("No se pudo conectar con el servidor."));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void mostrarPerfil(JSONObject usuario) {
        try {
            String documento = usuario.optString("documento", "-");
            String nombre = usuario.optString("nombre", "-");
            String apellido = usuario.optString("apellido", "-");
            String email = usuario.optString("email", "-");
            String telefono = usuario.optString("telefono", "-");
            String direccion = usuario.optString("direccion", "-");
            String estado = usuario.optString("estado", "-");
            String admitido = usuario.optString("admitido", "-");
            String categoria = usuario.optString("categoria", "-");

            txtDatosPerfil.setText(
                    "Nombre: " + nombre + " " + apellido + "\n" +
                            "Documento: " + documento + "\n" +
                            "Email: " + email + "\n" +
                            "Teléfono: " + telefono + "\n" +
                            "Dirección: " + direccion + "\n" +
                            "Estado de usuario: " + estado + "\n" +
                            "Admitido como postor: " + admitido + "\n" +
                            "Categoría: " + categoria
            );

            if (estado.equals("activo") && admitido.equals("si")) {
                txtEstadoPerfil.setText("Usuario habilitado para participar en subastas.");
                txtEstadoPerfil.setTextColor(Color.parseColor("#16A34A"));
            } else {
                txtEstadoPerfil.setText("Usuario no habilitado para participar.");
                txtEstadoPerfil.setTextColor(Color.parseColor("#DC2626"));
            }

        } catch (Exception e) {
            txtDatosPerfil.setText("Error mostrando perfil.");
        }
    }

    private void cerrarSesion() {
        SharedPreferences preferences = getSharedPreferences("sesion", MODE_PRIVATE);
        preferences.edit().clear().apply();

        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String leerRespuesta(InputStream inputStream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder respuesta = new StringBuilder();
        String linea;

        while ((linea = reader.readLine()) != null) {
            respuesta.append(linea);
        }

        return respuesta.toString();
    }
}