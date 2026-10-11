package modelo;

/**
 * Usuario con rol bibliotecario: gestión total del sistema.
 *
 * @author Rodolfo Delgado
 */
public class Bibliotecario extends Usuario {

    public Bibliotecario(int id, String nombre, String rut, String correo, String clave) {
        super(id, nombre, rut, correo, clave);
    }

    @Override public Rol getRol()                       { return Rol.BIBLIOTECARIO; }
    @Override public boolean puedeGestionarCatalogo()   { return true; }
    @Override public boolean puedeGestionarEstudiantes(){ return true; }
    @Override public boolean puedePrestarATodos()       { return true; }
    @Override public boolean puedeVerReportesGlobales() { return true; }
}
