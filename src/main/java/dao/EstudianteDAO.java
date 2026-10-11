package dao;

import modelo.Estudiante;

/**
 * Persistencia de los estudiantes (tabla estudiantes). Un estudiante también es un usuario del sistema
 * (tabla usuarios, rol estudiante, vinculado por RUT), así que las operaciones que cambian a ambos se
 * hacen en una sola transacción.
 *
 * @author Rodolfo Delgado
 */
public interface EstudianteDAO extends CrudDAO<Estudiante> {

    /** Estudiante con ese RUT, o null si no existe. */
    Estudiante readByRut(String rut);

    /** Estudiantes cuyo nombre, RUT, curso o correo contienen el texto. Vacío = todos. */
    java.util.List<Estudiante> buscar(String texto);

    /**
     * Registra al estudiante Y su cuenta de usuario (rol estudiante, con esa clave) en una transacción:
     * o se guardan los dos o ninguno.
     */
    void registrarConUsuario(Estudiante estudiante, String clave);
}
