package controlador;

import dao.EstudianteDAO;
import dao.LibroDAO;
import dao.PrestamoDAO;
import modelo.Estudiante;
import modelo.Libro;
import modelo.Prestamo;
import tareas.TareaSegundoPlano;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CONTROLADOR (MVC) de préstamos y devoluciones. Es la parte CONCURRENTE del sistema:
 *
 *  - HILO: registrarPrestamoEnSegundoPlano() ejecuta el proceso (guardar el préstamo, actualizar el stock y dejar
 *    el historial del estudiante) en un hilo independiente, así la interfaz no se congela.
 *  - SINCRONIZACIÓN: registrarPrestamo() y registrarDevolucion() son synchronized. Varios hilos pueden llegar al
 *    mismo tiempo (varias ventanas, varios usuarios), pero entran de a uno a la sección que verifica y modifica el
 *    stock; así no hay condiciones de carrera y el stock nunca queda negativo. Como segunda barrera, el DAO descuenta
 *    con "stock = stock - 1 WHERE stock > 0" dentro de una transacción.
 *
 * Importante: la sincronización funciona porque TODA la aplicación comparte una única instancia de este controlador
 * (se crea en VentanaPrincipal).
 *
 * @author Rodolfo Delgado
 */
public class ControladorPrestamos {

    /** Máximo de libros que un estudiante puede tener sin devolver al mismo tiempo. */
    public static final int MAX_PRESTAMOS_ACTIVOS = 3;

    private final PrestamoDAO prestamoDAO;
    private final EstudianteDAO estudianteDAO;
    private final LibroDAO libroDAO;
    private final Supplier<LocalDate> reloj;
    private final long demoraSimuladaMs;

    private volatile Consumer<String> bitacora = linea -> { };

    /** Constructor de la aplicación: fecha real y una pequeña demora que hace visible el trabajo del hilo. */
    public ControladorPrestamos(PrestamoDAO prestamoDAO, EstudianteDAO estudianteDAO, LibroDAO libroDAO) {
        this(prestamoDAO, estudianteDAO, libroDAO, LocalDate::now, 1200);
    }

    /** Constructor completo (las pruebas inyectan un reloj fijo y demora 0). */
    public ControladorPrestamos(PrestamoDAO prestamoDAO, EstudianteDAO estudianteDAO, LibroDAO libroDAO,
                                Supplier<LocalDate> reloj, long demoraSimuladaMs) {
        this.prestamoDAO = prestamoDAO;
        this.estudianteDAO = estudianteDAO;
        this.libroDAO = libroDAO;
        this.reloj = reloj;
        this.demoraSimuladaMs = demoraSimuladaMs;
    }

    /** Dónde se anotan los avisos (por ejemplo, la constancia de un atraso). La ventana principal lo conecta. */
    public void setBitacora(Consumer<String> bitacora) {
        this.bitacora = bitacora;
    }

    // ==================================================================
    // PRÉSTAMO
    // ==================================================================

    /**
     * Registra un préstamo: verifica disponibilidad, calcula la fecha de vencimiento (hoy + 7 días), guarda el
     * préstamo y descuenta el stock en una transacción. SYNCHRONIZED: una operación a la vez.
     *
     * @throws IllegalArgumentException con el motivo, listo para mostrar al usuario
     * @throws dao.DAOException         si falla la base de datos
     */
    public synchronized ResultadoPrestamo registrarPrestamo(int idEstudiante, int idLibro) {
        Estudiante estudiante = estudianteDAO.readById(idEstudiante);
        if (estudiante == null) {
            throw new IllegalArgumentException("Seleccione un estudiante.");
        }
        Libro libro = libroDAO.readById(idLibro);
        if (libro == null) {
            throw new IllegalArgumentException("Seleccione un libro.");
        }
        if (!libro.estaDisponible()) {
            throw new IllegalArgumentException("No hay stock disponible de \"" + libro.getTitulo() + "\".");
        }
        if (prestamoDAO.existeActivo(idEstudiante, idLibro)) {
            throw new IllegalArgumentException(estudiante.getNombre() + " ya tiene \"" + libro.getTitulo()
                    + "\" sin devolver.");
        }
        if (prestamoDAO.countActivosByEstudiante(idEstudiante) >= MAX_PRESTAMOS_ACTIVOS) {
            throw new IllegalArgumentException(estudiante.getNombre() + " ya tiene " + MAX_PRESTAMOS_ACTIVOS
                    + " libros sin devolver (el máximo). Debe devolver alguno antes de pedir otro.");
        }

        pausaSimulada();   // representa el tiempo de procesar el préstamo; ocurre DENTRO de la sección crítica

        Prestamo prestamo = Prestamo.nuevo(idEstudiante, idLibro, reloj.get());
        if (!prestamoDAO.prestarLibro(prestamo)) {
            throw new IllegalArgumentException("No hay stock disponible de \"" + libro.getTitulo() + "\".");
        }
        Libro actualizado = libroDAO.readById(idLibro);
        int historial = prestamoDAO.countByEstudiante(idEstudiante);
        return new ResultadoPrestamo(prestamo, actualizado == null ? 0 : actualizado.getStock(), historial);
    }

