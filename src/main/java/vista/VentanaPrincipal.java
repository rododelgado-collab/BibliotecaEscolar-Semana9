package vista;

import controlador.Sesion;
import main.Aplicacion;
import modelo.Usuario;

import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * VISTA: ventana principal (JFrame) con el panel de navegación. Solo muestra los botones que el rol del usuario
 * permite: el bibliotecario ve todo; el estudiante solo catálogo, préstamos/devoluciones e historial propio.
 * Los permisos se preguntan al objeto Usuario (polimorfismo), la ventana no compara roles.
 *
 * Distribución (BorderLayout):
 *   NORTH  -> encabezado con el usuario conectado
 *   WEST   -> botones de navegación (GridLayout)
 *   CENTER -> la pantalla elegida (CardLayout)
 *   SOUTH  -> bitácora de avisos
 *
 * @author Rodolfo Delgado
 */
public class VentanaPrincipal extends JFrame {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Aplicacion app;
    private final Sesion sesion;

    private final CardLayout cartas = new CardLayout();
    private final JPanel contenedor = new JPanel(cartas);
    private final Map<String, PanelBase> paneles = new LinkedHashMap<>();
    private final JTextArea txtBitacora = new JTextArea(3, 40);

    public VentanaPrincipal(Aplicacion app, Sesion sesion) {
        this.app = app;
        this.sesion = sesion;

        setTitle("Biblioteca Escolar - " + sesion.usuario().getNombre() + " (" + sesion.usuario().getTipo() + ")");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(980, 620));
        setLocationRelativeTo(null);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                salir();
            }
        });

        crearComponentes();

        // Los avisos del controlador de préstamos (por ejemplo la constancia de un atraso) llegan desde otros hilos
        app.prestamos.setBitacora(linea -> SwingUtilities.invokeLater(() -> registrar(linea)));
        registrar("Sesión iniciada: " + sesion.usuario().getNombre() + " (" + sesion.usuario().getTipo() + ").");
    }

    private void crearComponentes() {
        Usuario u = sesion.usuario();
        JPanel raiz = new JPanel(new BorderLayout(10, 10));
        raiz.setBorder(Estilo.margen(10));
        raiz.add(Estilo.encabezado("Biblioteca Escolar",
                "Conectado como " + u.getNombre() + " - rol " + u.getTipo()), BorderLayout.NORTH);

        // --- Pantallas según los permisos del rol ---
        paneles.put("Libros", new PanelLibros(app, u));
        if (u.puedeGestionarCatalogo()) {
            paneles.put("Categorías", new PanelCategorias(app));
        }
        if (u.puedeGestionarEstudiantes()) {
            paneles.put("Estudiantes", new PanelEstudiantes(app));
        }
        paneles.put("Préstamos y devoluciones", new PanelPrestamos(app, sesion));
        paneles.put("Reportes", new PanelReportes(app, sesion));
        paneles.forEach((nombre, panel) -> contenedor.add(panel, nombre));

        // --- Navegación (WEST) ---
        JPanel menu = new JPanel(new GridLayout(paneles.size() + 1, 1, 0, 8));
        menu.setBorder(BorderFactory.createTitledBorder("Menú"));
        ButtonGroup grupo = new ButtonGroup();
        for (String nombre : paneles.keySet()) {
            JToggleButton boton = new JToggleButton(nombre);
            boton.setFocusPainted(false);
            boton.addActionListener(e -> mostrar(nombre));
            grupo.add(boton);
            menu.add(boton);
        }
        JButton btnSalir = Estilo.boton("Cerrar sesión");
        btnSalir.addActionListener(e -> cerrarSesion());
        menu.add(btnSalir);
        JPanel contenedorMenu = new JPanel(new BorderLayout());
        contenedorMenu.add(menu, BorderLayout.NORTH);   // botones arriba, sin estirarse
        raiz.add(contenedorMenu, BorderLayout.WEST);

        raiz.add(contenedor, BorderLayout.CENTER);

        // --- Bitácora (SOUTH) ---
        txtBitacora.setEditable(false);
        txtBitacora.setLineWrap(true);
        txtBitacora.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(txtBitacora);
        scroll.setBorder(BorderFactory.createTitledBorder("Avisos"));
        raiz.add(scroll, BorderLayout.SOUTH);

        setContentPane(raiz);

        // Pantalla inicial: la primera del menú
        ((JToggleButton) menu.getComponent(0)).setSelected(true);
        mostrar(paneles.keySet().iterator().next());
    }

    private void mostrar(String nombre) {
        PanelBase panel = paneles.get(nombre);
        panel.actualizar();
        cartas.show(contenedor, nombre);
    }

    private void registrar(String mensaje) {
        txtBitacora.append(LocalTime.now().format(HORA) + "  " + mensaje + "\n");
        txtBitacora.setCaretPosition(txtBitacora.getDocument().getLength());
    }

    private void cerrarSesion() {
        if (Mensajes.confirmar(this, "¿Desea cerrar la sesión de " + sesion.usuario().getNombre() + "?")) {
            app.prestamos.setBitacora(linea -> { });
            new VentanaLogin(app).setVisible(true);
            dispose();
        }
    }

    private void salir() {
        if (Mensajes.confirmar(this, "¿Desea cerrar la Biblioteca Escolar?")) {
            dao.DatabaseConnection.getInstance().cerrar();
            System.exit(0);
        }
    }
}
