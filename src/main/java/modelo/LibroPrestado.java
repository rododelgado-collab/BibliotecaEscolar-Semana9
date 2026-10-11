package modelo;

/**
 * Fila del reporte "libros más prestados": un libro y cuántas veces se ha prestado.
 *
 * @author Rodolfo Delgado
 */
public record LibroPrestado(String titulo, String autor, int totalPrestamos) {
}
