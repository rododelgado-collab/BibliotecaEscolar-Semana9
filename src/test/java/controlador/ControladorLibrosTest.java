package controlador;

import dao.DAOException;
import modelo.Categoria;
import modelo.Estudiante;
import modelo.Libro;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de los controladores de libros y categorías con DAO falsos. */
class ControladorLibrosTest {

    private DaosFalsos.Libros libros;
    private DaosFalsos.Estudiantes estudiantes;
    private DaosFalsos.Prestamos prestamos;
    private DaosFalsos.Categorias categoriasDao;
    private ControladorLibros controlador;
    private ControladorCategorias controladorCategorias;
    private final Categoria categoria = new Categoria(1, "Ciencia Ficción");

    @BeforeEach
    void preparar() {
        DaosFalsos.caidos = false;
        libros = new DaosFalsos.Libros();
        estudiantes = new DaosFalsos.Estudiantes(new DaosFalsos.Usuarios());
        prestamos = new DaosFalsos.Prestamos(libros, estudiantes);
        categoriasDao = new DaosFalsos.Categorias(libros);
        controlador = new ControladorLibros(libros, prestamos);
        controladorCategorias = new ControladorCategorias(categoriasDao);
    }

    @AfterEach
    void restaurar() {
        DaosFalsos.caidos = false;
    }

    private String mensajeDe(Runnable accion) {
        return assertThrows(IllegalArgumentException.class, accion::run).getMessage();
    }

    @Test
    @DisplayName("crear guarda el libro con el ISBN sin guiones")
    void crear() {
        Libro l = controlador.crear("  Dune ", "Frank Herbert", "978-0-441-01359-3", "Ace Books", "5", categoria);
        assertEquals(1, l.getId());
        assertEquals("Dune", l.getTitulo());
        assertEquals("9780441013593", l.getIsbn());
        assertEquals(5, l.getStock());
        assertEquals(1, libros.datos.size());
    }

    @Test
    @DisplayName("crear valida título, autor, ISBN, editorial, stock y categoría")
    void crearValida() {
        assertTrue(mensajeDe(() -> controlador.crear("", "Autor X", "9780441013593", "Ace", "5", categoria)).contains("título"));
        assertTrue(mensajeDe(() -> controlador.crear("Dune", "", "9780441013593", "Ace", "5", categoria)).contains("autor"));
        assertTrue(mensajeDe(() -> controlador.crear("Dune", "Autor X", "123", "Ace", "5", categoria)).contains("ISBN"));
        assertTrue(mensajeDe(() -> controlador.crear("Dune", "Autor X", "9780441013593", "", "5", categoria)).contains("editorial"));
        assertTrue(mensajeDe(() -> controlador.crear("Dune", "Autor X", "9780441013593", "Ace", "abc", categoria)).contains("número entero"));
        assertTrue(mensajeDe(() -> controlador.crear("Dune", "Autor X", "9780441013593", "Ace", "-1", categoria)).contains("entre 0"));
        assertTrue(mensajeDe(() -> controlador.crear("Dune", "Autor X", "9780441013593", "Ace", "5", null)).contains("categoría"));
        assertTrue(libros.datos.isEmpty());
    }

    @Test
    @DisplayName("no se permiten dos libros con el mismo ISBN")
    void isbnDuplicado() {
        controlador.crear("Dune", "Frank Herbert", "9780441013593", "Ace", "5", categoria);
        assertTrue(mensajeDe(() -> controlador.crear("Otro", "Autor X", "9780441013593", "Ace", "1", categoria)).contains("Ya existe"));
        Libro l = libros.datos.get(1);
        // Actualizarse a sí mismo con su propio ISBN sí es válido
        controlador.actualizar(l.getId(), "Dune (edición nueva)", "Frank Herbert", "9780441013593", "Ace", "7", categoria);
        assertEquals(7, libros.datos.get(1).getStock());
    }

    @Test
    @DisplayName("actualizar y eliminar validan selección y existencia")
    void actualizarEliminar() {
        assertTrue(mensajeDe(() -> controlador.actualizar(0, "Dune", "Autor X", "9780441013593", "Ace", "5", categoria)).contains("Seleccione"));
        assertTrue(mensajeDe(() -> controlador.actualizar(99, "Dune", "Autor X", "9780441013593", "Ace", "5", categoria)).contains("ya no existe"));
        assertTrue(mensajeDe(() -> controlador.eliminar(0)).contains("Seleccione"));
        assertTrue(mensajeDe(() -> controlador.eliminar(99)).contains("ya no existe"));
        Libro l = controlador.crear("Dune", "Frank Herbert", "9780441013593", "Ace", "5", categoria);
        controlador.eliminar(l.getId());
        assertTrue(libros.datos.isEmpty());
    }

    @Test
    @DisplayName("un libro con préstamos en el historial no se puede eliminar")
    void eliminarConPrestamos() {
        Libro l = libros.agregar("Dune", "9780441013593", 5);
        Estudiante e = estudiantes.agregar("Carlos Ruiz", "98765432-1");
        new ControladorPrestamos(prestamos, estudiantes, libros, java.time.LocalDate::now, 0)
                .registrarPrestamo(e.getId(), l.getId());
        assertTrue(mensajeDe(() -> controlador.eliminar(l.getId())).contains("préstamo"));
        assertEquals(1, libros.datos.size());
    }

    @Test
    @DisplayName("buscar encuentra por título, autor o ISBN y disponibles omite los agotados")
    void buscarYDisponibles() {
        libros.agregar("Dune", "9780441013593", 5);
        libros.agregar("Fundación", "9788497594256", 0);
        assertEquals(1, controlador.buscar("dune").size());
        assertEquals(1, controlador.buscar("9788497594256").size());
        assertEquals(2, controlador.buscar("").size());
        assertEquals(1, controlador.disponibles().size());
    }

    @Test
    @DisplayName("categorías: crear, no duplicar, renombrar y no eliminar si tiene libros")
    void categorias() {
        Categoria c = controladorCategorias.crear("  Historia ");
        assertEquals("Historia", c.getNombre());
        assertTrue(mensajeDe(() -> controladorCategorias.crear("historia")).contains("Ya existe"));
        assertTrue(mensajeDe(() -> controladorCategorias.crear("A")).contains("al menos 2"));
        controladorCategorias.actualizar(c.getId(), "Historia Universal");
        assertEquals("Historia Universal", categoriasDao.datos.get(c.getId()).getNombre());

        libros.create(new Libro("Sapiens", "Harari", "9788499924211", "Debate", 4, c));
        assertTrue(mensajeDe(() -> controladorCategorias.eliminar(c.getId())).contains("libro(s)"));
        libros.datos.clear();
        controladorCategorias.eliminar(c.getId());
        assertTrue(categoriasDao.datos.isEmpty());
    }

    @Test
    @DisplayName("un error de la base de datos se propaga como DAOException")
    void bdCaida() {
        DaosFalsos.caidos = true;
        assertThrows(DAOException.class, () -> controlador.listar());
        assertThrows(DAOException.class, () -> controlador.crear("Dune", "Frank Herbert", "9780441013593", "Ace", "5", categoria));
    }
}
