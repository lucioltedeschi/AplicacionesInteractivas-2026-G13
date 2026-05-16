const express = require("express");
const cors = require("cors");
const { poolPromise, sql } = require("./db");

const app = express();

app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 3000;

/* ============================================================
   FUNCIONES AUXILIARES
   ============================================================ */

function categoriaValor(categoria) {
  const orden = {
    comun: 1,
    especial: 2,
    plata: 3,
    oro: 4,
    platino: 5,
  };

  return orden[categoria] || 0;
}

/* ============================================================
   TEST DE API
   ============================================================ */

app.get("/api/test", async (req, res) => {
  try {
    const pool = await poolPromise;
    const result = await pool.request().query("SELECT GETDATE() AS fechaServidor");

    res.status(200).json({
      ok: true,
      mensaje: "API conectada correctamente",
      fechaServidor: result.recordset[0].fechaServidor,
    });
  } catch (err) {
    res.status(500).json({
      ok: false,
      error: err.message,
    });
  }
});

/* ============================================================
   AUTH / LOGIN
   ============================================================ */

app.post("/api/auth/login", async (req, res) => {
  try {
    const { documento, clave } = req.body;

    if (!documento || !clave) {
      return res.status(400).json({
        error: "Debe ingresar documento y clave",
      });
    }

    const pool = await poolPromise;

    const result = await pool
      .request()
      .input("documento", sql.VarChar, documento)
      .input("clave", sql.VarChar, clave)
      .query(`
        SELECT
          u.identificador AS id,
          u.documento,
          u.nombre,
          u.apellido,
          u.email,
          u.estado,
          c.admitido,
          c.categoria
        FROM Users u
        INNER JOIN Clients c
          ON u.identificador = c.identificador
        WHERE u.documento = @documento
          AND u.clave = @clave
      `);

    if (result.recordset.length === 0) {
      return res.status(401).json({
        error: "Documento o clave incorrecta",
      });
    }

    const usuario = result.recordset[0];

    if (usuario.estado !== "activo") {
      return res.status(403).json({
        error: "El usuario no se encuentra activo",
      });
    }

    if (usuario.admitido !== "si") {
      return res.status(403).json({
        error: "El usuario todavía no fue admitido para participar",
      });
    }

    res.status(200).json({
      mensaje: "Login correcto",
      usuario,
    });
  } catch (err) {
    res.status(500).json({
      error: err.message,
    });
  }
});

/* ============================================================
   USUARIOS / PERFIL
   ============================================================ */

app.get("/api/users/:userId", async (req, res) => {
  try {
    const pool = await poolPromise;

    const result = await pool
      .request()
      .input("userId", sql.Int, req.params.userId)
      .query(`
        SELECT
          u.identificador AS id,
          u.documento,
          u.nombre,
          u.apellido,
          u.email,
          u.telefono,
          u.direccion,
          u.estado,
          c.admitido,
          c.categoria
        FROM Users u
        LEFT JOIN Clients c
          ON u.identificador = c.identificador
        WHERE u.identificador = @userId
      `);

    if (result.recordset.length === 0) {
      return res.status(404).json({
        error: "Usuario no encontrado",
      });
    }

    res.status(200).json(result.recordset[0]);
  } catch (err) {
    res.status(500).json({
      error: err.message,
    });
  }
});

/* ============================================================
   SUBASTAS
   ============================================================ */

app.get("/api/auctions", async (req, res) => {
  try {
    const pool = await poolPromise;

    const result = await pool.request().query(`
      SELECT
        a.identificador AS id,
        a.fecha,
        a.hora,
        a.estado,
        a.ubicacion,
        a.capacidadAsistentes,
        a.tieneDeposito,
        a.seguridadPropia,
        a.categoria,
        a.moneda,
        u.nombre + ' ' + u.apellido AS subastador
      FROM Auctions a
      LEFT JOIN Auctioneers au
        ON a.subastador = au.identificador
      LEFT JOIN Users u
        ON au.identificador = u.identificador
      ORDER BY a.fecha, a.hora
    `);

    res.status(200).json(result.recordset);
  } catch (err) {
    res.status(500).json({
      error: err.message,
    });
  }
});

