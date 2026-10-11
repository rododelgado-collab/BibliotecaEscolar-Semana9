package modelo;

/**
 * CLASE ABSTRACTA de los usuarios que inician sesión (HERENCIA de {@link Persona}).
 *
 * Los permisos de cada rol son métodos abstractos: {@link Bibliotecario} y {@link UsuarioEstudiante}
 * los resuelven distinto, y la interfaz gráfica solo pregunta "¿puede?" sin importar el rol
 * (POLIMORFISMO). Implementa {@link Autenticable}.
 *
 * @author Rodolfo Delgado
 */
public abstract class Usuario extends Persona implements Autenticable {

    private String clave;

    protected Usuario(int id, String nombre, String rut, String correo, String clave) {
        super(id, nombre, rut, correo);
        this.clave = clave;
    }

    /** Crea la subclase que corresponde al rol guardado en la base de datos (método fábrica). */
    public static Usuario crear(int id, String nombre, String rut, String correo, String clave, Rol rol) {
        switch (rol) {
            case BIBLIOTECARIO: return new Bibliotecario(id, nombre, rut, correo, clave);
            case ESTUDIANTE:    return new UsuarioEstudiante(id, nombre, rut, correo, clave);
            default: throw new IllegalArgumentException("Rol no soportado: " + rol);
        }
    }

    public abstract Rol getRol();

    /** Registrar, modificar y eliminar libros y categorías. */
    public abstract boolean puedeGestionarCatalogo();

    /** Registrar, modificar y eliminar estudiantes. */
    public abstract boolean puedeGestionarEstudiantes();

    /** Registrar préstamos a nombre de cualquier estudiante (si es false, solo a nombre propio). */
    public abstract boolean puedePrestarATodos();

    /** Ver reportes de todo el sistema (si es false, solo su propio historial). */
    public abstract boolean puedeVerReportesGlobales();

    @Override
    public boolean autenticar(String clave) {
        return this.clave != null && this.clave.equals(clave);
    }

    @Override
    public String getTipo() {
        return getRol().toString();
    }

    public String getClave()            { return clave; }
    public void setClave(String clave)  { this.clave = clave; }
}
