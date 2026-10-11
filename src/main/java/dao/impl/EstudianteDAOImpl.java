package dao.impl;

import dao.DatabaseConnection;
import dao.EstudianteDAO;
import modelo.Estudiante;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de EstudianteDAO sobre la tabla estudiantes. Las operaciones que tocan también la cuenta
 * de usuario (registrar, actualizar y eliminar) se ejecutan en una transacción para no dejar datos a medias.
 *
 * @author Rodolfo Delgado
 */
public class EstudianteDAOImpl implements EstudianteDAO {

    private static final String SELECT_BASE = "SELECT id, nombre, rut, curso, correo FROM estudiantes ";

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    @Override
    public void create(Estudiante estudiante) {
        db.ejecutar(c -> {
            insertar(c, estudiante);
            return null;
        });
    }

    @Override
    public void registrarConUsuario(Estudiante estudiante, String clave) {
        db.transaccion(c -> {
            insertar(c, estudiante);
            String sql = "INSERT INTO usuarios (nombre, rut, correo, `contraseña`, rol) VALUES (?, ?, ?, ?, 'estudiante')";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, estudiante.getNombre());
                ps.setString(2, estudiante.getRut());
                ps.setString(3, estudiante.getCorreo());
                ps.setString(4, clave);
                ps.executeUpdate();
            }
            return null;
        });
    }

    private void insertar(java.sql.Connection c, Estudiante estudiante) throws SQLException {
        String sql = "INSERT INTO estudiantes (nombre, rut, curso, correo) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, estudiante.getNombre());
            ps.setString(2, estudiante.getRut());
            ps.setString(3, estudiante.getCurso());
            ps.setString(4, estudiante.getCorreo());
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    estudiante.setId(claves.getInt(1));
                }
            }
        }
    }

    @Override
    public List<Estudiante> readAll() {
        return buscar("");
    }

    @Override
    public List<Estudiante> buscar(String texto) {
        String patron = "%" + (texto == null ? "" : texto.trim()) + "%";
        return db.ejecutar(c -> {
            String sql = SELECT_BASE + "WHERE nombre LIKE ? OR rut LIKE ? OR curso LIKE ? OR correo LIKE ? ORDER BY nombre";
            List<Estudiante> lista = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 1; i <= 4; i++) {
                    ps.setString(i, patron);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                }
            }
            return lista;
        });
    }

    @Override
    public Estudiante readById(int id) {
        return db.ejecutar(c -> {
            try (PreparedStatement ps = c.prepareStatement(SELECT_BASE + "WHERE id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? mapear(rs) : null;
                }
            }
        });
    }

    @Override
    public Estudiante readByRut(String rut) {
        return db.ejecutar(c -> {
            try (PreparedStatement ps = c.prepareStatement(SELECT_BASE + "WHERE rut = ?")) {
                ps.setString(1, rut);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? mapear(rs) : null;
                }
            }
        });
    }

    /** Actualiza nombre, curso y correo; la cuenta de usuario con el mismo RUT recibe el nombre y el correo nuevos. */
    @Override
    public boolean update(Estudiante estudiante) {
        return db.transaccion(c -> {
            boolean existia;
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE estudiantes SET nombre = ?, curso = ?, correo = ? WHERE id = ?")) {
                ps.setString(1, estudiante.getNombre());
                ps.setString(2, estudiante.getCurso());
                ps.setString(3, estudiante.getCorreo());
                ps.setInt(4, estudiante.getId());
                existia = ps.executeUpdate() > 0;
            }
            if (existia) {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE usuarios SET nombre = ?, correo = ? WHERE rut = ? AND rol = 'estudiante'")) {
                    ps.setString(1, estudiante.getNombre());
                    ps.setString(2, estudiante.getCorreo());
                    ps.setString(3, estudiante.getRut());
                    ps.executeUpdate();
                }
            }
            return existia;
        });
    }

    /** Elimina al estudiante y su cuenta de usuario. Falla (1451) si tiene préstamos. */
    @Override
    public boolean delete(int id) {
        return db.transaccion(c -> {
            String rut = null;
            try (PreparedStatement ps = c.prepareStatement("SELECT rut FROM estudiantes WHERE id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        rut = rs.getString(1);
                    }
                }
            }
            if (rut == null) {
                return false;
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM estudiantes WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM usuarios WHERE rut = ? AND rol = 'estudiante'")) {
                ps.setString(1, rut);
                ps.executeUpdate();
            }
            return true;
        });
    }

    private Estudiante mapear(ResultSet rs) throws SQLException {
        return new Estudiante(rs.getInt("id"), rs.getString("nombre"), rs.getString("rut"),
                rs.getString("curso"), rs.getString("correo"));
    }
}
