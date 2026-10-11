package dao.impl;

import dao.DatabaseConnection;
import dao.LibroDAO;
import modelo.Categoria;
import modelo.Libro;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de LibroDAO sobre la tabla libros. El LEFT JOIN trae el nombre de la categoría.
 *
 * @author Rodolfo Delgado
 */
public class LibroDAOImpl implements LibroDAO {

    private static final String SELECT_BASE =
            "SELECT l.id, l.titulo, l.autor, l.isbn, l.editorial, l.stock, l.id_categoria, c.nombre AS categoria "
                    + "FROM libros l LEFT JOIN categorias c ON c.id = l.id_categoria ";

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    @Override
    public void create(Libro libro) {
        db.ejecutar(c -> {
            String sql = "INSERT INTO libros (titulo, autor, isbn, editorial, stock, id_categoria) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                asignar(ps, libro);
                ps.executeUpdate();
                try (ResultSet claves = ps.getGeneratedKeys()) {
                    if (claves.next()) {
                        libro.setId(claves.getInt(1));
                    }
                }
            }
            return null;
        });
    }

    @Override
    public List<Libro> readAll() {
        return buscar("");
    }

    @Override
    public Libro readById(int id) {
        return db.ejecutar(c -> {
            try (PreparedStatement ps = c.prepareStatement(SELECT_BASE + "WHERE l.id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? mapear(rs) : null;
                }
            }
        });
    }

    @Override
    public Libro readByIsbn(String isbn) {
        return db.ejecutar(c -> {
            try (PreparedStatement ps = c.prepareStatement(SELECT_BASE + "WHERE l.isbn = ?")) {
                ps.setString(1, isbn);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? mapear(rs) : null;
                }
            }
        });
    }

    @Override
    public List<Libro> buscar(String texto) {
        String patron = "%" + (texto == null ? "" : texto.trim()) + "%";
        return db.ejecutar(c -> {
            String sql = SELECT_BASE
                    + "WHERE l.titulo LIKE ? OR l.autor LIKE ? OR l.isbn LIKE ? OR l.editorial LIKE ? "
                    + "ORDER BY l.titulo";
            List<Libro> lista = new ArrayList<>();
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
    public List<Libro> readDisponibles() {
        return db.ejecutar(c -> {
            List<Libro> lista = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(SELECT_BASE + "WHERE l.stock > 0 ORDER BY l.titulo");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        });
    }

    @Override
    public boolean update(Libro libro) {
        return db.ejecutar(c -> {
            String sql = "UPDATE libros SET titulo = ?, autor = ?, isbn = ?, editorial = ?, stock = ?, id_categoria = ? WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                asignar(ps, libro);
                ps.setInt(7, libro.getId());
                return ps.executeUpdate() > 0;
            }
        });
    }

    @Override
    public boolean delete(int id) {
        return db.ejecutar(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM libros WHERE id = ?")) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            }
        });
    }

    private void asignar(PreparedStatement ps, Libro libro) throws SQLException {
        ps.setString(1, libro.getTitulo());
        ps.setString(2, libro.getAutor());
        ps.setString(3, libro.getIsbn());
        ps.setString(4, libro.getEditorial());
        ps.setInt(5, libro.getStock());
        if (libro.getCategoria() == null) {
            ps.setNull(6, java.sql.Types.INTEGER);
        } else {
            ps.setInt(6, libro.getCategoria().getId());
        }
    }

    private Libro mapear(ResultSet rs) throws SQLException {
        int idCategoria = rs.getInt("id_categoria");
        Categoria categoria = rs.wasNull() ? null : new Categoria(idCategoria, rs.getString("categoria"));
        return new Libro(rs.getInt("id"), rs.getString("titulo"), rs.getString("autor"), rs.getString("isbn"),
                rs.getString("editorial"), rs.getInt("stock"), categoria);
    }
}
