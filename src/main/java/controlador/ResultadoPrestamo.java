package controlador;

import modelo.Prestamo;

/**
 * Lo que devuelve un préstamo registrado: el préstamo, el stock que quedó del libro y cuántos préstamos
 * lleva ahora el estudiante en su historial.
 *
 * @author Rodolfo Delgado
 */
public record ResultadoPrestamo(Prestamo prestamo, int stockRestante, int totalHistorial) {
}
