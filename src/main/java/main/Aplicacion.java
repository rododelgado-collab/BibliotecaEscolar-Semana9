package main;

import controlador.ControladorAutenticacion;
import controlador.ControladorCategorias;
import controlador.ControladorEstudiantes;
import controlador.ControladorLibros;
import controlador.ControladorPrestamos;
import controlador.ControladorReportes;
import dao.CategoriaDAO;
import dao.EstudianteDAO;
import dao.LibroDAO;
import dao.PrestamoDAO;
import dao.UsuarioDAO;
import dao.impl.CategoriaDAOImpl;
import dao.impl.EstudianteDAOImpl;
import dao.impl.LibroDAOImpl;
import dao.impl.PrestamoDAOImpl;
import dao.impl.UsuarioDAOImpl;

/**
 * Raíz de composición de la aplicación (inyección de dependencias "a mano"): crea UNA vez los DAO y los controladores
 * y se los entrega a las ventanas.
 *
 *   DAO (JDBC)  ->  controladores (validan y coordinan)  ->  vistas (Swing)
 *
 * Es importante que exista una sola instancia de ControladorPrestamos: su sincronización (synchronized) solo
 * protege el stock si todas las ventanas comparten ese mismo objeto.
 *
 * @author Rodolfo Delgado
 */
public class Aplicacion {

    public final ControladorAutenticacion autenticacion;
    public final ControladorLibros libros;
    public final ControladorCategorias categorias;
    public final ControladorEstudiantes estudiantes;
    public final ControladorPrestamos prestamos;
    public final ControladorReportes reportes;

    public Aplicacion() {
        // Capa de acceso a datos: interfaces DAO con su implementación JDBC (todas usan DatabaseConnection, el Singleton)
        UsuarioDAO usuarioDAO = new UsuarioDAOImpl();
        EstudianteDAO estudianteDAO = new EstudianteDAOImpl();
        CategoriaDAO categoriaDAO = new CategoriaDAOImpl();
        LibroDAO libroDAO = new LibroDAOImpl();
        PrestamoDAO prestamoDAO = new PrestamoDAOImpl();

        // Capa de controladores: reciben los DAO (nunca ejecutan SQL)
        autenticacion = new ControladorAutenticacion(usuarioDAO, estudianteDAO);
        libros = new ControladorLibros(libroDAO, prestamoDAO);
        categorias = new ControladorCategorias(categoriaDAO);
        estudiantes = new ControladorEstudiantes(estudianteDAO, usuarioDAO, prestamoDAO);
        prestamos = new ControladorPrestamos(prestamoDAO, estudianteDAO, libroDAO);
        reportes = new ControladorReportes(prestamoDAO);
    }
}
