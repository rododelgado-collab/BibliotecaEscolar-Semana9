package controlador;

import dao.EstudianteDAO;
import dao.UsuarioDAO;
import modelo.Estudiante;
import modelo.Rol;
import modelo.Usuario;

/**
 * CONTROLADOR (MVC) del módulo de autenticación: valida que el usuario exista en la base de datos y que la
 * clave sea correcta. El rol que devuelve decide qué funciones habilita la ventana principal.
 *
 * @author Rodolfo Delgado
 */
public class ControladorAutenticacion {

    private final UsuarioDAO usuarioDAO;
    private final EstudianteDAO estudianteDAO;

    public ControladorAutenticacion(UsuarioDAO usuarioDAO, EstudianteDAO estudianteDAO) {
        this.usuarioDAO = usuarioDAO;
        this.estudianteDAO = estudianteDAO;
    }

    /**
     * @param correo correo del usuario (es su nombre de usuario)
     * @throws IllegalArgumentException con el motivo, listo para mostrar al usuario
     * @throws dao.DAOException         si falla la base de datos
     */
    public Sesion iniciarSesion(String correo, String clave) {
        if (correo == null || correo.trim().isEmpty()) {
            throw new IllegalArgumentException("Ingrese su correo.");
        }
        if (clave == null || clave.isEmpty()) {
            throw new IllegalArgumentException("Ingrese su contraseña.");
        }
        Usuario usuario = usuarioDAO.readByCorreo(correo.trim().toLowerCase());
        // El mismo mensaje para "no existe" y "clave incorrecta": no se revela qué correos están registrados
        if (usuario == null || !usuario.autenticar(clave)) {
            throw new IllegalArgumentException("Correo o contraseña incorrectos.");
        }
        Estudiante estudiante = null;
        if (usuario.getRol() == Rol.ESTUDIANTE) {
            estudiante = estudianteDAO.readByRut(usuario.getRut());
            if (estudiante == null) {
                throw new IllegalArgumentException("Su usuario no está registrado como estudiante. Consulte al bibliotecario.");
            }
        }
        return new Sesion(usuario, estudiante);
    }
}
