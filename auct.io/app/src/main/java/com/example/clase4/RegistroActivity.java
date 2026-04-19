package com.example.clase4;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegistroActivity extends AppCompatActivity {
    private EditText etDoc, etNom, etApe, etDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        etDoc = findViewById(R.id.etDocumento);
        etNom = findViewById(R.id.etNombre);
        etApe = findViewById(R.id.etApellido);
        etDir = findViewById(R.id.etDireccion);
        Button btn = findViewById(R.id.btnRegistrar);

        btn.setOnClickListener(v -> {
            String doc = etDoc.getText().toString();
            String nom = etNom.getText().toString();
            String ape = etApe.getText().toString();
            String dir = etDir.getText().toString();

            if(doc.isEmpty() || nom.isEmpty()) return;

            RegistroRequest request = new RegistroRequest(doc, nom, ape, dir);

            // Llamada al backend
            ApiClient.getClient().create(ApiService.class).registroPaso1(request).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(RegistroActivity.this, "User Saved in DB!", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(RegistroActivity.this, SubastasActivity.class));
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(RegistroActivity.this, "Server Error", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}