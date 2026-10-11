package dao.impl;

import dao.CategoriaDAO;
import dao.DatabaseConnection;
import modelo.Categoria;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de CategoriaDAO. Consultas parametrizadas (PreparedStatement) y try-with-resources.
 *
 * @author Rodolfo Delgado
 */
public class CategoriaDAOImpl implements CategoriaDAO {

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    @Override
    public void create(Categoria categoria) {
        db.ejecutar(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO categorias (nombre) VALUES (?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, categoria.getNombre());
                ps.executeUpdate();
                try (ResultSet claves = ps.getGeneratedKeys()) {
                    if (claves.next()) {
                        categoria.setId(claves.getInt(1));
                    }
                }
            }
            return null;
        });
    }

    @Override
    public List<Categoria> readAll() {
        return db.ejecutar(c -> {
            List<Categoria> lista = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT id, nombre FROM categorias ORDER BY nombre");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Categoria(rs.getInt("id"), rs.getString("nombre")));
                }
            }
            return lista;
        });
    }

    @Override
    public Categoria readById(int id) {
        return db.ejecutar(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT id, nombre FROM categorias WHERE id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? new Categoria(rs.getInt("id"), rs.getString("nombre")) : null;
                }
            }
        });
    }

    @Override
    public boolean update(Categoria categoria) {
        return db.ejecutar(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE categorias SET nombre = ? WHERE id = ?")) {
                ps.setString(1, categoria.getNombre());
                ps.setInt(2, categoria.getId());
                return ps.executeUpdate() > 0;
            }
        });
    }

    @Override
    public boolean delete(int id) {
        return db.ejecutar(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM categorias WHERE id = ?")) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            }
        });
    }

    @Override
    public int countLibros(int idCategoria) {
        return db.ejecutar(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM libros WHERE id_categoria = ?")) {
                ps.setInt(1, idCategoria);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        });
    }
}
