package com.example.clase4;

import java.util.List;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ApiService {

    @Multipart
    @POST("usuarios/registro")
    Call<ResponseBody> registroPaso1(
            @Part("nombre") RequestBody nombre,
            @Part("apellido") RequestBody apellido,
            @Part MultipartBody.Part dniFrente,
            @Part MultipartBody.Part dniDorso
    );

    @POST("auth/registro/paso1")
    Call<ResponseBody> registroPaso1(@Body RegistroRequest request);
}