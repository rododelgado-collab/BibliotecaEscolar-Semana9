package modelo;

/**
 * Categoría de los libros (Ciencia Ficción, Historia, etc.).
 *
 * @author Rodolfo Delgado
 */
public class Categoria {

    private int id;
    private String nombre;

    public Categoria(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public Categoria(String nombre) {
        this(0, nombre);
    }

    public int getId()                  { return id; }
    public String getNombre()           { return nombre; }
    public void setId(int id)           { this.id = id; }
    public void setNombre(String n)     { this.nombre = n; }

    /** Lo que muestran los JComboBox. */
    @Override
    public String toString() {
        return nombre;
    }
}