app.get("/api/clients/:clientId/auctions", async (req, res) => {
  try {
    const clientId = Number(req.params.clientId);
    const pool = await poolPromise;

    const clientResult = await pool
      .request()
      .input("clientId", sql.Int, clientId)
      .query(`
        SELECT
          u.estado,
          c.admitido,
          c.categoria,
          (
            SELECT COUNT(*)
            FROM PaymentMethods pm
            WHERE pm.cliente = c.identificador
              AND pm.verificado = 'si'
          ) AS mediosPagoVerificados
        FROM Clients c
        INNER JOIN Users u
          ON c.identificador = u.identificador
        WHERE c.identificador = @clientId
      `);

    if (clientResult.recordset.length === 0) {
      return res.status(404).json({
        error: "Cliente no encontrado",
      });
    }

    const cliente = clientResult.recordset[0];

    const auctionsResult = await pool.request().query(`
      SELECT
        a.identificador AS id,
        a.fecha,
        a.hora,
        a.estado,
        a.ubicacion,
        a.categoria,
        a.moneda
      FROM Auctions a
      WHERE a.estado IN ('abierta', 'en_curso', 'programada')
      ORDER BY a.fecha, a.hora
    `);

    const subastas = auctionsResult.recordset.map((subasta) => {
      const categoriaOk =
        categoriaValor(cliente.categoria) >= categoriaValor(subasta.categoria);

      const puedeVer =
        cliente.estado === "activo" && cliente.admitido === "si";

      const puedePujar =
        puedeVer && categoriaOk && cliente.mediosPagoVerificados > 0;

      return {
        ...subasta,
        puedeVer,
        puedePujar,
        motivoBloqueo: puedePujar
          ? null
          : !puedeVer
          ? "Usuario no activo o no admitido"
          : !categoriaOk
          ? "Categoría insuficiente"
          : "No posee medio de pago verificado",
      };
    });

    res.status(200).json(subastas);
  } catch (err) {
    res.status(500).json({
      error: err.message,
    });
  }
});

app.get("/api/auctions/:auctionId", async (req, res) => {
  try {
    const pool = await poolPromise;

    const result = await pool
      .request()
      .input("auctionId", sql.Int, req.params.auctionId)
      .query(`
        SELECT
          a.identificador AS id,
          a.fecha,
          a.hora,
          a.estado,
          a.ubicacion,
          a.capacidadAsistentes,
          a.tieneDeposito,
          a.seguridadPropia,
          a.categoria,
          a.moneda,
          u.nombre + ' ' + u.apellido AS subastador
        FROM Auctions a
        LEFT JOIN Auctioneers au
          ON a.subastador = au.identificador
        LEFT JOIN Users u
          ON au.identificador = u.identificador
        WHERE a.identificador = @auctionId
      `);

    if (result.recordset.length === 0) {
      return res.status(404).json({
        error: "Subasta no encontrada",
      });
    }

    res.status(200).json(result.recordset[0]);
  } catch (err) {
    res.status(500).json({
      error: err.message,
    });
  }
});

/* ============================================================
   CATÁLOGO
   ============================================================ */

app.get("/api/auctions/:auctionId/catalog", async (req, res) => {
  try {
    const pool = await poolPromise;

    const result = await pool
      .request()
      .input("auctionId", sql.Int, req.params.auctionId)
      .query(`
        SELECT
          ci.identificador AS itemId,
          p.identificador AS productoId,
          p.descripcionCatalogo,
          p.descripcionCompleta,
          p.historia,
          p.artistaDiseniador,
          p.fechaObjeto,
          ci.precioBase,
          ci.comision,
          ci.subastado,
          ci.vendido,
          ISNULL(MAX(b.importe), ci.precioBase) AS mejorOferta
        FROM Catalogs c
        INNER JOIN CatalogItems ci
          ON c.identificador = ci.catalogo
        INNER JOIN Products p
          ON ci.producto = p.identificador
        LEFT JOIN Bids b
          ON ci.identificador = b.item
        WHERE c.subasta = @auctionId
        GROUP BY
          ci.identificador,
          p.identificador,
          p.descripcionCatalogo,
          p.descripcionCompleta,
          p.historia,
          p.artistaDiseniador,
          p.fechaObjeto,
          ci.precioBase,
          ci.comision,
          ci.subastado,
          ci.vendido
        ORDER BY ci.identificador
      `);

    res.status(200).json(result.recordset);
  } catch (err) {
    res.status(500).json({
      error: err.message,
    });
  }
});

