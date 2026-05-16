package com.example.clase4;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationsActivity extends AppCompatActivity {

    private TextView txtMensajeNotificaciones;
    private Button btnActualizarNotificaciones;
    private Button btnVolverNotificaciones;
    private LinearLayout contenedorNotificaciones;

    private int userId;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        txtMensajeNotificaciones = findViewById(R.id.txtMensajeNotificaciones);
        btnActualizarNotificaciones = findViewById(R.id.btnActualizarNotificaciones);
        btnVolverNotificaciones = findViewById(R.id.btnVolverNotificaciones);
        contenedorNotificaciones = findViewById(R.id.contenedorNotificaciones);

        SharedPreferences preferences = getSharedPreferences("sesion", MODE_PRIVATE);
        userId = preferences.getInt("userId", 0);

        btnActualizarNotificaciones.setOnClickListener(v -> cargarNotificaciones());
        btnVolverNotificaciones.setOnClickListener(v -> finish());

        cargarNotificaciones();
    }

    private void cargarNotificaciones() {
        txtMensajeNotificaciones.setText("Cargando notificaciones...");
        contenedorNotificaciones.removeAllViews();

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/clients/" + userId + "/notifications");
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
                    JSONArray notificaciones = new JSONArray(respuesta);
                    mainHandler.post(() -> mostrarNotificaciones(notificaciones));
                } else {
                    JSONObject errorJson = new JSONObject(respuesta);
                    String error = errorJson.optString("error", "Error al cargar notificaciones");
                    mainHandler.post(() -> txtMensajeNotificaciones.setText(error));
                }

            } catch (Exception e) {
                mainHandler.post(() -> txtMensajeNotificaciones.setText("No se pudo conectar con el servidor."));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void mostrarNotificaciones(JSONArray notificaciones) {
        contenedorNotificaciones.removeAllViews();

        if (notificaciones.length() == 0) {
            txtMensajeNotificaciones.setText("No tenés notificaciones.");
            return;
        }

        txtMensajeNotificaciones.setText("Notificaciones encontradas: " + notificaciones.length());

        try {
            for (int i = 0; i < notificaciones.length(); i++) {
                JSONObject notificacion = notificaciones.getJSONObject(i);

                int id = notificacion.optInt("id", 0);
                String titulo = notificacion.optString("titulo", "-");
                String mensaje = notificacion.optString("mensaje", "-");
                String fechaHora = notificacion.optString("fechaHora", "-");
                String leida = notificacion.optString("leida", "no");

                View card = crearCardNotificacion(id, titulo, mensaje, fechaHora, leida);
                contenedorNotificaciones.addView(card);
            }

        } catch (Exception e) {
            txtMensajeNotificaciones.setText("Error mostrando notificaciones.");
        }
    }

    private View crearCardNotificacion(
            int id,
            String titulo,
            String mensaje,
            String fechaHora,
            String leida
    ) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(28, 24, 28, 24);

        if (leida.equals("si")) {
            card.setBackgroundColor(Color.WHITE);
        } else {
            card.setBackgroundColor(Color.parseColor("#EFF6FF"));
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        params.setMargins(0, 0, 0, 22);
        card.setLayoutParams(params);
        card.setElevation(4);

        TextView txtTitulo = new TextView(this);
        txtTitulo.setText(titulo);
        txtTitulo.setTextSize(18);
        txtTitulo.setTextColor(Color.parseColor("#0F172A"));
        txtTitulo.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView txtMensaje = new TextView(this);
        txtMensaje.setText(mensaje);
        txtMensaje.setTextSize(15);
        txtMensaje.setTextColor(Color.parseColor("#475569"));
        txtMensaje.setPadding(0, 12, 0, 12);

        TextView txtDetalle = new TextView(this);
        txtDetalle.setText(
                "ID: #" + id + "\n" +
                        "Fecha: " + fechaHora + "\n" +
                        "Leída: " + leida
        );
        txtDetalle.setTextSize(13);
        txtDetalle.setTextColor(Color.parseColor("#64748B"));

        TextView txtEstado = new TextView(this);

        if (leida.equals("si")) {
            txtEstado.setText("Notificación leída");
            txtEstado.setTextColor(Color.parseColor("#64748B"));
        } else {
            txtEstado.setText("Nueva notificación");
            txtEstado.setTextColor(Color.parseColor("#2563EB"));
        }

        txtEstado.setTextSize(14);
        txtEstado.setTypeface(null, android.graphics.Typeface.BOLD);
        txtEstado.setPadding(0, 12, 0, 0);

        card.addView(txtTitulo);
        card.addView(txtMensaje);
        card.addView(txtDetalle);
        card.addView(txtEstado);

        return card;
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