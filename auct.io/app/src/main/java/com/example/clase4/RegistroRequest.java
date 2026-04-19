package com.example.clase4;

public class RegistroRequest {
    String documento, nombre, apellido, direccion;
    public RegistroRequest(String d, String n, String a, String dir) {
        this.documento = d; this.nombre = n; this.apellido = a; this.direccion = dir;
    }
}