package modelo;

/**
 * CLASE ABSTRACTA base de las personas del sistema (HERENCIA).
 *
 * Reúne los datos que comparten los usuarios del sistema y los estudiantes: id, nombre, RUT y correo.
 * Cada subclase define cómo se presenta ante el sistema con {@link #getTipo()} (POLIMORFISMO).
 *
 * @author Rodolfo Delgado
 */
public abstract class Persona {

    private int id;
    private String nombre;
    private String rut;
    private String correo;

    protected Persona(int id, String nombre, String rut, String correo) {
        this.id = id;
        this.nombre = nombre;
        this.rut = rut;
        this.correo = correo;
    }

    /** Etiqueta del tipo de persona (la define cada subclase). */
    public abstract String getTipo();

    public int getId()          { return id; }
    public String getNombre()   { return nombre; }
    public String getRut()      { return rut; }
    public String getCorreo()   { return correo; }

    /** Lo llama el DAO con la clave que generó MySQL. */
    public void setId(int id)           { this.id = id; }
    public void setNombre(String n)     { this.nombre = n; }
    public void setRut(String rut)      { this.rut = rut; }
    public void setCorreo(String c)     { this.correo = c; }

    @Override
    public String toString() {
        return nombre + " (" + getTipo() + ")";
    }
}
