package dao;

import modelo.Libro;

import java.util.List;

/**
 * Persistencia de los libros (tabla libros, con JOIN a categorias).
 *
 * @author Rodolfo Delgado
 */
public interface LibroDAO extends CrudDAO<Libro> {

    /** Libro con ese ISBN, o null si no existe (para validar que no se repita). */
    Libro readByIsbn(String isbn);

    /** Busca por texto en título, autor, ISBN o editorial. Texto vacío = todos. Consulta parametrizada. */
    List<Libro> buscar(String texto);

    /** Libros con stock disponible (los que se pueden prestar). */
    List<Libro> readDisponibles();
}
