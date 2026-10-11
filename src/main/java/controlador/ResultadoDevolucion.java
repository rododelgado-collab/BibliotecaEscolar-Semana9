package controlador;

import modelo.Prestamo;

/**
 * Lo que devuelve una devolución registrada: el préstamo y los días de atraso (0 si se devolvió a tiempo).
 *
 * @author Rodolfo Delgado
 */
public record ResultadoDevolucion(Prestamo prestamo, long diasDeAtraso) {

    public boolean tuvoAtraso() {
        return diasDeAtraso > 0;
    }
}
