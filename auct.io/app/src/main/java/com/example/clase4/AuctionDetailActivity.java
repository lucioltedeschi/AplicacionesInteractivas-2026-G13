package com.example.clase4;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

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

public class AuctionDetailActivity extends AppCompatActivity {

    private TextView txtTituloDetalle;
    private TextView txtDatosSubasta;
    private TextView txtMensajeDetalle;
    private Button btnActualizarCatalogo;
    private Button btnVolverHome;
    private LinearLayout contenedorCatalogo;

    private int auctionId;
    private boolean puedePujar;

    /*
     IMPORTANTE:
     Usá la misma IP que pusiste en LoginActivity.java y HomeActivity.java.
    */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auction_detail);

        txtTituloDetalle = findViewById(R.id.txtTituloDetalle);
        txtDatosSubasta = findViewById(R.id.txtDatosSubasta);
        txtMensajeDetalle = findViewById(R.id.txtMensajeDetalle);
        btnActualizarCatalogo = findViewById(R.id.btnActualizarCatalogo);
        btnVolverHome = findViewById(R.id.btnVolverHome);
        contenedorCatalogo = findViewById(R.id.contenedorCatalogo);

        auctionId = getIntent().getIntExtra("auctionId", 0);
        puedePujar = getIntent().getBooleanExtra("puedePujar", false);

        txtTituloDetalle.setText("Detalle de subasta #" + auctionId);

        btnActualizarCatalogo.setOnClickListener(v -> {
            cargarDetalleSubasta();
            cargarCatalogo();
        });

        btnVolverHome.setOnClickListener(v -> finish());

        cargarDetalleSubasta();
        cargarCatalogo();
    }

    private void cargarDetalleSubasta() {
        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/auctions/" + auctionId);
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
                    JSONObject subasta = new JSONObject(respuesta);

                    mainHandler.post(() -> mostrarDetalleSubasta(subasta));
                } else {
                    JSONObject errorJson = new JSONObject(respuesta);
                    String error = errorJson.optString("error", "Error al cargar subasta");

                    mainHandler.post(() -> txtDatosSubasta.setText(error));
                }

            } catch (Exception e) {
                mainHandler.post(() -> txtDatosSubasta.setText("No se pudo conectar con el servidor."));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void mostrarDetalleSubasta(JSONObject subasta) {
        try {
            String fecha = subasta.optString("fecha", "-");
            String hora = subasta.optString("hora", "-");
            String estado = subasta.optString("estado", "-");
            String ubicacion = subasta.optString("ubicacion", "-");
            String categoria = subasta.optString("categoria", "-");
            String moneda = subasta.optString("moneda", "-");
            String subastador = subasta.optString("subastador", "-");

            String permiso = puedePujar
                    ? "Estado del usuario: habilitado para pujar"
                    : "Estado del usuario: solo visualización";

            txtDatosSubasta.setText(
                    "Ubicación: " + ubicacion + "\n" +
                            "Fecha: " + fecha + "\n" +
                            "Hora: " + hora + "\n" +
                            "Estado: " + estado + "\n" +
                            "Categoría: " + categoria + "\n" +
                            "Moneda: " + moneda + "\n" +
                            "Subastador: " + subastador + "\n\n" +
                            permiso
            );

        } catch (Exception e) {
            txtDatosSubasta.setText("Error mostrando datos de la subasta.");
        }
    }

    private void cargarCatalogo() {
        txtMensajeDetalle.setText("Cargando catálogo...");
        contenedorCatalogo.removeAllViews();

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(ApiConfig.BASE_URL + "/api/auctions/" + auctionId + "/catalog");
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
                    JSONArray catalogo = new JSONArray(respuesta);

                    mainHandler.post(() -> mostrarCatalogo(catalogo));
                } else {
                    JSONObject errorJson = new JSONObject(respuesta);
                    String error = errorJson.optString("error", "Error al cargar catálogo");

                    mainHandler.post(() -> txtMensajeDetalle.setText(error));
                }

            } catch (Exception e) {
                mainHandler.post(() -> txtMensajeDetalle.setText("No se pudo conectar con el servidor."));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void mostrarCatalogo(JSONArray catalogo) {
        contenedorCatalogo.removeAllViews();

        if (catalogo.length() == 0) {
            txtMensajeDetalle.setText("No hay ítems cargados para esta subasta.");
            return;
        }

        txtMensajeDetalle.setText("Ítems encontrados: " + catalogo.length());

        try {
            for (int i = 0; i < catalogo.length(); i++) {
                JSONObject item = catalogo.getJSONObject(i);

                int itemId = item.getInt("itemId");
                String descripcionCatalogo = item.optString("descripcionCatalogo", "-");
                String descripcionCompleta = item.optString("descripcionCompleta", "-");
                String historia = item.optString("historia", "");
                String artistaDiseniador = item.optString("artistaDiseniador", "");
                double precioBase = item.optDouble("precioBase", 0);
                double comision = item.optDouble("comision", 0);
                double mejorOferta = item.optDouble("mejorOferta", precioBase);
                String vendido = item.optString("vendido", "no");

                View card = crearCardCatalogo(
                        itemId,
                        descripcionCatalogo,
                        descripcionCompleta,
                        historia,
                        artistaDiseniador,
                        precioBase,
                        comision,
                        mejorOferta,
                        vendido
                );

                contenedorCatalogo.addView(card);
            }

        } catch (Exception e) {
            txtMensajeDetalle.setText("Error mostrando catálogo.");
        }
    }

    private View crearCardCatalogo(
            int itemId,
            String descripcionCatalogo,
            String descripcionCompleta,
            String historia,
            String artistaDiseniador,
            double precioBase,
            double comision,
            double mejorOferta,
            String vendido
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
        titulo.setText(descripcionCatalogo);
        titulo.setTextSize(18);
        titulo.setTextColor(Color.parseColor("#0F172A"));
        titulo.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView detalle = new TextView(this);

        String textoDetalle =
                "Ítem: #" + itemId + "\n" +
                        "Descripción: " + descripcionCompleta + "\n" +
                        "Precio base: $" + precioBase + "\n" +
                        "Mejor oferta: $" + mejorOferta + "\n" +
                        "Comisión: $" + comision + "\n" +
                        "Vendido: " + vendido;

        if (artistaDiseniador != null && !artistaDiseniador.equals("") && !artistaDiseniador.equals("null")) {
            textoDetalle += "\nArtista/Diseñador: " + artistaDiseniador;
        }

        if (historia != null && !historia.equals("") && !historia.equals("null")) {
            textoDetalle += "\nHistoria: " + historia;
        }

        detalle.setText(textoDetalle);
        detalle.setTextSize(15);
        detalle.setTextColor(Color.parseColor("#475569"));
        detalle.setPadding(0, 12, 0, 12);

        Button btnPujar = new Button(this);

        if (puedePujar && !vendido.equals("si")) {
            btnPujar.setText("Pujar por este ítem");
            btnPujar.setBackgroundColor(Color.parseColor("#2563EB"));
            btnPujar.setTextColor(Color.WHITE);
            btnPujar.setEnabled(true);

            btnPujar.setOnClickListener(v -> {
                Intent intent = new Intent(AuctionDetailActivity.this, BidActivity.class);
                intent.putExtra("auctionId", auctionId);
                intent.putExtra("itemId", itemId);
                intent.putExtra("descripcion", descripcionCatalogo);
                intent.putExtra("precioBase", precioBase);
                intent.putExtra("mejorOferta", mejorOferta);
                startActivity(intent);
            });

        } else {
            btnPujar.setText("No habilitado para pujar");
            btnPujar.setBackgroundColor(Color.parseColor("#CBD5E1"));
            btnPujar.setTextColor(Color.parseColor("#475569"));
            btnPujar.setEnabled(false);
        }

        card.addView(titulo);
        card.addView(detalle);
        card.addView(btnPujar);

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