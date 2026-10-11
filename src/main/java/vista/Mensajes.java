package vista;

import dao.DAOException;

import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * Cuadros de diálogo (JOptionPane) que usan todas las pantallas para dar retroalimentación al usuario.
 *
 * @author Rodolfo Delgado
 */
public final class Mensajes {

    private Mensajes() {
    }

    public static void exito(Component padre, String titulo, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, titulo, JOptionPane.INFORMATION_MESSAGE);
    }

    /** Datos inválidos o regla de negocio que impide la operación (el usuario puede corregirlo). */
    public static void advertencia(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Revise los datos", JOptionPane.WARNING_MESSAGE);
    }

    /** Falla de la base de datos (SQLException envuelta en DAOException). */
    public static void errorBD(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Error de base de datos", JOptionPane.ERROR_MESSAGE);
    }

    /** Pregunta Sí / No antes de una acción que no se puede deshacer. */
    public static boolean confirmar(Component padre, String mensaje) {
        return JOptionPane.showConfirmDialog(padre, mensaje, "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** Muestra cualquier error de una operación según su tipo: regla de negocio, base de datos o imprevisto. */
    public static void error(Component padre, Throwable ex) {
        if (ex instanceof IllegalArgumentException) {
            advertencia(padre, ex.getMessage());
        } else if (ex instanceof DAOException) {
            errorBD(padre, ex.getMessage());
        } else {
            JOptionPane.showMessageDialog(padre, "Ocurrió un error inesperado: " + ex, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