    /**
     * Lo mismo que registrarPrestamo pero en un HILO INDEPENDIENTE: vuelve de inmediato y avisa el resultado con
     * los callbacks (que se ejecutan en el hilo de trabajo; la ventana debe pasar a SwingUtilities.invokeLater).
     */
    public Thread registrarPrestamoEnSegundoPlano(int idEstudiante, int idLibro,
                                                  Consumer<ResultadoPrestamo> alExito,
                                                  Consumer<RuntimeException> alError) {
        return TareaSegundoPlano.lanzar("Hilo-Prestamo", () -> registrarPrestamo(idEstudiante, idLibro), alExito, alError);
    }

    // ==================================================================
    // DEVOLUCIÓN
    // ==================================================================

    /**
     * Registra la devolución: marca el préstamo como devuelto y repone el stock. Si se devolvió después de la fecha de
     * vencimiento deja constancia del atraso (en el resultado y en la bitácora). SYNCHRONIZED.
     */
    public synchronized ResultadoDevolucion registrarDevolucion(int idPrestamo) {
        if (idPrestamo <= 0) {
            throw new IllegalArgumentException("Seleccione un préstamo de la tabla.");
        }
        Prestamo prestamo = prestamoDAO.readById(idPrestamo);
        if (prestamo == null) {
            throw new IllegalArgumentException("El préstamo #" + idPrestamo + " ya no existe en la base de datos.");
        }
        if (prestamo.isDevuelto()) {
            throw new IllegalArgumentException("El préstamo #" + idPrestamo + " ya fue devuelto.");
        }
        long atraso = prestamo.diasDeAtraso(reloj.get());
        if (!prestamoDAO.devolverLibro(idPrestamo)) {
            throw new IllegalArgumentException("El préstamo #" + idPrestamo + " ya fue devuelto.");
        }
        prestamo.setDevuelto(true);
        if (atraso > 0) {
            bitacora.accept("ATRASO: \"" + prestamo.getTituloLibro() + "\" de " + prestamo.getNombreEstudiante()
                    + " se devolvió con " + atraso + " día(s) de atraso (vencía el " + prestamo.getFechaVencimiento() + ").");
        }
        return new ResultadoDevolucion(prestamo, atraso);
    }

    /** Devolución en un hilo independiente (igual que el préstamo). */
    public Thread registrarDevolucionEnSegundoPlano(int idPrestamo, Consumer<ResultadoDevolucion> alExito,
                                                    Consumer<RuntimeException> alError) {
        return TareaSegundoPlano.lanzar("Hilo-Devolucion", () -> registrarDevolucion(idPrestamo), alExito, alError);
    }

    // ==================================================================
    // CONSULTAS
    // ==================================================================

    public List<Prestamo> listar() {
        return prestamoDAO.readAll();
    }

    /** Historial completo de un estudiante. */
    public List<Prestamo> historialDe(int idEstudiante) {
        return prestamoDAO.readByEstudiante(idEstudiante);
    }

    /** Préstamos sin devolver (todos). */
    public List<Prestamo> activos() {
        return prestamoDAO.readActivos();
    }

    /** Préstamos sin devolver de un estudiante. */
    public List<Prestamo> activosDe(int idEstudiante) {
        return prestamoDAO.readActivosByEstudiante(idEstudiante);
    }

    public LocalDate hoy() {
        return reloj.get();
    }

    private void pausaSimulada() {
        if (demoraSimuladaMs > 0) {
            try {
                Thread.sleep(demoraSimuladaMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
