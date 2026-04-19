const sql = require('mssql');

const config = {
    user: 'sa', 
    password: '1234',
    server: 'localhost',
    database: 'auction',
    options: {
        encrypt: false, 
        trustServerCertificate: true
    },
    port: 1433
};

const poolPromise = new sql.ConnectionPool(config)
    .connect()
    .then(pool => {
        console.log('✅ Conectado a SQL Server en puerto 1433');
        return pool;
    })
    .catch(err => console.log('❌ Error de conexión:', err));

module.exports = { sql, poolPromise };