package controlador;

import dao.DAOException;
import modelo.Estudiante;
import modelo.Libro;
import modelo.Rol;
import modelo.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de los controladores de estudiantes y de autenticación con DAO falsos. */
class ControladorEstudiantesTest {

    private DaosFalsos.Usuarios usuarios;
    private DaosFalsos.Estudiantes estudiantes;
    private DaosFalsos.Libros libros;
    private DaosFalsos.Prestamos prestamos;
    private ControladorEstudiantes controlador;
    private ControladorAutenticacion autenticacion;

    @BeforeEach
    void preparar() {
        DaosFalsos.caidos = false;
        usuarios = new DaosFalsos.Usuarios();
        estudiantes = new DaosFalsos.Estudiantes(usuarios);
        libros = new DaosFalsos.Libros();
        prestamos = new DaosFalsos.Prestamos(libros, estudiantes);
        controlador = new ControladorEstudiantes(estudiantes, usuarios, prestamos);
        autenticacion = new ControladorAutenticacion(usuarios, estudiantes);
    }

    @AfterEach
    void restaurar() {
        DaosFalsos.caidos = false;
    }

    private String mensajeDe(Runnable accion) {
        return assertThrows(IllegalArgumentException.class, accion::run).getMessage();
    }

    @Test
    @DisplayName("registrar crea el estudiante y su cuenta de usuario con rol estudiante")
    void registrar() {
        Estudiante e = controlador.registrar("  Ana   Pérez ", "12.345.678-5", "2do Medio A", "ANA@correo.cl", "clave1");
        assertEquals("Ana Pérez", e.getNombre());
        assertEquals("12345678-5", e.getRut());
        assertEquals("ana@correo.cl", e.getCorreo());
        assertEquals(1, estudiantes.datos.size());
        Usuario u = usuarios.readByCorreo("ana@correo.cl");
        assertNotNull(u);
        assertEquals(Rol.ESTUDIANTE, u.getRol());
        assertTrue(u.autenticar("clave1"));
    }

    @Test
    @DisplayName("registrar valida nombre, RUT, curso, correo y contraseña")
    void registrarValida() {
        assertTrue(mensajeDe(() -> controlador.registrar("", "12345678-5", "2do A", "a@correo.cl", "clave1")).contains("nombre"));
        assertTrue(mensajeDe(() -> controlador.registrar("Ana Pérez", "1234", "2do A", "a@correo.cl", "clave1")).contains("RUT"));
        assertTrue(mensajeDe(() -> controlador.registrar("Ana Pérez", "12345678-5", "", "a@correo.cl", "clave1")).contains("curso"));
        assertTrue(mensajeDe(() -> controlador.registrar("Ana Pérez", "12345678-5", "2do A", "sin-arroba", "clave1")).contains("correo"));
        assertTrue(mensajeDe(() -> controlador.registrar("Ana Pérez", "12345678-5", "2do A", "a@correo.cl", "abc")).contains("contraseña"));
        assertTrue(estudiantes.datos.isEmpty());
        assertTrue(usuarios.datos.isEmpty());
    }

    @Test
    @DisplayName("no se repiten el RUT ni el correo")
    void duplicados() {
        controlador.registrar("Ana Pérez", "12345678-5", "2do A", "ana@correo.cl", "clave1");
        assertTrue(mensajeDe(() -> controlador.registrar("Otra Ana", "12345678-5", "2do A", "otra@correo.cl", "clave1")).contains("RUT"));
        assertTrue(mensajeDe(() -> controlador.registrar("Otra Ana", "87654321-K", "2do A", "ana@correo.cl", "clave1")).contains("correo"));
        assertEquals(1, estudiantes.datos.size());
    }

    @Test
    @DisplayName("actualizar cambia nombre, curso y correo pero conserva el RUT")
    void actualizar() {
        Estudiante e = controlador.registrar("Ana Pérez", "12345678-5", "2do A", "ana@correo.cl", "clave1");
        Estudiante nuevo = controlador.actualizar(e.getId(), "Ana Soto", "3ro B", "ana.soto@correo.cl");
        assertEquals("12345678-5", nuevo.getRut());
        assertEquals("Ana Soto", estudiantes.datos.get(e.getId()).getNombre());
        assertTrue(mensajeDe(() -> controlador.actualizar(0, "Ana", "3ro B", "a@correo.cl")).contains("Seleccione"));
        assertTrue(mensajeDe(() -> controlador.actualizar(99, "Ana Soto", "3ro B", "a@correo.cl")).contains("ya no existe"));
    }

