package vista;

import controlador.Sesion;
import main.Aplicacion;
import tareas.TareaSegundoPlano;

import javax.swing.*;
import java.awt.*;

/**
 * VISTA: pantalla de autenticación (JFrame). Pide correo y contraseña, valida contra la tabla usuarios y abre la
 * ventana principal con las funciones que corresponden al rol. La consulta se hace en un hilo aparte para que la
 * ventana no se congele si la base de datos tarda en responder.
 *
 * @author Rodolfo Delgado
 */
public class VentanaLogin extends JFrame {

    private final Aplicacion app;

    private final JTextField txtCorreo = new JTextField(22);
    private final JPasswordField txtClave = new JPasswordField(22);
    private final JButton btnIngresar = Estilo.boton("Ingresar");
    private final JLabel lblEstado = new JLabel(" ");

    public VentanaLogin(Aplicacion app) {
        this.app = app;

        setTitle("Biblioteca Escolar - Iniciar sesión");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        crearComponentes();
        pack();
        setMinimumSize(new Dimension(460, getHeight()));
        setLocationRelativeTo(null);
    }

    private void crearComponentes() {
        JPanel raiz = new JPanel(new BorderLayout(10, 10));
        raiz.setBorder(Estilo.margen(10));
        raiz.add(Estilo.encabezado("Biblioteca Escolar", "Ingrese con su correo y contraseña"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        Estilo.agregarFila(form, c, 0, "Correo:", txtCorreo);
        Estilo.agregarFila(form, c, 1, "Contraseña:", txtClave);

        JButton btnSalir = Estilo.boton("Salir");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        botones.add(btnIngresar);
        botones.add(btnSalir);

        lblEstado.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel sur = new JPanel(new BorderLayout());
        sur.add(botones, BorderLayout.NORTH);
        sur.add(lblEstado, BorderLayout.SOUTH);

        raiz.add(form, BorderLayout.CENTER);
        raiz.add(sur, BorderLayout.SOUTH);
        setContentPane(raiz);
        getRootPane().setDefaultButton(btnIngresar);   // Enter = Ingresar

        btnIngresar.addActionListener(e -> ingresar());
        btnSalir.addActionListener(e -> System.exit(0));
    }

    private void ingresar() {
        String correo = txtCorreo.getText();
        String clave = new String(txtClave.getPassword());
        setOcupado(true);

        TareaSegundoPlano.lanzar("Hilo-Login",
                () -> app.autenticacion.iniciarSesion(correo, clave),
                (Sesion sesion) -> SwingUtilities.invokeLater(() -> {
                    new VentanaPrincipal(app, sesion).setVisible(true);
                    dispose();
                }),
                ex -> SwingUtilities.invokeLater(() -> {
                    setOcupado(false);
                    txtClave.setText("");
                    Mensajes.error(this, ex);
                    txtClave.requestFocus();
                }));
    }

    private void setOcupado(boolean ocupado) {
        btnIngresar.setEnabled(!ocupado);
        txtCorreo.setEnabled(!ocupado);
        txtClave.setEnabled(!ocupado);
        lblEstado.setText(ocupado ? "Verificando credenciales..." : " ");
    }
}
