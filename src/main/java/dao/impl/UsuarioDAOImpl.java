package dao.impl;

import dao.DatabaseConnection;
import dao.UsuarioDAO;
import modelo.Rol;
import modelo.Usuario;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de UsuarioDAO sobre la tabla usuarios. Al leer, crea la subclase de Usuario que
 * corresponde al rol (Bibliotecario o UsuarioEstudiante). La columna de la clave se llama `contraseña`.
 *
 * @author Rodolfo Delgado
 */
public class UsuarioDAOImpl implements UsuarioDAO {

    private static final String SELECT_BASE = "SELECT id, nombre, rut, correo, `contraseña` AS clave, rol FROM usuarios ";

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    @Override
    public void create(Usuario usuario) {
        db.ejecutar(c -> {
            String sql = "INSERT INTO usuarios (nombre, rut, correo, `contraseña`, rol) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, usuario.getNombre());
                ps.setString(2, usuario.getRut());
                ps.setString(3, usuario.getCorreo());
                ps.setString(4, usuario.getClave());
                ps.setString(5, usuario.getRol().getValorBD());
                ps.executeUpdate();
                try (ResultSet claves = ps.getGeneratedKeys()) {
                    if (claves.next()) {
                        usuario.setId(claves.getInt(1));
                    }
                }
            }
            return null;
        });
    }

    @Override
    public List<Usuario> readAll() {
        return db.ejecutar(c -> {
            List<Usuario> lista = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(SELECT_BASE + "ORDER BY nombre");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        });
    }

    @Override
    public Usuario readById(int id) {
        return leerUno("WHERE id = ?", String.valueOf(id));
    }

    @Override
    public Usuario readByCorreo(String correo) {
        return leerUno("WHERE correo = ?", correo);
    }

    @Override
    public Usuario readByRut(String rut) {
        return leerUno("WHERE rut = ?", rut);
    }

    @Override
    public boolean update(Usuario usuario) {
        return db.ejecutar(c -> {
            String sql = "UPDATE usuarios SET nombre = ?, rut = ?, correo = ?, `contraseña` = ?, rol = ? WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, usuario.getNombre());
                ps.setString(2, usuario.getRut());
                ps.setString(3, usuario.getCorreo());
                ps.setString(4, usuario.getClave());
                ps.setString(5, usuario.getRol().getValorBD());
                ps.setInt(6, usuario.getId());
                return ps.executeUpdate() > 0;
            }
        });
    }

    @Override
    public boolean delete(int id) {
        return db.ejecutar(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM usuarios WHERE id = ?")) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            }
        });
    }

    private Usuario leerUno(String where, String valor) {
        return db.ejecutar(c -> {
            try (PreparedStatement ps = c.prepareStatement(SELECT_BASE + where)) {
                if (where.contains("id =")) {
                    ps.setInt(1, Integer.parseInt(valor));
                } else {
                    ps.setString(1, valor);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? mapear(rs) : null;
                }
            }
        });
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        return Usuario.crear(rs.getInt("id"), rs.getString("nombre"), rs.getString("rut"), rs.getString("correo"),
                rs.getString("clave"), Rol.desdeBD(rs.getString("rol")));
    }
}
