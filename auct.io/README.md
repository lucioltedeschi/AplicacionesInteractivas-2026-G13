# AUCT.IO - Premier Auction House App

AUCT.IO es una plataforma de subastas de lujo diseñada para conectar artefactos excepcionales con coleccionistas modernos. Este repositorio contiene el ecosistema completo: Base de Datos, Backend (API) y Frontend (Android).

## 📂 Estructura del Proyecto

El repositorio está organizado de la siguiente manera:

* **`/app`**: Proyecto principal de Android Studio (Java). Contiene la lógica del front-end.
    * Ruta clave: `app/src/main/java/com/example/clase4` (o tu nombre de paquete).
* **`/backend`**: Servidor API REST desarrollado en Node.js.
    * Archivo principal: `index.js`.
* **`/database`**: Scripts de configuración de datos.
    * Archivo: `script.txt` (Contiene la creación de tablas y procedimientos).
* **`/docs`**: Documentación visual y de negocio.
    * `PantallasHD.pdf`: Diseños de alta resolución.
    * `Presentacion.pdf`: Concepto y arquitectura.

---

## 🛠️ Requisitos Previos

Antes de empezar, asegúrate de tener instalado:
1.  **Node.js** (v14 o superior).
2.  **SQL Server** (o el motor de base de datos que utilices).
3.  **Android Studio** (Koala o superior).

---

## 🚀 Guía de Instalación (Paso a Paso)

### 1. Configurar la Base de Datos
1.  Navega a la carpeta `/database`.
2.  Abre el archivo `script.txt`.
3.  Copia y ejecuta el contenido en tu gestor de base de datos para crear la estructura de **Auct.io**.

### 2. Levantar el Backend
1.  Abre una terminal en la carpeta `/backend`.
2.  Instala las dependencias necesarias:
    ```bash
    npm install
    ```
3.  Inicia el servidor:
    ```bash
    node index.js
    ```
    *Nota: El servidor correrá por defecto en el puerto 3000.*

### 3. Configurar el Frontend (Android)
1.  Abre **Android Studio**.
2.  Importa la carpeta `/app`.
3.  **Importante:** Si usas un emulador, asegúrate de que la URL base de tu API en el código Java apunte a `http://10.0.2.2:3000`. Si usas un celular físico, usa la IP local de tu PC.
4.  Haz un **Sync Project with Gradle Files** y dale a **Run**.

---

## 🎨 Identidad Visual
El proyecto sigue estrictamente el manual de marca definido en la documentación:
* **Primary Navy:** `#0A2647`
* **Auct Gold:** `#C5A059`
* **Neutral Background:** `#F8F9FA`

---

## 📝 Notas de Desarrollo
* **KYC (Know Your Customer):** La pantalla de registro implementa un flujo de validación de identidad de 3 pasos (Cuenta, Identidad, Finalización).
* **Seguridad:** Se utiliza cifrado para las transacciones de subastas de alto valor.

---
**Desarrollado por:** Lucio Leonardo Tedeschi Pontiroli