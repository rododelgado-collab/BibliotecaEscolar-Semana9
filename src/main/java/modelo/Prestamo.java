package modelo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Préstamo de un libro a un estudiante. Es también el HISTORIAL del estudiante: cada fila de la tabla
 * prestamos queda registrada para siempre (devuelta o no).
 *
 * La fecha de vencimiento se calcula sola al prestar (hoy + {@value #PLAZO_DIAS} días) y se guarda en la
 * columna fecha_devolucion. Mientras el préstamo no se devuelva y la fecha de vencimiento ya pasó, está atrasado.
 *
 * @author Rodolfo Delgado
 */
public class Prestamo {

    /** Días que dura un préstamo (igual que los datos de ejemplo del profesor: 7 días). */
    public static final int PLAZO_DIAS = 7;

    private int id;
    private final int idEstudiante;
    private final int idLibro;
    private final LocalDate fechaPrestamo;
    private final LocalDate fechaVencimiento;
    private boolean devuelto;

    // Datos de lectura (vienen de los JOIN; no se guardan desde aquí)
    private String nombreEstudiante;
    private String tituloLibro;

    public Prestamo(int id, int idEstudiante, int idLibro, LocalDate fechaPrestamo,
                    LocalDate fechaVencimiento, boolean devuelto) {
        this.id = id;
        this.idEstudiante = idEstudiante;
        this.idLibro = idLibro;
        this.fechaPrestamo = fechaPrestamo;
        this.fechaVencimiento = fechaVencimiento;
        this.devuelto = devuelto;
    }

    /** Préstamo nuevo con fecha de hoy y vencimiento calculado automáticamente. */
    public static Prestamo nuevo(int idEstudiante, int idLibro, LocalDate hoy) {
        return new Prestamo(0, idEstudiante, idLibro, hoy, hoy.plusDays(PLAZO_DIAS), false);
    }

    /** @return true si no se ha devuelto y la fecha de vencimiento ya pasó */
    public boolean estaAtrasado(LocalDate hoy) {
        return !devuelto && hoy.isAfter(fechaVencimiento);
    }

    /** Días de atraso a la fecha indicada (0 si no está atrasado). */
    public long diasDeAtraso(LocalDate hoy) {
        return estaAtrasado(hoy) ? ChronoUnit.DAYS.between(fechaVencimiento, hoy) : 0;
    }

    /** Texto para las tablas: Devuelto / Vigente / Atrasado (N días). */
    public String estado(LocalDate hoy) {
        if (devuelto) {
            return "Devuelto";
        }
        return estaAtrasado(hoy) ? "ATRASADO (" + diasDeAtraso(hoy) + " d)" : "Vigente";
    }

    public int getId()                      { return id; }
    public int getIdEstudiante()            { return idEstudiante; }
    public int getIdLibro()                 { return idLibro; }
    public LocalDate getFechaPrestamo()     { return fechaPrestamo; }
    public LocalDate getFechaVencimiento()  { return fechaVencimiento; }
    public boolean isDevuelto()             { return devuelto; }
    public String getNombreEstudiante()     { return nombreEstudiante; }
    public String getTituloLibro()          { return tituloLibro; }

    public void setId(int id)                       { this.id = id; }
    public void setDevuelto(boolean devuelto)       { this.devuelto = devuelto; }
    public void setNombreEstudiante(String nombre)  { this.nombreEstudiante = nombre; }
    public void setTituloLibro(String titulo)       { this.tituloLibro = titulo; }
}
