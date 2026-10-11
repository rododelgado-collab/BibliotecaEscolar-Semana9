package modelo;

/**
 * Contrato de todo objeto que puede iniciar sesión en el sistema.
 * Lo implementa {@link Usuario}; el controlador de autenticación solo conoce esta interfaz.
 *
 * @author Rodolfo Delgado
 */
public interface Autenticable {

    /** @return true si la clave entregada corresponde a la del usuario */
    boolean autenticar(String clave);
}
