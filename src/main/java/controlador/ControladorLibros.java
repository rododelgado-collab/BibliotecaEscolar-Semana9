package controlador;

import dao.LibroDAO;
import dao.PrestamoDAO;
import modelo.Categoria;
import modelo.Libro;
import util.Validador;

import java.util.List;

/**
 * CONTROLADOR (MVC) de los libros: registrar, modificar, eliminar y visualizar. Valida lo que llega desde la ventana
 * y delega la persistencia en LibroDAO. Nunca ejecuta SQL.
 *
 * @author Rodolfo Delgado
 */
public class ControladorLibros {

    private static final int STOCK_MAXIMO = 9999;

    private final LibroDAO libroDAO;
    private final PrestamoDAO prestamoDAO;

    public ControladorLibros(LibroDAO libroDAO, PrestamoDAO prestamoDAO) {
        this.libroDAO = libroDAO;
        this.prestamoDAO = prestamoDAO;
    }

    /**
     * @param stock texto del campo de la ventana (se valida que sea un entero entre 0 y 9999)
     * @throws IllegalArgumentException con un mensaje listo para mostrar al usuario
     * @throws dao.DAOException         si falla la base de datos
     */
    public Libro crear(String titulo, String autor, String isbn, String editorial, String stock, Categoria categoria) {
        Libro libro = armar(0, titulo, autor, isbn, editorial, stock, categoria);
        libroDAO.create(libro);
        return libro;
    }

    public Libro actualizar(int id, String titulo, String autor, String isbn, String editorial, String stock,
                            Categoria categoria) {
        exigirSeleccion(id);
        Libro libro = armar(id, titulo, autor, isbn, editorial, stock, categoria);
        if (!libroDAO.update(libro)) {
            throw new IllegalArgumentException("El libro #" + id + " ya no existe en la base de datos.");
        }
        return libro;
    }

    public void eliminar(int id) {
        exigirSeleccion(id);
        int prestamos = prestamoDAO.countByLibro(id);
        if (prestamos > 0) {
            throw new IllegalArgumentException("El libro tiene " + prestamos
                    + " préstamo(s) en el historial, por eso no se puede eliminar.");
        }
        if (!libroDAO.delete(id)) {
            throw new IllegalArgumentException("El libro #" + id + " ya no existe en la base de datos.");
        }
    }

    public List<Libro> listar() {
        return libroDAO.readAll();
    }

    /** Busca por título, autor, ISBN o editorial (texto vacío = todos). */
    public List<Libro> buscar(String texto) {
        return libroDAO.buscar(texto);
    }

    /** Libros que se pueden prestar (stock mayor que cero). */
    public List<Libro> disponibles() {
        return libroDAO.readDisponibles();
    }

    private Libro armar(int id, String titulo, String autor, String isbn, String editorial, String stock,
                        Categoria categoria) {
        String t = Validador.texto(titulo, "el título", 1, 200);
        String a = Validador.texto(autor, "el autor", 2, 100);
        String i = Validador.isbn(isbn);
        String e = Validador.texto(editorial, "la editorial", 2, 100);
        int s = Validador.entero(stock, "el stock", 0, STOCK_MAXIMO);
        if (categoria == null) {
            throw new IllegalArgumentException("Debe seleccionar una categoría.");
        }
        Libro existente = libroDAO.readByIsbn(i);
        if (existente != null && existente.getId() != id) {
            throw new IllegalArgumentException("Ya existe un libro con el ISBN " + i + ": \"" + existente.getTitulo() + "\".");
        }
        return new Libro(id, t, a, i, e, s, categoria);
    }

    private void exigirSeleccion(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Seleccione un libro de la tabla.");
        }
    }
}
