package vista;

import javax.swing.JPanel;

/**
 * Pantalla interna de la ventana principal. Cada pantalla sabe volver a leer sus datos desde la base de datos
 * ({@link #actualizar()}); la ventana principal lo llama cada vez que se navega hacia ella.
 *
 * @author Rodolfo Delgado
 */
public abstract class PanelBase extends JPanel {

    /** Vuelve a leer desde MySQL lo que muestra la pantalla. */
    public abstract void actualizar();
}
