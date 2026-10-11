package controlador;

import dao.PrestamoDAO;
import modelo.LibroPrestado;
import modelo.Prestamo;

import java.time.LocalDate;
import java.util.List;

/**
 * CONTROLADOR (MVC) de los reportes: libros más prestados, historial por estudiante y listado de libros
 * actualmente en préstamo (con los atrasados señalados). Solo consulta; nunca modifica datos.
 *
 * @author Rodolfo Delgado
 */
public class ControladorReportes {

    private final PrestamoDAO prestamoDAO;

    public ControladorReportes(PrestamoDAO prestamoDAO) {
        this.prestamoDAO = prestamoDAO;
    }

    /** Los libros más prestados, de mayor a menor. */
    public List<LibroPrestado> librosMasPrestados(int limite) {
        if (limite <= 0) {
            throw new IllegalArgumentException("El límite debe ser mayor que cero.");
        }
        return prestamoDAO.readLibrosMasPrestados(limite);
    }

    /** Historial de préstamos de un estudiante. */
    public List<Prestamo> historialPorEstudiante(int idEstudiante) {
        if (idEstudiante <= 0) {
            throw new IllegalArgumentException("Seleccione un estudiante.");
        }
        return prestamoDAO.readByEstudiante(idEstudiante);
    }

    /** Libros actualmente en préstamo (sin devolver). */
    public List<Prestamo> librosEnPrestamo() {
        return prestamoDAO.readActivos();
    }

    /** Cuántos préstamos sin devolver ya pasaron su fecha de vencimiento. */
    public long contarAtrasados(List<Prestamo> prestamos, LocalDate hoy) {
        return prestamos.stream().filter(p -> p.estaAtrasado(hoy)).count();
    }
}
