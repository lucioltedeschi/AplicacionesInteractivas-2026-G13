package com.example.clase4;

import android.content.Intent;
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
import android.content.Intent;

public class HomeActivity extends AppCompatActivity {

    private TextView txtBienvenida;
    private TextView txtCategoria;
    private TextView txtMensajeHome;
    private Button btnActualizarSubastas;
    private LinearLayout contenedorSubastas;

    private Button btnMediosPago;
    private Button btnSolicitarSubasta;
    private Button btnHistorial;
    private Button btnPerfil;
    private Button btnNotificaciones;

    /*
     IMPORTANTE:
     Usá la misma IP que pusiste en LoginActivity.java.
    */

    private int userId;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        txtBienvenida = findViewById(R.id.txtBienvenida);
        txtCategoria = findViewById(R.id.txtCategoria);
        txtMensajeHome = findViewById(R.id.txtMensajeHome);
        btnActualizarSubastas = findViewById(R.id.btnActualizarSubastas);
        btnMediosPago = findViewById(R.id.btnMediosPago);
        btnSolicitarSubasta = findViewById(R.id.btnSolicitarSubasta);
        btnHistorial = findViewById(R.id.btnHistorial);
        btnPerfil = findViewById(R.id.btnPerfil);
        btnNotificaciones = findViewById(R.id.btnNotificaciones);
        contenedorSubastas = findViewById(R.id.contenedorSubastas);

        SharedPreferences preferences = getSharedPreferences("sesion", MODE_PRIVATE);

        userId = preferences.getInt("userId", 0);
        String nombre = preferences.getString("nombre", "");
        String apellido = preferences.getString("apellido", "");
        String categoria = preferences.getString("categoria", "");

        txtBienvenida.setText("Bienvenido, " + nombre + " " + apellido);
        txtCategoria.setText("Categoría: " + categoria);

        btnActualizarSubastas.setOnClickListener(v -> cargarSubastas());

        btnMediosPago.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, PaymentMethodsActivity.class);
            startActivity(intent);
        });

        btnSolicitarSubasta.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProductRequestActivity.class);
            startActivity(intent);
        });

        btnHistorial.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        btnPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        btnNotificaciones.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, NotificationsActivity.class);
            startActivity(intent);
        });

        cargarSubastas();
    }

    private void cargarSubastas() {
        txtMensajeHome.setText("Cargando subastas...");
        contenedorSubastas.removeAllViews();

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/clients/" + userId + "/auctions");
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
                    JSONArray subastas = new JSONArray(respuesta);

                    mainHandler.post(() -> mostrarSubastas(subastas));
                } else {
                    JSONObject errorJson = new JSONObject(respuesta);
                    String error = errorJson.optString("error", "Error al cargar subastas");

                    mainHandler.post(() -> txtMensajeHome.setText(error));
                }

            } catch (Exception e) {
                mainHandler.post(() -> txtMensajeHome.setText("No se pudo conectar con el servidor."));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void mostrarSubastas(JSONArray subastas) {
        contenedorSubastas.removeAllViews();

        if (subastas.length() == 0) {
            txtMensajeHome.setText("No hay subastas disponibles.");
            return;
        }

        txtMensajeHome.setText("Subastas encontradas: " + subastas.length());

        try {
            for (int i = 0; i < subastas.length(); i++) {
                JSONObject subasta = subastas.getJSONObject(i);

                int id = subasta.getInt("id");
                String fecha = subasta.optString("fecha", "-");
                String hora = subasta.optString("hora", "-");
                String estado = subasta.optString("estado", "-");
                String ubicacion = subasta.optString("ubicacion", "-");
                String categoria = subasta.optString("categoria", "-");
                String moneda = subasta.optString("moneda", "-");
                boolean puedePujar = subasta.optBoolean("puedePujar", false);
                String motivoBloqueo = subasta.optString("motivoBloqueo", "");

                View card = crearCardSubasta(
                        id,
                        fecha,
                        hora,
                        estado,
                        ubicacion,
                        categoria,
                        moneda,
                        puedePujar,
                        motivoBloqueo
                );

                contenedorSubastas.addView(card);
            }
        } catch (Exception e) {
            txtMensajeHome.setText("Error mostrando subastas.");
        }
    }

    private View crearCardSubasta(
            int id,
            String fecha,
            String hora,
            String estado,
            String ubicacion,
            String categoria,
            String moneda,
            boolean puedePujar,
            String motivoBloqueo
    ) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(28, 24, 28, 24);
        card.setBackgroundColor(Color.WHITE);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        params.setMargins(0, 0, 0, 22);
        card.setLayoutParams(params);
        card.setElevation(4);

        TextView titulo = new TextView(this);
        titulo.setText("Subasta #" + id + " - " + ubicacion);
        titulo.setTextSize(18);
        titulo.setTextColor(Color.parseColor("#0F172A"));
        titulo.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView detalle = new TextView(this);
        detalle.setText(
                "Fecha: " + fecha + "\n" +
                        "Hora: " + hora + "\n" +
                        "Estado: " + estado + "\n" +
                        "Categoría: " + categoria + "\n" +
                        "Moneda: " + moneda
        );
        detalle.setTextSize(15);
        detalle.setTextColor(Color.parseColor("#475569"));
        detalle.setPadding(0, 12, 0, 12);

        TextView permiso = new TextView(this);
        if (puedePujar) {
            permiso.setText("Habilitado para pujar");
            permiso.setTextColor(Color.parseColor("#16A34A"));
        } else {
            permiso.setText("Solo visualización: " + motivoBloqueo);
            permiso.setTextColor(Color.parseColor("#DC2626"));
        }
        permiso.setTextSize(14);
        permiso.setTypeface(null, android.graphics.Typeface.BOLD);

        Button btnVerDetalle = new Button(this);
        btnVerDetalle.setText("Ver detalle");
        btnVerDetalle.setBackgroundColor(Color.parseColor("#2563EB"));
        btnVerDetalle.setTextColor(Color.WHITE);

        btnVerDetalle.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AuctionDetailActivity.class);
            intent.putExtra("auctionId", id);
            intent.putExtra("puedePujar", puedePujar);
            startActivity(intent);
        });

        card.addView(titulo);
        card.addView(detalle);
        card.addView(permiso);
        card.addView(btnVerDetalle);

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