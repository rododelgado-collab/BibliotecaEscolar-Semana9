package vista;

import controlador.ResultadoDevolucion;
import controlador.ResultadoPrestamo;
import controlador.Sesion;
import dao.DAOException;
import main.Aplicacion;
import modelo.Estudiante;
import modelo.Libro;
import modelo.Prestamo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * VISTA: préstamos y devoluciones.
 *
 * Registrar un préstamo y registrar una devolución se ejecutan en un HILO INDEPENDIENTE (ControladorPrestamos
 * .registrarPrestamoEnSegundoPlano): mientras trabaja se muestra una barra de progreso y la ventana sigue
 * respondiendo. Al terminar, el resultado vuelve al hilo de Swing con SwingUtilities.invokeLater.
 *
 * El bibliotecario presta a cualquier estudiante y ve todos los préstamos sin devolver; el estudiante solo puede
 * pedir libros a su nombre y ve sus propios préstamos.
 *
 * @author Rodolfo Delgado
 */
public class PanelPrestamos extends PanelBase {

    private static final String[] COLUMNAS = {"ID", "Estudiante", "Libro", "Prestado", "Vence", "Estado"};

    private final Aplicacion app;
    private final Sesion sesion;
    private final boolean veTodos;

    private final JComboBox<Estudiante> cmbEstudiante = new JComboBox<>();
    private final JComboBox<Libro> cmbLibro = new JComboBox<>();
    private final JLabel lblVencimiento = new JLabel(" ");
    private final JButton btnPrestar = Estilo.boton("Registrar préstamo");
    private final JButton btnDevolver = Estilo.boton("Registrar devolución");
    private final JProgressBar progreso = new JProgressBar();
    private final JLabel lblEstado = new JLabel(" ");

    private final DefaultTableModel modeloTabla = Estilo.modeloSoloLectura(COLUMNAS);
    private final JTable tabla = Estilo.tabla(modeloTabla, 0, 3, 4, 5);
    private final JLabel lblTotal = new JLabel(" ");

    public PanelPrestamos(Aplicacion app, Sesion sesion) {
        this.app = app;
        this.sesion = sesion;
        this.veTodos = sesion.usuario().puedePrestarATodos();
        crearComponentes();
    }

