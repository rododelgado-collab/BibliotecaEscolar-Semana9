package dao;

import modelo.Usuario;

/**
 * Persistencia de los usuarios que inician sesión (tabla usuarios).
 *
 * @author Rodolfo Delgado
 */
public interface UsuarioDAO extends CrudDAO<Usuario> {

    /** Usuario con ese correo, o null si no existe (se usa en el inicio de sesión). */
    Usuario readByCorreo(String correo);

    /** Usuario con ese RUT, o null si no existe. */
    Usuario readByRut(String rut);
}