app.get("/api/catalog-items/:itemId/best-bid", async (req, res) => {
  try {
    const pool = await poolPromise;

    const result = await pool
      .request()
      .input("itemId", sql.Int, req.params.itemId)
      .query(`
        SELECT
          ci.identificador AS itemId,
          ci.precioBase,
          a.moneda,
          ISNULL(MAX(b.importe), ci.precioBase) AS mejorOferta
        FROM CatalogItems ci
        INNER JOIN Catalogs c
          ON ci.catalogo = c.identificador
        INNER JOIN Auctions a
          ON c.subasta = a.identificador
        LEFT JOIN Bids b
          ON ci.identificador = b.item
        WHERE ci.identificador = @itemId
        GROUP BY
          ci.identificador,
          ci.precioBase,
          a.moneda
      `);

    if (result.recordset.length === 0) {
      return res.status(404).json({
        error: "Ítem no encontrado",
      });
    }

    res.status(200).json(result.recordset[0]);
  } catch (err) {
    res.status(500).json({
      error: err.message,
    });
  }
});

app.get("/api/catalog-items/:itemId/bids", async (req, res) => {
  try {
    const pool = await poolPromise;

    const result = await pool
      .request()
      .input("itemId", sql.Int, req.params.itemId)
      .query(`
        SELECT
          b.identificador AS bidId,
          b.importe,
          b.fechaHora,
          b.ganador,
          u.nombre + ' ' + u.apellido AS postor
        FROM Bids b
        INNER JOIN Attendees a
          ON b.asistente = a.identificador
        INNER JOIN Users u
          ON a.cliente = u.identificador
        WHERE b.item = @itemId
        ORDER BY b.fechaHora DESC
      `);

    res.status(200).json(result.recordset);
  } catch (err) {
    res.status(500).json({
      error: err.message,
    });
  }
});

/* ============================================================
   PUJAS
   ============================================================ */

