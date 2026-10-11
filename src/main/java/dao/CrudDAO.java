package dao;

import java.util.List;

/**
 * Contrato CRUD genérico que comparten todos los DAO (patrón DAO). Cada DAO concreto solo agrega las
 * consultas propias de su entidad. Los controladores dependen de estas interfaces, no del SQL.
 *
 * @param <T> la entidad que guarda
 * @author Rodolfo Delgado
 */
public interface CrudDAO<T> {

    /** CREATE: inserta la entidad y deja en ella el ID que generó la base de datos. */
    void create(T entidad);

    /** READ: todas las filas. */
    List<T> readAll();

    /** READ de una fila por ID. @return la entidad, o null si no existe */
    T readById(int id);

    /** UPDATE. @return true si la fila existía */
    boolean update(T entidad);

    /** DELETE por ID. @return true si se eliminó una fila */
    boolean delete(int id);
}
