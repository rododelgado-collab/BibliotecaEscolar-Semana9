package dao;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Conexión a la base de datos con el patrón SINGLETON: en toda la aplicación existe UNA sola instancia
 * (se obtiene con {@link #getInstance()}) y esa instancia mantiene UNA sola conexión JDBC abierta con
 * DriverManager.getConnection.
 *
 * Como todos los hilos comparten la misma conexión, esta clase también serializa el acceso: ejecutar() y
 * transaccion() son synchronized, así dos hilos nunca mezclan sus consultas ni sus transacciones
 * (por ejemplo, dos préstamos simultáneos del mismo libro).
 *
 * Los datos de acceso se leen de db.properties (junto al programa; NO se sube a GitHub). Con la propiedad
 * del sistema -Ddb.config=otro.properties se elige otro archivo (las pruebas usan db-test.properties).
 * Si el archivo no existe se usan valores por defecto: MySQL en localhost, base "biblioteca", usuario root.
 *
 * @author Rodolfo Delgado
 */
public final class DatabaseConnection {

    private static final String URL_POR_DEFECTO = "jdbc:mysql://localhost:3306/biblioteca";
    private static final String USUARIO_POR_DEFECTO = "root";
    private static final String CLAVE_POR_DEFECTO = "";
    private static final String ARCHIVO_POR_DEFECTO = "db.properties";
    private static final String PROPIEDAD_ARCHIVO = "db.config";

    // Parámetros recomendados del conector para MySQL 8 en un equipo local (solo para URL jdbc:mysql)
    private static final String PARAMETROS_MYSQL =
            "allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Santiago";

    private static DatabaseConnection instancia;

    private Connection conexion;

    /** Constructor privado: nadie más puede crear otra instancia (esa es la esencia del Singleton). */
    private DatabaseConnection() {
    }

    /** Devuelve la única instancia, creándola la primera vez (seguro entre hilos). */
    public static synchronized DatabaseConnection getInstance() {
        if (instancia == null) {
            instancia = new DatabaseConnection();
        }
        return instancia;
    }

    /** Nombre del archivo de configuración en uso (para mostrarlo al usuario). */
    public static String archivoConfiguracion() {
        return System.getProperty(PROPIEDAD_ARCHIVO, ARCHIVO_POR_DEFECTO);
    }

    private static Properties cargarConfiguracion() {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(archivoConfiguracion())) {
            p.load(in);
        } catch (IOException e) {
            // Sin archivo se usan los valores por defecto
        }
        return p;
    }

    /**
     * Devuelve la conexión abierta; si todavía no existe, o se cayó, abre una nueva con DriverManager.getConnection.
     *
     * @throws SQLException si el servidor está apagado, las credenciales fallan o falta el driver
     */
    public synchronized Connection getConnection() throws SQLException {
        if (conexion == null || conexion.isClosed() || !conexion.isValid(2)) {
            Properties config = cargarConfiguracion();
            String url = config.getProperty("db.url", URL_POR_DEFECTO);
            if (url.startsWith("jdbc:mysql:") && !url.contains("?")) {
                url += "?" + PARAMETROS_MYSQL;
            }
            conexion = DriverManager.getConnection(url,
                    config.getProperty("db.user", USUARIO_POR_DEFECTO),
                    config.getProperty("db.password", CLAVE_POR_DEFECTO));
        }
        return conexion;
    }

    /** Ejecuta una consulta o actualización suelta (autocommit). Lanza DAOException si algo falla. */
    public synchronized <T> T ejecutar(OperacionSql<T> operacion) {
        try {
            return operacion.ejecutar(getConnection());
        } catch (SQLException e) {
            throw new DAOException(describirError(e), e);
        }
    }

    /**
     * Ejecuta varias operaciones como UNA transacción: o se guardan todas (commit) o no se guarda ninguna
     * (rollback). Se usa, por ejemplo, para registrar el préstamo y descontar el stock a la vez.
     */
    public synchronized <T> T transaccion(OperacionSql<T> operacion) {
        Connection c = null;
        try {
            c = getConnection();
            c.setAutoCommit(false);
            T resultado = operacion.ejecutar(c);
            c.commit();
            return resultado;
        } catch (SQLException e) {
            deshacer(c);
            throw new DAOException(describirError(e), e);
        } catch (RuntimeException e) {
            deshacer(c);
            throw e;
        } finally {
            restaurarAutocommit(c);
        }
    }

    private void deshacer(Connection c) {
        if (c != null) {
            try {
                c.rollback();
            } catch (SQLException ignorada) {
                // Si ni siquiera se puede deshacer, la conexión se recreará en la próxima operación
            }
        }
    }

    private void restaurarAutocommit(Connection c) {
        if (c != null) {
            try {
                c.setAutoCommit(true);
            } catch (SQLException ignorada) {
                // idem
            }
        }
    }

    /** Comprueba que la base responde (se usa al iniciar la aplicación). */
    public void probarConexion() throws SQLException {
        Connection c = getConnection();
        if (!c.isValid(3)) {
            throw new SQLException("La base de datos no respondió a tiempo.");
        }
    }

    /** Cierra la conexión (al salir de la aplicación). La instancia sigue existiendo y puede reconectar. */
    public synchronized void cerrar() {
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException e) {
                System.err.println("[DatabaseConnection] No se pudo cerrar la conexión: " + e.getMessage());
            }
            conexion = null;
        }
    }

    /** Traduce el error de la base de datos a un mensaje entendible. */
    public static String describirError(SQLException e) {
        String detalle = " (código " + e.getErrorCode() + ": " + e.getMessage() + ")";
        switch (e.getErrorCode()) {
            case 1045:
                return "Usuario o contraseña de MySQL incorrectos. Revise db.properties" + detalle;
            case 1049:
                return "No existe la base de datos 'biblioteca'. Ejecute los scripts SQL de la carpeta bd" + detalle;
            case 1146:
                return "Falta una tabla. Ejecute los scripts SQL de la carpeta bd" + detalle;
            case 1062:
                return "Ya existe un registro con ese dato único (RUT, ISBN o correo)" + detalle;
            case 1451:
                return "No se puede eliminar el registro porque otros registros dependen de él "
                        + "(por ejemplo, préstamos asociados)" + detalle;
            case 1452:
                return "El estudiante, libro o categoría indicado no existe" + detalle;
            case 1406:
                return "Alguno de los textos es más largo de lo que permite la columna" + detalle;
            default:
                String estado = e.getSQLState();
                if (e.getMessage() != null && e.getMessage().contains("No suitable driver")) {
                    return "No se encontró el driver JDBC. Revise la dependencia mysql-connector-j en el pom.xml" + detalle;
                }
                if (e.getErrorCode() <= 0 || (estado != null && estado.startsWith("08"))) {
                    return "No se pudo conectar con MySQL. ¿Está iniciado el servicio y es correcta la url de db.properties?"
                            + detalle;
                }
                return "Error de base de datos" + detalle;
        }
    }
}
