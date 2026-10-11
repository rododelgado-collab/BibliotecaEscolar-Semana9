package modelo;

/**
 * Rol de un usuario. Los valores coinciden con el ENUM('bibliotecario','estudiante') de la tabla usuarios.
 *
 * @author Rodolfo Delgado
 */
public enum Rol {
    BIBLIOTECARIO("bibliotecario", "Bibliotecario"),
    ESTUDIANTE("estudiante", "Estudiante");

    private final String valorBD;
    private final String descripcion;

    Rol(String valorBD, String descripcion) {
        this.valorBD = valorBD;
        this.descripcion = descripcion;
    }

    /** Texto tal como se guarda en MySQL. */
    public String getValorBD() {
        return valorBD;
    }

    /** Convierte el texto de MySQL en el enum. @throws IllegalArgumentException si no existe */
    public static Rol desdeBD(String valor) {
        for (Rol r : values()) {
            if (r.valorBD.equalsIgnoreCase(valor)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Rol desconocido: " + valor);
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