    private void crearComponentes() {
        setLayout(new BorderLayout(10, 10));
        add(Estilo.encabezado("Préstamos y devoluciones",
                veTodos ? "Registre préstamos (vencen a los " + Prestamo.PLAZO_DIAS + " días) y devoluciones"
                        : "Pida un libro prestado o devuelva uno de los suyos"), BorderLayout.NORTH);

        // --- Formulario de préstamo ---
        JPanel campos = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        Estilo.agregarFila(campos, c, 0, "Estudiante:", cmbEstudiante);
        Estilo.agregarFila(campos, c, 1, "Libro disponible:", cmbLibro);
        Estilo.agregarFila(campos, c, 2, "Fecha de vencimiento:", lblVencimiento);
        cmbEstudiante.setPrototypeDisplayValue(null);
        cmbEstudiante.setEnabled(veTodos);    // el estudiante solo puede pedir a su propio nombre
        cmbEstudiante.setPreferredSize(new Dimension(340, 26));
        cmbLibro.setPreferredSize(new Dimension(340, 26));

        progreso.setIndeterminate(true);
        progreso.setVisible(false);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        botones.add(btnPrestar);
        botones.add(progreso);
        botones.add(lblEstado);

        JPanel form = new JPanel(new BorderLayout());
        form.setBorder(BorderFactory.createTitledBorder("Nuevo préstamo"));
        form.add(campos, BorderLayout.CENTER);
        form.add(botones, BorderLayout.SOUTH);

        // --- Tabla de préstamos sin devolver ---
        Estilo.resaltarAtrasados(tabla, 5);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(40);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(220);
        JButton btnActualizar = Estilo.boton("Actualizar lista");
        JPanel accionesTabla = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        accionesTabla.add(btnDevolver);
        accionesTabla.add(btnActualizar);

        JPanel listado = new JPanel(new BorderLayout(0, 4));
        listado.setBorder(BorderFactory.createTitledBorder(veTodos ? "Libros actualmente en préstamo"
                : "Mis libros sin devolver"));
        listado.add(accionesTabla, BorderLayout.NORTH);
        listado.add(new JScrollPane(tabla), BorderLayout.CENTER);
        listado.add(lblTotal, BorderLayout.SOUTH);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.add(form, BorderLayout.NORTH);
        centro.add(listado, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        btnPrestar.addActionListener(e -> prestar());
        btnDevolver.addActionListener(e -> devolver());
        btnActualizar.addActionListener(e -> actualizar());
        cmbEstudiante.addActionListener(e -> actualizarVencimiento());
    }

    @Override
    public void actualizar() {
        try {
            // Combo de estudiantes
            cmbEstudiante.removeAllItems();
            if (veTodos) {
                for (Estudiante e : app.estudiantes.listar()) {
                    cmbEstudiante.addItem(e);
                }
            } else {
                cmbEstudiante.addItem(sesion.estudiante());
            }
            // Combo de libros con stock
            cmbLibro.removeAllItems();
            for (Libro l : app.libros.disponibles()) {
                cmbLibro.addItem(l);
            }
            cargarTabla();
        } catch (DAOException ex) {
            Mensajes.errorBD(this, ex.getMessage());
        }
        actualizarVencimiento();
    }

    private void actualizarVencimiento() {
        lblVencimiento.setText(app.prestamos.hoy().plusDays(Prestamo.PLAZO_DIAS) + "  (se calcula sola: hoy + "
                + Prestamo.PLAZO_DIAS + " días)");
    }

    private void cargarTabla() {
        LocalDate hoy = app.prestamos.hoy();
        List<Prestamo> lista = veTodos ? app.prestamos.activos() : app.prestamos.activosDe(sesion.estudiante().getId());
        modeloTabla.setRowCount(0);
        long atrasados = 0;
        for (Prestamo p : lista) {
            modeloTabla.addRow(new Object[]{p.getId(), p.getNombreEstudiante(), p.getTituloLibro(),
                    p.getFechaPrestamo(), p.getFechaVencimiento(), p.estado(hoy)});
            if (p.estaAtrasado(hoy)) {
                atrasados++;
            }
        }
        lblTotal.setText("Sin devolver: " + lista.size() + "   |   Atrasados: " + atrasados);
    }

    // ------------------------------------------------- Acciones en segundo plano

    private void prestar() {
        Estudiante estudiante = (Estudiante) cmbEstudiante.getSelectedItem();
        Libro libro = (Libro) cmbLibro.getSelectedItem();
        if (estudiante == null) {
            Mensajes.advertencia(this, "Seleccione un estudiante.");
            return;
        }
        if (libro == null) {
            Mensajes.advertencia(this, "No hay libros disponibles para prestar.");
            return;
        }
        setOcupado(true, "Registrando préstamo en segundo plano...");
        app.prestamos.registrarPrestamoEnSegundoPlano(estudiante.getId(), libro.getId(),
                (ResultadoPrestamo r) -> SwingUtilities.invokeLater(() -> {
                    setOcupado(false, " ");
                    Mensajes.exito(this, "Préstamo registrado",
                            "\"" + libro.getTitulo() + "\" prestado a " + estudiante.getNombre() + ".\n"
                                    + "Vence el " + r.prestamo().getFechaVencimiento() + ".\n"
                                    + "Stock restante: " + r.stockRestante() + "   |   Préstamos en su historial: "
                                    + r.totalHistorial());
                    actualizar();
                }),
                ex -> SwingUtilities.invokeLater(() -> {
                    setOcupado(false, " ");
                    Mensajes.error(this, ex);
                    actualizar();   // el stock pudo cambiar mientras tanto
                }));
    }

    private void devolver() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            Mensajes.advertencia(this, "Seleccione un préstamo de la tabla.");
            return;
        }
        int id = (int) modeloTabla.getValueAt(tabla.convertRowIndexToModel(fila), 0);
        setOcupado(true, "Registrando devolución en segundo plano...");
        app.prestamos.registrarDevolucionEnSegundoPlano(id,
                (ResultadoDevolucion r) -> SwingUtilities.invokeLater(() -> {
                    setOcupado(false, " ");
                    String texto = "\"" + r.prestamo().getTituloLibro() + "\" devuelto por " + r.prestamo().getNombreEstudiante() + ".";
                    if (r.tuvoAtraso()) {
                        Mensajes.advertencia(this, texto + "\n\nATRASO: se devolvió " + r.diasDeAtraso()
                                + " día(s) después de la fecha de vencimiento (" + r.prestamo().getFechaVencimiento()
                                + ").\nQuedó constancia en los avisos.");
                    } else {
                        Mensajes.exito(this, "Devolución registrada", texto + "\nSe devolvió a tiempo.");
                    }
                    actualizar();
                }),
                ex -> SwingUtilities.invokeLater(() -> {
                    setOcupado(false, " ");
                    Mensajes.error(this, ex);
                    actualizar();
                }));
    }

    private void setOcupado(boolean ocupado, String texto) {
        btnPrestar.setEnabled(!ocupado);
        btnDevolver.setEnabled(!ocupado);
        progreso.setVisible(ocupado);
        lblEstado.setText(texto);
    }
}
