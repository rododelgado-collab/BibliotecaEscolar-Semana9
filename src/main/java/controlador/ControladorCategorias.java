package controlador;

import dao.CategoriaDAO;
import modelo.Categoria;
import util.Validador;

import java.util.List;

/**
 * CONTROLADOR (MVC) de las categorías de libros: CRUD con validaciones. Nunca ejecuta SQL.
 *
 * @author Rodolfo Delgado
 */
public class ControladorCategorias {

    private final CategoriaDAO categoriaDAO;

    public ControladorCategorias(CategoriaDAO categoriaDAO) {
        this.categoriaDAO = categoriaDAO;
    }

    public Categoria crear(String nombre) {
        String limpio = validar(nombre, 0);
        Categoria categoria = new Categoria(limpio);
        categoriaDAO.create(categoria);
        return categoria;
    }

    public List<Categoria> listar() {
        return categoriaDAO.readAll();
    }

    public Categoria actualizar(int id, String nombre) {
        exigirSeleccion(id);
        String limpio = validar(nombre, id);
        Categoria categoria = new Categoria(id, limpio);
        if (!categoriaDAO.update(categoria)) {
            throw new IllegalArgumentException("La categoría ya no existe en la base de datos.");
        }
        return categoria;
    }

    public void eliminar(int id) {
        exigirSeleccion(id);
        int libros = categoriaDAO.countLibros(id);
        if (libros > 0) {
            throw new IllegalArgumentException("La categoría tiene " + libros
                    + " libro(s). Cambie la categoría de esos libros antes de eliminarla.");
        }
        if (!categoriaDAO.delete(id)) {
            throw new IllegalArgumentException("La categoría ya no existe en la base de datos.");
        }
    }

    private String validar(String nombre, int idPropio) {
        String limpio = Validador.texto(nombre, "el nombre de la categoría", 2, 100);
        for (Categoria c : categoriaDAO.readAll()) {
            if (c.getId() != idPropio && c.getNombre().equalsIgnoreCase(limpio)) {
                throw new IllegalArgumentException("Ya existe la categoría \"" + c.getNombre() + "\".");
            }
        }
        return limpio;
    }

    private void exigirSeleccion(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Seleccione una categoría de la lista.");
        }
    }
}
