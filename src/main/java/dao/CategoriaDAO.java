package dao;

import modelo.Categoria;

/**
 * Persistencia de las categorías de libros (tabla categorias).
 *
 * @author Rodolfo Delgado
 */
public interface CategoriaDAO extends CrudDAO<Categoria> {

    /** Cuántos libros pertenecen a la categoría (para impedir eliminarla si tiene libros). */
    int countLibros(int idCategoria);
}
