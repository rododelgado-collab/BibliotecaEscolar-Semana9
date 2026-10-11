package dao;

/**
 * Excepción única de la capa de persistencia: envuelve cualquier SQLException con un mensaje
 * entendible para el usuario. Los controladores y las vistas solo conocen esta clase, nunca SQLException.
 *
 * @author Rodolfo Delgado
 */
public class DAOException extends RuntimeException {

    public DAOException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public DAOException(String mensaje) {
        super(mensaje);
    }
}
