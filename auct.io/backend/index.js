const express = require('express');
const cors = require('cors');
const { poolPromise, sql } = require('./db');

const app = express();
app.use(cors());
app.use(express.json());

// ==========================================
// 1. AUTENTICACIÓN & USUARIOS (KYC)
// ==========================================

// Paso 1: Registro Inicial (Pendiente de Clave)
app.post('/auth/registro/paso1', async (req, res) => {
    try {
        const { documento, nombre, apellido, direccion } = req.body;
        const pool = await poolPromise;
        
        await pool.request()
            .input('doc', sql.VarChar, documento)
            .input('nom', sql.VarChar, nombre)
            .input('ape', sql.VarChar, apellido)
            .input('dir', sql.VarChar, direccion)
            .query(`INSERT INTO Users (documento, nombre, apellido, direccion, estado) 
                    VALUES (@doc, @nom, @ape, @dir, 'activo')`);

        res.status(201).json({ message: "Registro inicial exitoso. Pendiente de verificación." });
    } catch (err) {
        res.status(400).json({ error: err.message });
    }
});

// Paso 2: Generación de Clave
app.post('/auth/registro/paso2', async (req, res) => {
    try {
        const { documento, clave } = req.body;
        const pool = await poolPromise;
        
        const result = await pool.request()
            .input('doc', sql.Int, documento)
            .input('pass', sql.VarChar, clave)
            .query(`UPDATE Users SET clave = @pass WHERE documento = @doc`);

        if (result.rowsAffected[0] === 0) return res.status(404).json({ error: "Usuario no encontrado" });
        res.status(200).json({ message: "Clave generada exitosamente" });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Listar todos los usuarios
app.get('/usuarios', async (req, res) => {
    try {
        const pool = await poolPromise;
        const result = await pool.request()
            .query("SELECT identificador, documento, nombre, apellido, direccion, estado FROM Users");
        
        res.status(200).json(result.recordset);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Perfil del Usuario
app.get('/usuarios/:id/perfil', async (req, res) => {
    try {
        const pool = await poolPromise;
        const result = await pool.request()
            .input('id', sql.Int, req.params.id)
            .query("SELECT identificador, nombre, apellido, estado FROM Users WHERE identificador = @id");
        
        if (result.recordset.length === 0) return res.status(404).json({ error: "Usuario no encontrado" });
        res.status(200).json(result.recordset[0]);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});


// ==========================================
// 2. SUBASTAS & CATÁLOGO
// ==========================================

// Listar todas las subastas disponibles
app.get('/subastas', async (req, res) => {
    try {
        const pool = await poolPromise;
        const result = await pool.request().query("SELECT * FROM Auctions");
        res.status(200).json(result.recordset);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Catálogo de una Subasta (Ítems y Precios Base)
app.get('/subastas/:id/items', async (req, res) => {
    try {
        const pool = await poolPromise;
        const result = await pool.request()
            .input('subastaId', sql.Int, req.params.id)
            .query(`
                SELECT ci.identificador as catalogItemId, p.descripcionCompleta, ci.precioBase
                FROM CatalogItems ci
                INNER JOIN Catalogs c ON ci.catalogo = c.identificador
                INNER JOIN Products p ON ci.producto = p.identificador
                WHERE c.subasta = @subastaId
            `);
        
        res.status(200).json(result.recordset);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});


// ==========================================
// 3. PUJAS (BIDS)
// ==========================================

// Historial de Pujas de una subasta específica
app.get('/subastas/:id/pujas', async (req, res) => {
    try {
        const pool = await poolPromise;
        const result = await pool.request()
            .input('subastaId', sql.Int, req.params.id)
            .query(`
                SELECT b.identificador, b.importe, b.fechaHora, u.nombre as Postor
                FROM Bids b
                INNER JOIN Attendees a ON b.asistente = a.identificador
                INNER JOIN Clients c ON a.cliente = c.identificador
                INNER JOIN Users u ON c.identificador = u.identificador
                INNER JOIN CatalogItems ci ON b.item = ci.identificador
                INNER JOIN Catalogs cat ON ci.catalogo = cat.identificador
                WHERE cat.subasta = @subastaId
                ORDER BY b.fechaHora DESC
            `);
        
        res.status(200).json(result.recordset);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Realizar una nueva puja
app.post('/pujas', async (req, res) => {
    try {
        const { asistenteId, itemId, importe } = req.body;
        const pool = await poolPromise;

        // 1. Obtener precio base y categoría (Para la regla del 1% y 20%)
        const itemData = await pool.request()
            .input('itemId', sql.Int, itemId)
            .query(`
                SELECT ci.precioBase, a.categoria 
                FROM CatalogItems ci
                INNER JOIN Catalogs c ON ci.catalogo = c.identificador
                INNER JOIN Auctions a ON c.subasta = a.identificador
                WHERE ci.identificador = @itemId
            `);

        if (itemData.recordset.length === 0) {
            return res.status(404).json({ error: "Ítem no encontrado en el catálogo" });
        }
        
        const { precioBase, categoria } = itemData.recordset[0];

        // 2. Obtener la mayor oferta actual
        const maxBidData = await pool.request()
            .input('itemId', sql.Int, itemId)
            .query(`SELECT ISNULL(MAX(importe), 0) as mayorOferta FROM Bids WHERE item = @itemId`);
        
        const mayorOferta = maxBidData.recordset[0].mayorOferta;
        const valorReferencia = mayorOferta > 0 ? mayorOferta : precioBase;

        // 3. Validación de Negocio (El famoso 1% y 20%)
        if (categoria !== 'oro' && categoria !== 'platino') {
            const minimoPermitido = valorReferencia + (precioBase * 0.01);
            const maximoPermitido = valorReferencia + (precioBase * 0.20);

            if (importe < minimoPermitido) {
                return res.status(400).json({ error: `La puja debe ser de al menos $${minimoPermitido.toFixed(2)}` });
            }
            if (importe > maximoPermitido) {
                return res.status(400).json({ error: `La puja máxima permitida es $${maximoPermitido.toFixed(2)}` });
            }
        } else {
            // Regla VIP: Solo debe superar la oferta actual
            if (importe <= valorReferencia) {
                return res.status(400).json({ error: `La puja debe ser mayor a $${valorReferencia.toFixed(2)}` });
            }
        }

        // 4. Insertar la Puja
        await pool.request()
            .input('asistente', sql.Int, asistenteId)
            .input('item', sql.Int, itemId)
            .input('monto', sql.Decimal(18, 2), importe)
            .query(`INSERT INTO Bids (asistente, item, importe, fechaHora, ganador) 
                    VALUES (@asistente, @item, @monto, GETDATE(), 'no')`);

        res.status(201).json({ message: "Puja registrada correctamente" });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});


// ==========================================
// 4. CONSIGNACIÓN DE ARTÍCULOS
// ==========================================

app.post('/productos/consignar', async (req, res) => {
    try {
        const { duenioId, descripcion } = req.body;
        
        const pool = await poolPromise;
        await pool.request()
            .input('duenio', sql.Int, duenioId)
            .input('desc', sql.VarChar, descripcion)
            .query(`INSERT INTO Products (duenio, descripcionCompleta, estadoAprobacion, disponible, declaracionPropiedad, origenLicito) 
                    VALUES (@duenio, @desc, 'pendiente', 'no', 'si', 'si')`);

        res.status(202).json({ message: "Solicitud de consignación en revisión" });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ==========================================
// INICIO DEL SERVIDOR
// ==========================================
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`🚀 API Subastas corriendo en http://localhost:${PORT}`);
});