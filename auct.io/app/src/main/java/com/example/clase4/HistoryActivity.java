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

public class HistoryActivity extends AppCompatActivity {

    private TextView txtMensajeHistorial;
    private Button btnActualizarHistorial;
    private Button btnVolverHistorial;
    private LinearLayout contenedorHistorial;

    private int userId;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        txtMensajeHistorial = findViewById(R.id.txtMensajeHistorial);
        btnActualizarHistorial = findViewById(R.id.btnActualizarHistorial);
        btnVolverHistorial = findViewById(R.id.btnVolverHistorial);
        contenedorHistorial = findViewById(R.id.contenedorHistorial);

        SharedPreferences preferences = getSharedPreferences("sesion", MODE_PRIVATE);
        userId = preferences.getInt("userId", 0);

        btnActualizarHistorial.setOnClickListener(v -> cargarHistorial());
        btnVolverHistorial.setOnClickListener(v -> finish());

        cargarHistorial();
    }

    private void cargarHistorial() {
        txtMensajeHistorial.setText("Cargando historial...");
        contenedorHistorial.removeAllViews();

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/clients/" + userId + "/history");
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
                    JSONArray historial = new JSONArray(respuesta);
                    mainHandler.post(() -> mostrarHistorial(historial));
                } else {
                    JSONObject errorJson = new JSONObject(respuesta);
                    String error = errorJson.optString("error", "Error al cargar historial");
                    mainHandler.post(() -> txtMensajeHistorial.setText(error));
                }

            } catch (Exception e) {
                mainHandler.post(() -> txtMensajeHistorial.setText("No se pudo conectar con el servidor."));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void mostrarHistorial(JSONArray historial) {
        contenedorHistorial.removeAllViews();

        if (historial.length() == 0) {
            txtMensajeHistorial.setText("Todavía no tenés participaciones registradas.");
            return;
        }

        txtMensajeHistorial.setText("Participaciones encontradas: " + historial.length());

        try {
            for (int i = 0; i < historial.length(); i++) {
                JSONObject item = historial.getJSONObject(i);

                int subastaId = item.optInt("subastaId", 0);
                String fecha = item.optString("fecha", "-");
                String hora = item.optString("hora", "-");
                String moneda = item.optString("moneda", "-");
                String descripcionCatalogo = item.optString("descripcionCatalogo", "-");
                double mejorOfertaPropia = item.optDouble("mejorOfertaPropia", 0);
                int gano = item.optInt("gano", 0);

                View card = crearCardHistorial(
                        subastaId,
                        fecha,
                        hora,
                        moneda,
                        descripcionCatalogo,
                        mejorOfertaPropia,
                        gano
                );

                contenedorHistorial.addView(card);
            }

        } catch (Exception e) {
            txtMensajeHistorial.setText("Error mostrando historial.");
        }
    }

    private View crearCardHistorial(
            int subastaId,
            String fecha,
            String hora,
            String moneda,
            String descripcionCatalogo,
            double mejorOfertaPropia,
            int gano
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
        titulo.setText("Subasta #" + subastaId);
        titulo.setTextSize(18);
        titulo.setTextColor(Color.parseColor("#0F172A"));
        titulo.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView detalle = new TextView(this);
        detalle.setText(
                "Artículo: " + descripcionCatalogo + "\n" +
                        "Fecha: " + fecha + "\n" +
                        "Hora: " + hora + "\n" +
                        "Moneda: " + moneda + "\n" +
                        "Mejor oferta propia: $" + mejorOfertaPropia
        );

        detalle.setTextSize(15);
        detalle.setTextColor(Color.parseColor("#475569"));
        detalle.setPadding(0, 12, 0, 12);

        TextView estado = new TextView(this);

        if (gano == 1) {
            estado.setText("Resultado: Subasta ganada");
            estado.setTextColor(Color.parseColor("#16A34A"));
        } else {
            estado.setText("Resultado: Participación registrada");
            estado.setTextColor(Color.parseColor("#2563EB"));
        }

        estado.setTextSize(14);
        estado.setTypeface(null, android.graphics.Typeface.BOLD);

        card.addView(titulo);
        card.addView(detalle);
        card.addView(estado);

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