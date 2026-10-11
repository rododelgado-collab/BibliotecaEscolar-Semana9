package dao.impl;

import dao.DatabaseConnection;
import dao.PrestamoDAO;
import modelo.LibroPrestado;
import modelo.Prestamo;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de PrestamoDAO sobre la tabla prestamos. Los JOIN con estudiantes y libros traen
 * los nombres para mostrarlos en las tablas y reportes. Las operaciones que cambian el stock
 * (prestarLibro y devolverLibro) son transacciones.
 *
 * @author Rodolfo Delgado
 */
public class PrestamoDAOImpl implements PrestamoDAO {

    private static final String SELECT_BASE =
            "SELECT p.id, p.id_estudiante, p.id_libro, p.fecha_prestamo, p.fecha_devolucion, p.devuelto, "
                    + "e.nombre AS estudiante, l.titulo AS libro "
                    + "FROM prestamos p "
                    + "JOIN estudiantes e ON e.id = p.id_estudiante "
                    + "JOIN libros l ON l.id = p.id_libro ";

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    // ------------------------------------------------------------------ CRUD

    @Override
    public void create(Prestamo prestamo) {
        db.ejecutar(c -> {
            insertar(c, prestamo);
            return null;
        });
    }

    private void insertar(Connection c, Prestamo p) throws SQLException {
        String sql = "INSERT INTO prestamos (id_estudiante, id_libro, fecha_prestamo, fecha_devolucion, devuelto) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getIdEstudiante());
            ps.setInt(2, p.getIdLibro());
            ps.setDate(3, Date.valueOf(p.getFechaPrestamo()));
            ps.setDate(4, Date.valueOf(p.getFechaVencimiento()));
            ps.setBoolean(5, p.isDevuelto());
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    p.setId(claves.getInt(1));
                }
            }
        }
    }

    @Override
    public List<Prestamo> readAll() {
        return listar("ORDER BY p.id DESC", null);
    }

    @Override
    public Prestamo readById(int id) {
        List<Prestamo> lista = listar("WHERE p.id = ?", id);
        return lista.isEmpty() ? null : lista.get(0);
    }

    @Override
    public boolean update(Prestamo p) {
        return db.ejecutar(c -> {
            String sql = "UPDATE prestamos SET id_estudiante = ?, id_libro = ?, fecha_prestamo = ?, "
                    + "fecha_devolucion = ?, devuelto = ? WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, p.getIdEstudiante());
                ps.setInt(2, p.getIdLibro());
                ps.setDate(3, Date.valueOf(p.getFechaPrestamo()));
                ps.setDate(4, Date.valueOf(p.getFechaVencimiento()));
                ps.setBoolean(5, p.isDevuelto());
                ps.setInt(6, p.getId());
                return ps.executeUpdate() > 0;
            }
        });
    }

    /** Elimina el préstamo (corrección administrativa). Si estaba sin devolver, repone la unidad al stock. */
    @Override
    public boolean delete(int id) {
        return db.transaccion(c -> {
            int idLibro = -1;
            boolean devuelto = true;
            try (PreparedStatement ps = c.prepareStatement("SELECT id_libro, devuelto FROM prestamos WHERE id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        idLibro = rs.getInt(1);
                        devuelto = rs.getBoolean(2);
                    }
                }
            }
            if (idLibro < 0) {
                return false;
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM prestamos WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            if (!devuelto) {
                cambiarStock(c, idLibro, +1);
            }
            return true;
        });
    }

    // ------------------------------------------------------- Transacciones de stock

    @Override
    public boolean prestarLibro(Prestamo prestamo) {
        return db.transaccion(c -> {
            // 1) Descuenta una unidad SOLO si queda stock (la condición stock > 0 es atómica en la base de datos)
            if (!cambiarStock(c, prestamo.getIdLibro(), -1)) {
                return false;
            }
            // 2) Registra el préstamo (queda también en el historial del estudiante)
            insertar(c, prestamo);
            return true;
        });
    }

    @Override
    public boolean devolverLibro(int idPrestamo) {
        return db.transaccion(c -> {
            int idLibro = -1;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id_libro FROM prestamos WHERE id = ? AND devuelto = FALSE")) {
                ps.setInt(1, idPrestamo);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        idLibro = rs.getInt(1);
                    }
                }
            }
            if (idLibro < 0) {
                return false;
            }
            try (PreparedStatement ps = c.prepareStatement("UPDATE prestamos SET devuelto = TRUE WHERE id = ?")) {
                ps.setInt(1, idPrestamo);
                ps.executeUpdate();
            }
            cambiarStock(c, idLibro, +1);
            return true;
        });
    }

    /** Suma o resta una unidad. Al restar exige stock > 0. @return true si se modificó una fila */
    private boolean cambiarStock(Connection c, int idLibro, int delta) throws SQLException {
        String sql = delta < 0
                ? "UPDATE libros SET stock = stock - 1 WHERE id = ? AND stock > 0"
                : "UPDATE libros SET stock = stock + 1 WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idLibro);
            return ps.executeUpdate() > 0;
        }
    }

    // ------------------------------------------------------------- Consultas

    @Override
    public List<Prestamo> readByEstudiante(int idEstudiante) {
        return listar("WHERE p.id_estudiante = ? ORDER BY p.fecha_prestamo DESC, p.id DESC", idEstudiante);
    }

    @Override
    public List<Prestamo> readActivos() {
        return listar("WHERE p.devuelto = FALSE ORDER BY p.fecha_devolucion, p.id", null);
    }

    @Override
    public List<Prestamo> readActivosByEstudiante(int idEstudiante) {
        return listar("WHERE p.devuelto = FALSE AND p.id_estudiante = ? ORDER BY p.fecha_devolucion, p.id", idEstudiante);
    }

    @Override
    public int countActivosByEstudiante(int idEstudiante) {
        return contar("SELECT COUNT(*) FROM prestamos WHERE devuelto = FALSE AND id_estudiante = ?", idEstudiante);
    }

    @Override
    public boolean existeActivo(int idEstudiante, int idLibro) {
        return db.ejecutar(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM prestamos WHERE devuelto = FALSE AND id_estudiante = ? AND id_libro = ?")) {
                ps.setInt(1, idEstudiante);
                ps.setInt(2, idLibro);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        });
    }

    @Override
    public int countByEstudiante(int idEstudiante) {
        return contar("SELECT COUNT(*) FROM prestamos WHERE id_estudiante = ?", idEstudiante);
    }

    @Override
    public int countByLibro(int idLibro) {
        return contar("SELECT COUNT(*) FROM prestamos WHERE id_libro = ?", idLibro);
    }

    @Override
    public List<LibroPrestado> readLibrosMasPrestados(int limite) {
        return db.ejecutar(c -> {
            String sql = "SELECT l.titulo, l.autor, COUNT(p.id) AS total "
                    + "FROM prestamos p JOIN libros l ON l.id = p.id_libro "
                    + "GROUP BY l.id, l.titulo, l.autor ORDER BY total DESC, l.titulo LIMIT ?";
            List<LibroPrestado> lista = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, limite);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lista.add(new LibroPrestado(rs.getString("titulo"), rs.getString("autor"), rs.getInt("total")));
                    }
                }
            }
            return lista;
        });
    }

    // ---------------------------------------------------------------- Ayudas

    private int contar(String sql, int parametro) {
        return db.ejecutar(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, parametro);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        });
    }

    /** SELECT_BASE + el WHERE/ORDER BY indicado; el parámetro (si hay) es un entero. */
    private List<Prestamo> listar(String complemento, Integer parametro) {
        return db.ejecutar(c -> {
            List<Prestamo> lista = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(SELECT_BASE + complemento)) {
                if (parametro != null) {
                    ps.setInt(1, parametro);
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

    private Prestamo mapear(ResultSet rs) throws SQLException {
        Prestamo p = new Prestamo(rs.getInt("id"), rs.getInt("id_estudiante"), rs.getInt("id_libro"),
                rs.getDate("fecha_prestamo").toLocalDate(), rs.getDate("fecha_devolucion").toLocalDate(),
                rs.getBoolean("devuelto"));
        p.setNombreEstudiante(rs.getString("estudiante"));
        p.setTituloLibro(rs.getString("libro"));
        return p;
    }
}
