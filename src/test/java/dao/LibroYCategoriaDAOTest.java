package dao;

import modelo.Categoria;
import modelo.Libro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de CategoriaDAO y LibroDAO contra la base de pruebas. */
class LibroYCategoriaDAOTest extends BaseDaoTest {

    @Test
    @DisplayName("categorías: CRUD completo")
    void categoriasCrud() {
        Categoria c = guardarCategoria("Historia");
        assertTrue(c.getId() > 0);
        assertEquals("Historia", categoriaDAO.readById(c.getId()).getNombre());
        assertEquals(1, categoriaDAO.readAll().size());

        c.setNombre("Historia Universal");
        assertTrue(categoriaDAO.update(c));
        assertEquals("Historia Universal", categoriaDAO.readById(c.getId()).getNombre());

        assertTrue(categoriaDAO.delete(c.getId()));
        assertNull(categoriaDAO.readById(c.getId()));
        assertFalse(categoriaDAO.delete(c.getId()));
    }

    @Test
    @DisplayName("libros: CRUD completo con su categoría, tildes y eñes")
    void librosCrud() {
        Categoria c = guardarCategoria("Literatura");
        Libro l = guardarLibro("Crónica de una muerte anunciada", "9780307387340", 3, c);
        assertTrue(l.getId() > 0);

        Libro leido = libroDAO.readById(l.getId());
        assertEquals("Crónica de una muerte anunciada", leido.getTitulo());
        assertEquals(3, leido.getStock());
        assertEquals("Literatura", leido.getCategoria().getNombre());

        leido.setStock(10);
        leido.setTitulo("El año de la peste ñandú");
        assertTrue(libroDAO.update(leido));
        assertEquals(10, libroDAO.readById(l.getId()).getStock());
        assertEquals("El año de la peste ñandú", libroDAO.readById(l.getId()).getTitulo());

        assertTrue(libroDAO.delete(l.getId()));
        assertNull(libroDAO.readById(l.getId()));
    }

    @Test
    @DisplayName("libros: buscar, buscar por ISBN y listar solo disponibles")
    void librosConsultas() {
        Categoria c = guardarCategoria("Ciencia Ficción");
        guardarLibro("Dune", "9780441013593", 5, c);
        guardarLibro("Fundación", "9788497594256", 0, c);

        assertEquals(2, libroDAO.readAll().size());
        assertEquals(1, libroDAO.buscar("dun").size());
        assertEquals(1, libroDAO.buscar("9788497594256").size());
        assertEquals(2, libroDAO.buscar("autor prueba").size());
        assertEquals("Dune", libroDAO.readByIsbn("9780441013593").getTitulo());
        assertNull(libroDAO.readByIsbn("0000000000000"));
        assertEquals(1, libroDAO.readDisponibles().size());
        assertEquals(2, categoriaDAO.countLibros(c.getId()));
    }

    @Test
    @DisplayName("un texto con SQL malicioso se guarda como texto y no daña la base (consulta parametrizada)")
    void sqlMalicioso() {
        Categoria c = guardarCategoria("General");
        guardarLibro("'; DROP TABLE libros; --", "9780441013593", 1, c);
        assertEquals(1, libroDAO.buscar("DROP TABLE").size());
        assertEquals(1, libroDAO.readAll().size());
    }

    @Test
    @DisplayName("ISBN repetido o categoría con libros violan las restricciones y llegan como DAOException")
    void restricciones() {
        Categoria c = guardarCategoria("General");
        guardarLibro("Uno", "9780441013593", 1, c);
        DAOException repetido = assertThrows(DAOException.class, () -> guardarLibro("Dos", "9780441013593", 1, c));
        assertTrue(repetido.getMessage().contains("Ya existe"));

        DAOException conLibros = assertThrows(DAOException.class, () -> categoriaDAO.delete(c.getId()));
        assertTrue(conLibros.getMessage().contains("dependen"));
        assertNotNull(libroDAO.readByIsbn("9780441013593"));
    }
}
