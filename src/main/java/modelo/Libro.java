package modelo;

/**
 * Libro del inventario. Pertenece a una {@link Categoria} y lleva el stock disponible.
 * Implementa {@link Prestable}: el stock solo se modifica con descontarUnidad() y reponerUnidad().
 *
 * @author Rodolfo Delgado
 */
public class Libro implements Prestable {

    private int id;
    private String titulo;
    private String autor;
    private String isbn;
    private String editorial;
    private int stock;
    private Categoria categoria;

    public Libro(int id, String titulo, String autor, String isbn, String editorial, int stock, Categoria categoria) {
        this.id = id;
        this.titulo = titulo;
        this.autor = autor;
        this.isbn = isbn;
        this.editorial = editorial;
        this.stock = stock;
        this.categoria = categoria;
    }

    public Libro(String titulo, String autor, String isbn, String editorial, int stock, Categoria categoria) {
        this(0, titulo, autor, isbn, editorial, stock, categoria);
    }

    @Override
    public boolean estaDisponible() {
        return stock > 0;
    }

    @Override
    public void descontarUnidad() {
        if (stock <= 0) {
            throw new IllegalStateException("No queda stock de \"" + titulo + "\".");
        }
        stock--;
    }

    @Override
    public void reponerUnidad() {
        stock++;
    }

    public int getId()                  { return id; }
    public String getTitulo()           { return titulo; }
    public String getAutor()            { return autor; }
    public String getIsbn()             { return isbn; }
    public String getEditorial()        { return editorial; }
    public int getStock()               { return stock; }
    public Categoria getCategoria()     { return categoria; }

    public void setId(int id)                       { this.id = id; }
    public void setTitulo(String titulo)            { this.titulo = titulo; }
    public void setAutor(String autor)              { this.autor = autor; }
    public void setIsbn(String isbn)                { this.isbn = isbn; }
    public void setEditorial(String editorial)      { this.editorial = editorial; }
    public void setStock(int stock)                 { this.stock = stock; }
    public void setCategoria(Categoria categoria)   { this.categoria = categoria; }

    /** Texto de los combos de préstamo. */
    @Override
    public String toString() {
        return titulo + " (" + autor + ") - stock " + stock;
    }
}
