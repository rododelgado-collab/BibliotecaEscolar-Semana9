package controlador;

import dao.EstudianteDAO;
import dao.PrestamoDAO;
import dao.UsuarioDAO;
import modelo.Estudiante;
import modelo.Usuario;
import util.Validador;

import java.util.List;

/**
 * CONTROLADOR (MVC) de los estudiantes: los registra como usuarios del sistema (cuenta con rol estudiante), los
 * modifica, los elimina y los lista. Valida lo que llega desde la ventana y nunca ejecuta SQL.
 *
 * @author Rodolfo Delgado
 */
public class ControladorEstudiantes {

    private static final int CLAVE_MINIMA = 4;

    private final EstudianteDAO estudianteDAO;
    private final UsuarioDAO usuarioDAO;
    private final PrestamoDAO prestamoDAO;

    public ControladorEstudiantes(EstudianteDAO estudianteDAO, UsuarioDAO usuarioDAO, PrestamoDAO prestamoDAO) {
        this.estudianteDAO = estudianteDAO;
        this.usuarioDAO = usuarioDAO;
        this.prestamoDAO = prestamoDAO;
    }

    /**
     * Registra al estudiante y su cuenta de acceso (el correo es su usuario).
     *
     * @throws IllegalArgumentException con un mensaje listo para mostrar al usuario
     * @throws dao.DAOException         si falla la base de datos
     */
    public Estudiante registrar(String nombre, String rut, String curso, String correo, String clave) {
        String n = Validador.texto(nombre, "el nombre", 3, 100);
        String r = Validador.rut(rut);
        String cu = Validador.texto(curso, "el curso", 2, 20);
        String co = Validador.correo(correo);
        if (clave == null || clave.length() < CLAVE_MINIMA) {
            throw new IllegalArgumentException("La contraseña debe tener al menos " + CLAVE_MINIMA + " caracteres.");
        }
        if (clave.length() > 100) {
            throw new IllegalArgumentException("La contraseña no puede superar los 100 caracteres.");
        }
        if (estudianteDAO.readByRut(r) != null || usuarioDAO.readByRut(r) != null) {
            throw new IllegalArgumentException("Ya existe una persona registrada con el RUT " + r + ".");
        }
        if (usuarioDAO.readByCorreo(co) != null) {
            throw new IllegalArgumentException("Ya existe un usuario con el correo " + co + ".");
        }
        Estudiante estudiante = new Estudiante(n, r, cu, co);
        estudianteDAO.registrarConUsuario(estudiante, clave);
        return estudiante;
    }

    /** Cambia nombre, curso y correo. El RUT no se puede cambiar (es lo que vincula al estudiante con su usuario). */
    public Estudiante actualizar(int id, String nombre, String curso, String correo) {
        if (id <= 0) {
            throw new IllegalArgumentException("Seleccione un estudiante de la tabla.");
        }
        Estudiante actual = estudianteDAO.readById(id);
        if (actual == null) {
            throw new IllegalArgumentException("El estudiante #" + id + " ya no existe en la base de datos.");
        }
        String n = Validador.texto(nombre, "el nombre", 3, 100);
        String cu = Validador.texto(curso, "el curso", 2, 20);
        String co = Validador.correo(correo);
        Usuario conEseCorreo = usuarioDAO.readByCorreo(co);
        if (conEseCorreo != null && !conEseCorreo.getRut().equals(actual.getRut())) {
            throw new IllegalArgumentException("Ya existe otro usuario con el correo " + co + ".");
        }
        Estudiante nuevo = new Estudiante(id, n, actual.getRut(), cu, co);
        estudianteDAO.update(nuevo);
        return nuevo;
    }

    public void eliminar(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Seleccione un estudiante de la tabla.");
        }
        int prestamos = prestamoDAO.countByEstudiante(id);
        if (prestamos > 0) {
            throw new IllegalArgumentException("El estudiante tiene " + prestamos
                    + " préstamo(s) en su historial, por eso no se puede eliminar.");
        }
        if (!estudianteDAO.delete(id)) {
            throw new IllegalArgumentException("El estudiante #" + id + " ya no existe en la base de datos.");
        }
    }

    public List<Estudiante> listar() {
        return estudianteDAO.readAll();
    }

    public List<Estudiante> buscar(String texto) {
        return estudianteDAO.buscar(texto);
    }
}