    @Test
    @DisplayName("eliminar borra al estudiante y su usuario, salvo que tenga préstamos")
    void eliminar() {
        Estudiante e = controlador.registrar("Ana Pérez", "12345678-5", "2do A", "ana@correo.cl", "clave1");
        Libro l = libros.agregar("Dune", "9780441013593", 2);
        new ControladorPrestamos(prestamos, estudiantes, libros, java.time.LocalDate::now, 0)
                .registrarPrestamo(e.getId(), l.getId());
        assertTrue(mensajeDe(() -> controlador.eliminar(e.getId())).contains("préstamo"));
        prestamos.datos.clear();

        controlador.eliminar(e.getId());
        assertTrue(estudiantes.datos.isEmpty());
        assertTrue(usuarios.datos.isEmpty());
        assertTrue(mensajeDe(() -> controlador.eliminar(e.getId())).contains("ya no existe"));
    }

    @Test
    @DisplayName("el inicio de sesión devuelve el rol correcto y rechaza claves o correos inválidos")
    void autenticacion() {
        usuarios.create(Usuario.crear(0, "Antonia Pérez", "12345678-9", "antonia@correo.cl", "clave123", Rol.BIBLIOTECARIO));
        controlador.registrar("Carlos Ruiz", "98765432-1", "3ro A", "carlos@correo.cl", "clave123");

        Sesion biblio = autenticacion.iniciarSesion("antonia@correo.cl", "clave123");
        assertEquals(Rol.BIBLIOTECARIO, biblio.usuario().getRol());
        assertNull(biblio.estudiante());
        assertTrue(biblio.usuario().puedeGestionarCatalogo());

        Sesion alumno = autenticacion.iniciarSesion(" CARLOS@correo.cl ", "clave123");
        assertEquals(Rol.ESTUDIANTE, alumno.usuario().getRol());
        assertEquals("Carlos Ruiz", alumno.estudiante().getNombre());
        assertFalse(alumno.usuario().puedeGestionarCatalogo());
        assertFalse(alumno.usuario().puedePrestarATodos());

        assertTrue(mensajeDe(() -> autenticacion.iniciarSesion("antonia@correo.cl", "mala")).contains("incorrectos"));
        assertTrue(mensajeDe(() -> autenticacion.iniciarSesion("nadie@correo.cl", "clave123")).contains("incorrectos"));
        assertTrue(mensajeDe(() -> autenticacion.iniciarSesion("", "clave123")).contains("correo"));
        assertTrue(mensajeDe(() -> autenticacion.iniciarSesion("antonia@correo.cl", "")).contains("contraseña"));
    }

    @Test
    @DisplayName("un usuario estudiante sin ficha de estudiante no puede entrar")
    void usuarioSinFicha() {
        usuarios.create(Usuario.crear(0, "Huérfano", "11111111-1", "h@correo.cl", "clave123", Rol.ESTUDIANTE));
        assertTrue(mensajeDe(() -> autenticacion.iniciarSesion("h@correo.cl", "clave123")).contains("no está registrado como estudiante"));
    }

    @Test
    @DisplayName("POLIMORFISMO: cada rol responde distinto a la misma pregunta de permisos")
    void polimorfismo() {
        Usuario b = Usuario.crear(1, "B", "1-9", "b@c.cl", "x", Rol.BIBLIOTECARIO);
        Usuario s = Usuario.crear(2, "S", "2-7", "s@c.cl", "x", Rol.ESTUDIANTE);
        assertEquals("Bibliotecario", b.getTipo());
        assertEquals("Estudiante", s.getTipo());
        assertTrue(b.puedeGestionarEstudiantes() && b.puedeVerReportesGlobales());
        assertFalse(s.puedeGestionarEstudiantes() || s.puedeVerReportesGlobales());
    }

    @Test
    @DisplayName("un error de la base de datos al iniciar sesión llega como DAOException")
    void bdCaida() {
        DaosFalsos.caidos = true;
        assertThrows(DAOException.class, () -> autenticacion.iniciarSesion("a@correo.cl", "clave123"));
    }
}
