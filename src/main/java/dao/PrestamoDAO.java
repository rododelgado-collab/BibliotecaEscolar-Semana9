package dao;

import modelo.LibroPrestado;
import modelo.Prestamo;

import java.util.List;

/**
 * Persistencia de los préstamos (tabla prestamos, con JOIN a estudiantes y libros).
 *
 * Además del CRUD tiene dos operaciones TRANSACCIONALES que cambian préstamo y stock a la vez
 * ({@link #prestarLibro} y {@link #devolverLibro}), y las consultas de los reportes.
 *
 * @author Rodolfo Delgado
 */
public interface PrestamoDAO extends CrudDAO<Prestamo> {

    /**
     * Registra el préstamo Y descuenta una unidad del stock en una sola transacción. El descuento es
     * "UPDATE ... SET stock = stock - 1 WHERE stock > 0", así que ni siquiera la base de datos deja el stock bajo cero.
     *
     * @return true si se prestó; false si el libro ya no tenía stock (no se guarda nada)
     */
    boolean prestarLibro(Prestamo prestamo);

    /**
     * Marca el préstamo como devuelto Y repone una unidad al stock, en una sola transacción.
     *
     * @return true si se devolvió; false si el préstamo no existe o ya estaba devuelto
     */
    boolean devolverLibro(int idPrestamo);

    /** Historial completo de un estudiante, el más reciente primero. */
    List<Prestamo> readByEstudiante(int idEstudiante);

    /** Préstamos que todavía no se devuelven (de todos los estudiantes), el más antiguo primero. */
    List<Prestamo> readActivos();

    /** Préstamos sin devolver de un estudiante. */
    List<Prestamo> readActivosByEstudiante(int idEstudiante);

    /** Cuántos préstamos sin devolver tiene un estudiante (para el límite por estudiante). */
    int countActivosByEstudiante(int idEstudiante);

    /** ¿El estudiante ya tiene este libro sin devolver? */
    boolean existeActivo(int idEstudiante, int idLibro);

    /** Cuántos préstamos (devueltos o no) tiene un estudiante. */
    int countByEstudiante(int idEstudiante);

    /** Cuántos préstamos (devueltos o no) tiene un libro. */
    int countByLibro(int idLibro);

    /** Reporte: los libros más prestados, de mayor a menor. */
    List<LibroPrestado> readLibrosMasPrestados(int limite);
}
