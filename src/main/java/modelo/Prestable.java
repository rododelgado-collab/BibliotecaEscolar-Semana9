package modelo;

/**
 * Contrato de todo material que se puede prestar. Hoy solo lo implementa {@link Libro},
 * pero así el módulo de préstamos queda desacoplado del tipo de material (revistas, mapas, etc.).
 *
 * @author Rodolfo Delgado
 */
public interface Prestable {

    /** @return true si queda al menos una unidad disponible para prestar */
    boolean estaDisponible();

    /** Descuenta una unidad del inventario. @throws IllegalStateException si no hay stock */
    void descontarUnidad();

    /** Devuelve una unidad al inventario. */
    void reponerUnidad();
}
