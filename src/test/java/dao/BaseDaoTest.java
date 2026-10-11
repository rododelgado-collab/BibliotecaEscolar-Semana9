package dao;

import dao.impl.CategoriaDAOImpl;
import dao.impl.EstudianteDAOImpl;
import dao.impl.LibroDAOImpl;
import dao.impl.PrestamoDAOImpl;
import dao.impl.UsuarioDAOImpl;
import modelo.Categoria;
import modelo.Estudiante;
import modelo.Libro;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Base de las pruebas de los DAO (implementaciones JDBC). Se conectan a la base de PRUEBAS biblioteca_test
 * (configurada en db-test.properties) y dejan las tablas vacías antes de cada test.
 *
 * Seguridad: como los tests borran datos, solo se ejecutan si la base conectada termina en "_test". Si no hay base de
 * pruebas disponible, los tests se OMITEN (no fallan) y el mensaje explica cómo prepararla, así "mvn package"
 * compila igual en equipos que no tienen esa base.
 *
 * @author Rodolfo Delgado
 */
abstract class BaseDaoTest {

    // Si se ejecuta desde IntelliJ (sin Maven) nadie definió db.config: se usa db-test.properties.
    // Con "mvn test" ya lo define el pom.xml (surefire) y este valor no se pisa.
    static {
        if (System.getProperty("db.config") == null) {
            System.setProperty("db.config", "db-test.properties");
        }
    }

    protected final CategoriaDAO categoriaDAO = new CategoriaDAOImpl();
    protected final LibroDAO libroDAO = new LibroDAOImpl();
    protected final UsuarioDAO usuarioDAO = new UsuarioDAOImpl();
    protected final EstudianteDAO estudianteDAO = new EstudianteDAOImpl();
    protected final PrestamoDAO prestamoDAO = new PrestamoDAOImpl();

    @BeforeAll
    static void verificarBaseDePruebas() {
        String problema = null;
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            String base = conn.getCatalog();
            if (base == null || !base.endsWith("_test")) {
                problema = "La base conectada es '" + base + "', no una base de pruebas (*_test). "
                        + "Copie db-test.properties.example como db-test.properties.";
            }
        } catch (SQLException e) {
            problema = "No se pudo usar la base de pruebas: " + DatabaseConnection.describirError(e)
                    + ". Ejecute bd/Script_base_pruebas.sql y revise db-test.properties.";
        }
        if (problema != null) {
            System.err.println("[Pruebas DAO omitidas] " + problema);
        }
        Assumptions.assumeTrue(problema == null, problema);
    }

    @BeforeEach
    void vaciarTablas() {
        // Orden: primero las tablas con claves foráneas (prestamos, libros), después las que ellas referencian
        DatabaseConnection.getInstance().ejecutar(c -> {
            try (Statement st = c.createStatement()) {
                st.executeUpdate("DELETE FROM prestamos");
                st.executeUpdate("DELETE FROM libros");
                st.executeUpdate("DELETE FROM categorias");
                st.executeUpdate("DELETE FROM estudiantes");
                st.executeUpdate("DELETE FROM usuarios");
            }
            return null;
        });
    }

    protected Categoria guardarCategoria(String nombre) {
        Categoria c = new Categoria(nombre);
        categoriaDAO.create(c);
        return c;
    }

    protected Libro guardarLibro(String titulo, String isbn, int stock, Categoria categoria) {
        Libro l = new Libro(titulo, "Autor Prueba", isbn, "Editorial", stock, categoria);
        libroDAO.create(l);
        return l;
    }

    protected Estudiante guardarEstudiante(String nombre, String rut) {
        Estudiante e = new Estudiante(nombre, rut, "3ro Medio A", nombre.split(" ")[0].toLowerCase() + "@correo.cl");
        estudianteDAO.create(e);
        return e;
    }
}
