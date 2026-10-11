package dao;

import modelo.Bibliotecario;
import modelo.Estudiante;
import modelo.Rol;
import modelo.Usuario;
import modelo.UsuarioEstudiante;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de UsuarioDAO y EstudianteDAO contra la base de pruebas. */
class UsuarioYEstudianteDAOTest extends BaseDaoTest {

    @Test
    @DisplayName("usuarios: CRUD completo; la columna `contraseña` y el rol se leen bien y crean la subclase correcta")
    void usuariosCrud() {
        Usuario b = Usuario.crear(0, "Antonia Pérez", "12345678-9", "antonia@correo.cl", "clave123", Rol.BIBLIOTECARIO);
        Usuario s = Usuario.crear(0, "Carlos Ruiz", "98765432-1", "carlos@correo.cl", "otra", Rol.ESTUDIANTE);
        usuarioDAO.create(b);
        usuarioDAO.create(s);
        assertTrue(b.getId() > 0 && s.getId() > 0);

        Usuario leidoB = usuarioDAO.readByCorreo("antonia@correo.cl");
        assertInstanceOf(Bibliotecario.class, leidoB);
        assertTrue(leidoB.autenticar("clave123"));
        assertFalse(leidoB.autenticar("mala"));
        assertInstanceOf(UsuarioEstudiante.class, usuarioDAO.readByRut("98765432-1"));
        assertNull(usuarioDAO.readByCorreo("nadie@correo.cl"));
        assertEquals(2, usuarioDAO.readAll().size());

        leidoB.setClave("nueva");
        assertTrue(usuarioDAO.update(leidoB));
        assertTrue(usuarioDAO.readById(b.getId()).autenticar("nueva"));

        assertTrue(usuarioDAO.delete(s.getId()));
        assertNull(usuarioDAO.readById(s.getId()));
    }

    @Test
    @DisplayName("estudiantes: CRUD completo y búsqueda por texto")
    void estudiantesCrud() {
        Estudiante e = guardarEstudiante("María Torres", "11222333-4");
        assertTrue(e.getId() > 0);
        assertEquals("María Torres", estudianteDAO.readById(e.getId()).getNombre());
        assertEquals(e.getId(), estudianteDAO.readByRut("11222333-4").getId());
        assertEquals(1, estudianteDAO.buscar("torres").size());
        assertEquals(0, estudianteDAO.buscar("zzz").size());

        Estudiante cambiado = new Estudiante(e.getId(), "María Torres Soto", e.getRut(), "4to Medio B", "maria.t@correo.cl");
        assertTrue(estudianteDAO.update(cambiado));
        assertEquals("4to Medio B", estudianteDAO.readById(e.getId()).getCurso());

        assertTrue(estudianteDAO.delete(e.getId()));
        assertNull(estudianteDAO.readById(e.getId()));
        assertFalse(estudianteDAO.delete(e.getId()));
    }

    @Test
    @DisplayName("registrarConUsuario guarda estudiante y usuario juntos; actualizar y eliminar también los mantienen sincronizados")
    void registrarConUsuario() {
        Estudiante e = new Estudiante("Ana Pérez", "12345678-5", "2do A", "ana@correo.cl");
        estudianteDAO.registrarConUsuario(e, "clave1");
        Usuario u = usuarioDAO.readByRut("12345678-5");
        assertNotNull(u);
        assertEquals(Rol.ESTUDIANTE, u.getRol());
        assertTrue(u.autenticar("clave1"));

        estudianteDAO.update(new Estudiante(e.getId(), "Ana Soto", e.getRut(), "3ro B", "ana.soto@correo.cl"));
        assertEquals("Ana Soto", usuarioDAO.readByRut("12345678-5").getNombre());
        assertEquals("ana.soto@correo.cl", usuarioDAO.readByRut("12345678-5").getCorreo());

        assertTrue(estudianteDAO.delete(e.getId()));
        assertNull(usuarioDAO.readByRut("12345678-5"));
    }

    @Test
    @DisplayName("TRANSACCIÓN: si falla el usuario (correo/RUT repetido) tampoco se guarda el estudiante")
    void registrarConUsuarioHaceRollback() {
        usuarioDAO.create(Usuario.crear(0, "Dueño del RUT", "12345678-5", "otro@correo.cl", "x", Rol.BIBLIOTECARIO));
        Estudiante e = new Estudiante("Ana Pérez", "12345678-5", "2do A", "ana@correo.cl");

        assertThrows(DAOException.class, () -> estudianteDAO.registrarConUsuario(e, "clave1"));

        assertNull(estudianteDAO.readByRut("12345678-5"), "el estudiante no debe quedar guardado a medias");
        assertEquals(0, estudianteDAO.readAll().size());
    }
}
