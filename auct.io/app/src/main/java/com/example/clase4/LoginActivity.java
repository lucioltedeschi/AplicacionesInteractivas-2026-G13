package com.example.clase4;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
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

public class LoginActivity extends AppCompatActivity {

    private EditText edtDocumento;
    private EditText edtClave;
    private TextView txtMensaje;
    private Button btnIngresar;

    /*
     IMPORTANTE:
     Reemplazá esta IP por la IP de tu PC.
     Para verla, abrí CMD y ejecutá: ipconfig
     Buscá "Dirección IPv4".
    */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtDocumento = findViewById(R.id.edtDocumento);
        edtClave = findViewById(R.id.edtClave);
        txtMensaje = findViewById(R.id.txtMensaje);
        btnIngresar = findViewById(R.id.btnIngresar);

        btnIngresar.setOnClickListener(v -> validarLogin());
    }

    private void validarLogin() {
        String documento = edtDocumento.getText().toString().trim();
        String clave = edtClave.getText().toString().trim();

        if (documento.isEmpty()) {
            txtMensaje.setText("Ingresá el documento.");
            return;
        }

        if (clave.isEmpty()) {
            txtMensaje.setText("Ingresá la clave.");
            return;
        }

        txtMensaje.setText("");
        btnIngresar.setEnabled(false);
        btnIngresar.setText("Ingresando...");

        hacerLogin(documento, clave);
    }

    private void hacerLogin(String documento, String clave) {
        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/auth/login");
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("documento", documento);
                body.put("clave", clave);

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

                if (statusCode == 200) {
                    JSONObject usuario = json.getJSONObject("usuario");

                    int userId = usuario.getInt("id");
                    String nombre = usuario.getString("nombre");
                    String apellido = usuario.getString("apellido");
                    String categoria = usuario.getString("categoria");

                    guardarSesion(userId, nombre, apellido, categoria);

                    mainHandler.post(() -> {
                        btnIngresar.setEnabled(true);
                        btnIngresar.setText("Ingresar");

                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        startActivity(intent);
                        finish();
                    });

                } else {
                    String error = json.optString("error", "Error al iniciar sesión");

                    mainHandler.post(() -> {
                        btnIngresar.setEnabled(true);
                        btnIngresar.setText("Ingresar");
                        txtMensaje.setText(error);
                    });
                }

            } catch (Exception e) {
                mainHandler.post(() -> {
                    btnIngresar.setEnabled(true);
                    btnIngresar.setText("Ingresar");
                    txtMensaje.setText("No se pudo conectar con el servidor.");
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
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

    private void guardarSesion(int userId, String nombre, String apellido, String categoria) {
        SharedPreferences preferences = getSharedPreferences("sesion", MODE_PRIVATE);

        preferences.edit()
                .putInt("userId", userId)
                .putString("nombre", nombre)
                .putString("apellido", apellido)
                .putString("categoria", categoria)
                .apply();
    }
}