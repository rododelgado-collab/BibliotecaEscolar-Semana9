package dao;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Trabajo que se ejecuta con la conexión de {@link DatabaseConnection}. Es una interfaz funcional:
 * cada DAO escribe su consulta como una lambda y la conexión se encarga de abrir, sincronizar y reintentar.
 *
 * @param <T> lo que devuelve la operación
 * @author Rodolfo Delgado
 */
@FunctionalInterface
public interface OperacionSql<T> {

    T ejecutar(Connection conexion) throws SQLException;
}
