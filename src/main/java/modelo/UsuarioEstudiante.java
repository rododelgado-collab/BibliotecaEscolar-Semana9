package modelo;

/**
 * Usuario con rol estudiante: solo puede consultar el catálogo y registrar préstamos y devoluciones
 * a su propio nombre.
 *
 * @author Rodolfo Delgado
 */
public class UsuarioEstudiante extends Usuario {

    public UsuarioEstudiante(int id, String nombre, String rut, String correo, String clave) {
        super(id, nombre, rut, correo, clave);
    }

    @Override public Rol getRol()                       { return Rol.ESTUDIANTE; }
    @Override public boolean puedeGestionarCatalogo()   { return false; }
    @Override public boolean puedeGestionarEstudiantes(){ return false; }
    @Override public boolean puedePrestarATodos()       { return false; }
    @Override public boolean puedeVerReportesGlobales() { return false; }
}