app.post("/api/bids", async (req, res) => {
  try {
    const { clienteId, subastaId, itemId, importe } = req.body;

    if (!clienteId || !subastaId || !itemId || !importe) {
      return res.status(400).json({
        error: "Debe enviar clienteId, subastaId, itemId e importe",
      });
    }

    const pool = await poolPromise;

    const clientResult = await pool
      .request()
      .input("clienteId", sql.Int, clienteId)
      .query(`
        SELECT
          u.estado,
          c.admitido,
          c.categoria,
          (
            SELECT COUNT(*)
            FROM PaymentMethods pm
            WHERE pm.cliente = c.identificador
              AND pm.verificado = 'si'
          ) AS mediosPagoVerificados
        FROM Clients c
        INNER JOIN Users u
          ON c.identificador = u.identificador
        WHERE c.identificador = @clienteId
      `);

    if (clientResult.recordset.length === 0) {
      return res.status(404).json({
        error: "Cliente no encontrado",
      });
    }

    const cliente = clientResult.recordset[0];

    if (cliente.estado !== "activo" || cliente.admitido !== "si") {
      return res.status(403).json({
        error: "Cliente no activo o no admitido",
      });
    }

    if (cliente.mediosPagoVerificados <= 0) {
      return res.status(403).json({
        error: "El cliente no posee medios de pago verificados",
      });
    }

    const itemResult = await pool
      .request()
      .input("subastaId", sql.Int, subastaId)
      .input("itemId", sql.Int, itemId)
      .query(`
        SELECT
          ci.identificador AS itemId,
          ci.precioBase,
          ci.vendido,
          a.identificador AS subastaId,
          a.estado,
          a.categoria,
          a.moneda
        FROM CatalogItems ci
        INNER JOIN Catalogs c
          ON ci.catalogo = c.identificador
        INNER JOIN Auctions a
          ON c.subasta = a.identificador
        WHERE ci.identificador = @itemId
          AND a.identificador = @subastaId
      `);

    if (itemResult.recordset.length === 0) {
      return res.status(404).json({
        error: "Ítem o subasta no encontrados",
      });
    }

    const item = itemResult.recordset[0];

    if (item.estado !== "abierta" && item.estado !== "en_curso") {
      return res.status(403).json({
        error: "La subasta no está abierta para recibir pujas",
      });
    }

    if (item.vendido === "si") {
      return res.status(409).json({
        error: "El ítem ya fue vendido",
      });
    }

    if (categoriaValor(cliente.categoria) < categoriaValor(item.categoria)) {
      return res.status(403).json({
        error: "La categoría del cliente no permite pujar en esta subasta",
      });
    }

    const maxBidResult = await pool
      .request()
      .input("itemId", sql.Int, itemId)
      .query(`
        SELECT ISNULL(MAX(importe), 0) AS mayorOferta
        FROM Bids
        WHERE item = @itemId
      `);

    const mayorOferta = Number(maxBidResult.recordset[0].mayorOferta);
    const precioBase = Number(item.precioBase);
    const importeNumerico = Number(importe);
    const valorReferencia = mayorOferta > 0 ? mayorOferta : precioBase;

    if (importeNumerico <= valorReferencia) {
      return res.status(400).json({
        error: `La puja debe ser mayor a ${valorReferencia.toFixed(2)}`,
      });
    }

    if (item.categoria !== "oro" && item.categoria !== "platino") {
      const minimoPermitido = valorReferencia + precioBase * 0.01;
      const maximoPermitido = valorReferencia + precioBase * 0.2;

      if (importeNumerico < minimoPermitido) {
        return res.status(400).json({
          error: `La puja mínima permitida es ${minimoPermitido.toFixed(2)}`,
        });
      }

      if (importeNumerico > maximoPermitido) {
        return res.status(400).json({
          error: `La puja máxima permitida es ${maximoPermitido.toFixed(2)}`,
        });
      }
    }

    let attendeeResult = await pool
      .request()
      .input("clienteId", sql.Int, clienteId)
      .input("subastaId", sql.Int, subastaId)
      .query(`
        SELECT identificador
        FROM Attendees
        WHERE cliente = @clienteId
          AND subasta = @subastaId
      `);

    let asistenteId;

    if (attendeeResult.recordset.length > 0) {
      asistenteId = attendeeResult.recordset[0].identificador;
    } else {
      const numeroPostor = Math.floor(Math.random() * 9000) + 1000;

      const insertAttendee = await pool
        .request()
        .input("numeroPostor", sql.Int, numeroPostor)
        .input("clienteId", sql.Int, clienteId)
        .input("subastaId", sql.Int, subastaId)
        .query(`
          INSERT INTO Attendees (numeroPostor, cliente, subasta)
          OUTPUT INSERTED.identificador
          VALUES (@numeroPostor, @clienteId, @subastaId)
        `);

      asistenteId = insertAttendee.recordset[0].identificador;
    }

    const insertBid = await pool
      .request()
      .input("asistenteId", sql.Int, asistenteId)
      .input("itemId", sql.Int, itemId)
      .input("importe", sql.Decimal(18, 2), importeNumerico)
      .query(`
        INSERT INTO Bids (asistente, item, importe, fechaHora, ganador)
        OUTPUT INSERTED.identificador, INSERTED.importe, INSERTED.fechaHora
        VALUES (@asistenteId, @itemId, @importe, GETDATE(), 'no')
      `);

    res.status(201).json({
      mensaje: "Puja registrada correctamente",
      puja: insertBid.recordset[0],
    });
  } catch (err) {
    res.status(500).json({
      error: err.message,
    });
  }
});

/* ============================================================
   MEDIOS DE PAGO
   ============================================================ */

app.get("/api/clients/:clientId/payment-methods", async (req, res) => {
  try {
    const pool = await poolPromise;

    const result = await pool
      .request()
      .input("clientId", sql.Int, req.params.clientId)
      .query(`
        SELECT
          identificador AS id,
          tipo,
          entidad,
          numeroReferencia,
          esExtranjera,
          moneda,
          verificado,
          montoCheque,
          montoDisponible
        FROM PaymentMethods
        WHERE cliente = @clientId
        ORDER BY identificador DESC
      `);

    res.status(200).json(result.recordset);
  } catch (err) {
    res.status(500).json({
      error: err.message,
    });
  }
});

/* ============================================================
   PRODUCTOS PARA CONSIGNACIÓN
   ============================================================ */

