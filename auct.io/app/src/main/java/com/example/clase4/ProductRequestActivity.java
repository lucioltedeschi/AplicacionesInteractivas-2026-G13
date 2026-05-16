package com.example.clase4;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductRequestActivity extends AppCompatActivity {

    private EditText edtDescripcionCatalogo;
    private EditText edtDescripcionCompleta;
    private EditText edtHistoria;
    private EditText edtArtista;
    private CheckBox chkPropiedad;
    private CheckBox chkOrigenLicito;
    private TextView txtMensajeSolicitud;
    private Button btnEnviarSolicitud;
    private Button btnVolverSolicitud;

    private int userId;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_request);

        edtDescripcionCatalogo = findViewById(R.id.edtDescripcionCatalogo);
        edtDescripcionCompleta = findViewById(R.id.edtDescripcionCompleta);
        edtHistoria = findViewById(R.id.edtHistoria);
        edtArtista = findViewById(R.id.edtArtista);
        chkPropiedad = findViewById(R.id.chkPropiedad);
        chkOrigenLicito = findViewById(R.id.chkOrigenLicito);
        txtMensajeSolicitud = findViewById(R.id.txtMensajeSolicitud);
        btnEnviarSolicitud = findViewById(R.id.btnEnviarSolicitud);
        btnVolverSolicitud = findViewById(R.id.btnVolverSolicitud);

        SharedPreferences preferences = getSharedPreferences("sesion", MODE_PRIVATE);
        userId = preferences.getInt("userId", 0);

        btnEnviarSolicitud.setOnClickListener(v -> validarYEnviarSolicitud());
        btnVolverSolicitud.setOnClickListener(v -> finish());
    }

    private void validarYEnviarSolicitud() {
        String descripcionCatalogo = edtDescripcionCatalogo.getText().toString().trim();
        String descripcionCompleta = edtDescripcionCompleta.getText().toString().trim();
        String historia = edtHistoria.getText().toString().trim();
        String artista = edtArtista.getText().toString().trim();

        if (descripcionCatalogo.isEmpty()) {
            txtMensajeSolicitud.setText("Ingresá un título corto para el catálogo.");
            return;
        }

        if (descripcionCompleta.isEmpty()) {
            txtMensajeSolicitud.setText("Ingresá una descripción completa del artículo.");
            return;
        }

        if (!chkPropiedad.isChecked()) {
            txtMensajeSolicitud.setText("Debés declarar que el bien te pertenece.");
            return;
        }

        if (!chkOrigenLicito.isChecked()) {
            txtMensajeSolicitud.setText("Debés declarar el origen lícito del bien.");
            return;
        }

        txtMensajeSolicitud.setText("");
        btnEnviarSolicitud.setEnabled(false);
        btnEnviarSolicitud.setText("Enviando...");

        enviarSolicitud(descripcionCatalogo, descripcionCompleta, historia, artista);
    }

    private void enviarSolicitud(
            String descripcionCatalogo,
            String descripcionCompleta,
            String historia,
            String artista
    ) {
        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/products");
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("duenio", userId);
                body.put("descripcionCatalogo", descripcionCatalogo);
                body.put("descripcionCompleta", descripcionCompleta);
                body.put("historia", historia);
                body.put("artistaDiseniador", artista);
                body.put("declaracionPropiedad", "si");
                body.put("origenLicito", "si");

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int statusCode = connection.getResponseCode();

                InputStream inputStream;

                if (statusCode >= 200 && statusCode < 300) {
                    inputStream = connection.getInputStream();
                } else {
                    inputStream = connection.getErrorStream();
                }

                String respuesta = leerRespuesta(inputStream);
                JSONObject json = new JSONObject(respuesta);

                if (statusCode == 202 || statusCode == 201 || statusCode == 200) {
                    String mensaje = json.optString("mensaje", "Solicitud enviada correctamente");

                    mainHandler.post(() -> {
                        btnEnviarSolicitud.setEnabled(true);
                        btnEnviarSolicitud.setText("Enviar solicitud");
                        txtMensajeSolicitud.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        txtMensajeSolicitud.setText(mensaje);

                        limpiarFormulario();
                    });

                } else {
                    String error = json.optString("error", "No se pudo enviar la solicitud");

                    mainHandler.post(() -> {
                        btnEnviarSolicitud.setEnabled(true);
                        btnEnviarSolicitud.setText("Enviar solicitud");
                        txtMensajeSolicitud.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        txtMensajeSolicitud.setText(error);
                    });
                }

            } catch (Exception e) {
                mainHandler.post(() -> {
                    btnEnviarSolicitud.setEnabled(true);
                    btnEnviarSolicitud.setText("Enviar solicitud");
                    txtMensajeSolicitud.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    txtMensajeSolicitud.setText("No se pudo conectar con el servidor.");
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void limpiarFormulario() {
        edtDescripcionCatalogo.setText("");
        edtDescripcionCompleta.setText("");
        edtHistoria.setText("");
        edtArtista.setText("");
        chkPropiedad.setChecked(false);
        chkOrigenLicito.setChecked(false);
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