package modelo;

/**
 * Estudiante de la biblioteca: la persona que realiza los préstamos y tiene un historial individual.
 * Hereda de {@link Persona} y agrega el curso.
 *
 * @author Rodolfo Delgado
 */
public class Estudiante extends Persona {

    private String curso;

    public Estudiante(int id, String nombre, String rut, String curso, String correo) {
        super(id, nombre, rut, correo);
        this.curso = curso;
    }

    public Estudiante(String nombre, String rut, String curso, String correo) {
        this(0, nombre, rut, curso, correo);
    }

    @Override
    public String getTipo() {
        return "Estudiante";
    }

    public String getCurso()            { return curso; }
    public void setCurso(String curso)  { this.curso = curso; }

    /** Texto de los combos: el objeto conserva su id, el usuario ve nombre y curso. */
    @Override
    public String toString() {
        return getNombre() + " - " + curso;
    }
}