app.post("/api/products", async (req, res) => {
  try {
    const {
      duenio,
      descripcionCatalogo,
      descripcionCompleta,
      historia,
      artistaDiseniador,
      fechaObjeto,
      declaracionPropiedad,
      origenLicito,
    } = req.body;

    if (!duenio || !descripcionCompleta || declaracionPropiedad !== "si") {
      return res.status(400).json({
        error: "Debe indicar dueño, descripción y declaración de propiedad",
      });
    }

    const pool = await poolPromise;

    const result = await pool
      .request()
      .input("duenio", sql.Int, duenio)
      .input("descripcionCatalogo", sql.VarChar, descripcionCatalogo || "")
      .input("descripcionCompleta", sql.VarChar, descripcionCompleta)
      .input("historia", sql.VarChar, historia || null)
      .input("artistaDiseniador", sql.VarChar, artistaDiseniador || null)
      .input("fechaObjeto", sql.Date, fechaObjeto || null)
      .input("declaracionPropiedad", sql.VarChar, declaracionPropiedad)
      .input("origenLicito", sql.VarChar, origenLicito || "si")
      .query(`
        INSERT INTO Products (
          fecha,
          disponible,
          descripcionCatalogo,
          descripcionCompleta,
          historia,
          artistaDiseniador,
          fechaObjeto,
          declaracionPropiedad,
          origenLicito,
          estadoAprobacion,
          duenio
        )
        OUTPUT INSERTED.identificador, INSERTED.estadoAprobacion
        VALUES (
          CAST(GETDATE() AS DATE),
          'no',
          @descripcionCatalogo,
          @descripcionCompleta,
          @historia,
          @artistaDiseniador,
          @fechaObjeto,
          @declaracionPropiedad,
          @origenLicito,
          'pendiente',
          @duenio
        )
      `);

    res.status(202).json({
      mensaje: "Artículo enviado para revisión",
      producto: result.recordset[0],
    });
  } catch (err) {
    res.status(500).json({
      error: err.message,
    });
  }
});

/* ============================================================
   HISTORIAL / MÉTRICAS
   ============================================================ */

app.get("/api/clients/:clientId/history", async (req, res) => {
  try {
    const pool = await poolPromise;

    const result = await pool
      .request()
      .input("clientId", sql.Int, req.params.clientId)
      .query(`
        SELECT
          a.identificador AS subastaId,
          a.fecha,
          a.hora,
          a.moneda,
          p.descripcionCatalogo,
          MAX(b.importe) AS mejorOfertaPropia,
          MAX(CASE WHEN b.ganador = 'si' THEN 1 ELSE 0 END) AS gano
        FROM Bids b
        INNER JOIN Attendees at
          ON b.asistente = at.identificador
        INNER JOIN CatalogItems ci
          ON b.item = ci.identificador
        INNER JOIN Products p
          ON ci.producto = p.identificador
        INNER JOIN Catalogs c
          ON ci.catalogo = c.identificador
        INNER JOIN Auctions a
          ON c.subasta = a.identificador
        WHERE at.cliente = @clientId
        GROUP BY
          a.identificador,
          a.fecha,
          a.hora,
          a.moneda,
          p.descripcionCatalogo
        ORDER BY a.fecha DESC
      `);

    res.status(200).json(result.recordset);
  } catch (err) {
    res.status(500).json({
      error: err.message,
    });
  }
});

/* ============================================================
   NOTIFICACIONES
   ============================================================ */

app.get("/api/clients/:clientId/notifications", async (req, res) => {
  try {
    const pool = await poolPromise;

    const result = await pool
      .request()
      .input("clientId", sql.Int, req.params.clientId)
      .query(`
        SELECT
          identificador AS id,
          titulo,
          mensaje,
          fechaHora,
          leida
        FROM Notifications
        WHERE cliente = @clientId
        ORDER BY fechaHora DESC
      `);

    res.status(200).json(result.recordset);
  } catch (err) {
    res.status(500).json({
      error: err.message,
    });
  }
});

/* ============================================================
   RUTAS ANTIGUAS COMPATIBLES
   ============================================================ */

app.get("/subastas", async (req, res) => {
  try {
    const pool = await poolPromise;
    const result = await pool.request().query("SELECT * FROM Auctions");
    res.status(200).json(result.recordset);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

/* ============================================================
   INICIO SERVIDOR
   ============================================================ */

app.listen(PORT, () => {
  console.log(`API Subastas corriendo en http://localhost:${PORT}`);
});