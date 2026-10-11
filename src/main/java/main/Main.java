package main;

import dao.DatabaseConnection;
import vista.VentanaLogin;

import javax.swing.*;
import java.sql.SQLException;

/**
 * Punto de entrada de la Biblioteca Escolar (Swing + JDBC + MySQL con MVC, DAO y Singleton).
 *
 * @author Rodolfo Delgado
 */
public class Main {

    public static void main(String[] args) {
        // Aspecto nativo del sistema operativo; si falla se usa el de Java.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("No se pudo aplicar el Look and Feel del sistema: " + e.getMessage());
        }

        // Se comprueba la conexión con MySQL (Singleton) antes de abrir la interfaz.
        try {
            DatabaseConnection.getInstance().probarConexion();
            System.out.println("[Main] Conexión exitosa con la base de datos (" + DatabaseConnection.archivoConfiguracion() + ").");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    DatabaseConnection.describirError(e)
                            + "\n\nLa aplicación se abrirá igualmente; cuando MySQL esté disponible"
                            + "\nvuelva a intentar iniciar sesión.",
                    "Biblioteca Escolar - Sin conexión a la base de datos", JOptionPane.ERROR_MESSAGE);
        }

        // La interfaz se crea en el Event Dispatch Thread de Swing.
        SwingUtilities.invokeLater(() -> new VentanaLogin(new Aplicacion()).setVisible(true));
    }
}
