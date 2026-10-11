package vista;

import controlador.Sesion;
import dao.DAOException;
import main.Aplicacion;
import modelo.Estudiante;
import modelo.LibroPrestado;
import modelo.Prestamo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * VISTA: reportes. El bibliotecario ve los tres informes (libros más prestados, historial por estudiante y libros
 * actualmente en préstamo); el estudiante solo ve su propio historial.
 *
 * @author Rodolfo Delgado
 */
public class PanelReportes extends PanelBase {

    private static final int TOP = 10;

    private final Aplicacion app;
    private final Sesion sesion;
    private final boolean global;

    private final DefaultTableModel modeloTop = Estilo.modeloSoloLectura("Puesto", "Título", "Autor", "Veces prestado");
    private final DefaultTableModel modeloHistorial = Estilo.modeloSoloLectura("ID", "Libro", "Prestado", "Vence", "Estado");
    private final DefaultTableModel modeloEnPrestamo =
            Estilo.modeloSoloLectura("ID", "Libro", "Estudiante", "Prestado", "Vence", "Estado");
    private final JComboBox<Estudiante> cmbEstudiante = new JComboBox<>();
    private final JLabel lblHistorial = new JLabel(" ");
    private final JLabel lblEnPrestamo = new JLabel(" ");
    private boolean cargandoCombo = false;

    public PanelReportes(Aplicacion app, Sesion sesion) {
        this.app = app;
        this.sesion = sesion;
        this.global = sesion.usuario().puedeVerReportesGlobales();
        crearComponentes();
    }

    private void crearComponentes() {
        setLayout(new BorderLayout(10, 10));
        add(Estilo.encabezado("Reportes", global ? "Decisiones de gestión: lo más prestado, historiales y préstamos vigentes"
                : "Su historial de préstamos"), BorderLayout.NORTH);

        JTabbedPane pestanas = new JTabbedPane();

        if (global) {
            JTable tablaTop = Estilo.tabla(modeloTop, 0, 3);
            tablaTop.getColumnModel().getColumn(0).setPreferredWidth(50);
            tablaTop.getColumnModel().getColumn(1).setPreferredWidth(300);
            pestanas.addTab("Libros más prestados", new JScrollPane(tablaTop));
        }

        // Historial por estudiante
        JTable tablaHistorial = Estilo.tabla(modeloHistorial, 0, 2, 3, 4);
        Estilo.resaltarAtrasados(tablaHistorial, 4);
        JPanel selector = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        selector.add(new JLabel("Estudiante:"));
        cmbEstudiante.setPreferredSize(new Dimension(320, 26));
        cmbEstudiante.setEnabled(global);
        selector.add(cmbEstudiante);
        JPanel panelHistorial = new JPanel(new BorderLayout(0, 4));
        panelHistorial.add(selector, BorderLayout.NORTH);
        panelHistorial.add(new JScrollPane(tablaHistorial), BorderLayout.CENTER);
        panelHistorial.add(lblHistorial, BorderLayout.SOUTH);
        pestanas.addTab("Historial por estudiante", panelHistorial);
        cmbEstudiante.addActionListener(e -> {
            if (!cargandoCombo) {
                cargarHistorial();
            }
        });

        if (global) {
            JTable tablaEnPrestamo = Estilo.tabla(modeloEnPrestamo, 0, 3, 4, 5);
            Estilo.resaltarAtrasados(tablaEnPrestamo, 5);
            JPanel panelEnPrestamo = new JPanel(new BorderLayout(0, 4));
            panelEnPrestamo.add(new JScrollPane(tablaEnPrestamo), BorderLayout.CENTER);
            panelEnPrestamo.add(lblEnPrestamo, BorderLayout.SOUTH);
            pestanas.addTab("Libros en préstamo", panelEnPrestamo);
        }

        JButton btnActualizar = Estilo.boton("Actualizar reportes");
        btnActualizar.addActionListener(e -> actualizar());
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sur.add(btnActualizar);

        add(pestanas, BorderLayout.CENTER);
        add(sur, BorderLayout.SOUTH);
    }


    @Override
    public void actualizar() {
        try {
            cargandoCombo = true;
            Estudiante previo = (Estudiante) cmbEstudiante.getSelectedItem();
            cmbEstudiante.removeAllItems();
            if (global) {
                for (Estudiante e : app.estudiantes.listar()) {
                    cmbEstudiante.addItem(e);
                    if (previo != null && previo.getId() == e.getId()) {
                        cmbEstudiante.setSelectedItem(e);
                    }
                }
            } else {
                cmbEstudiante.addItem(sesion.estudiante());
            }
            cargandoCombo = false;

            cargarHistorial();
            if (global) {
                cargarTop();
                cargarEnPrestamo();
            }
        } catch (DAOException ex) {
            cargandoCombo = false;
            Mensajes.errorBD(this, ex.getMessage());
        }
    }

    private void cargarTop() {
        modeloTop.setRowCount(0);
        int puesto = 1;
        for (LibroPrestado l : app.reportes.librosMasPrestados(TOP)) {
            modeloTop.addRow(new Object[]{puesto++, l.titulo(), l.autor(), l.totalPrestamos()});
        }
    }

    private void cargarHistorial() {
        modeloHistorial.setRowCount(0);
        Estudiante e = (Estudiante) cmbEstudiante.getSelectedItem();
        if (e == null) {
            lblHistorial.setText("Seleccione un estudiante.");
            return;
        }
        try {
            LocalDate hoy = app.prestamos.hoy();
            List<Prestamo> historial = app.reportes.historialPorEstudiante(e.getId());
            for (Prestamo p : historial) {
                modeloHistorial.addRow(new Object[]{p.getId(), p.getTituloLibro(), p.getFechaPrestamo(),
                        p.getFechaVencimiento(), p.estado(hoy)});
            }
            lblHistorial.setText("Préstamos en el historial de " + e.getNombre() + ": " + historial.size()
                    + "   |   Atrasados: " + app.reportes.contarAtrasados(historial, hoy));
        } catch (DAOException ex) {
            Mensajes.errorBD(this, ex.getMessage());
        }
    }

    private void cargarEnPrestamo() {
        LocalDate hoy = app.prestamos.hoy();
        List<Prestamo> lista = app.reportes.librosEnPrestamo();
        modeloEnPrestamo.setRowCount(0);
        for (Prestamo p : lista) {
            modeloEnPrestamo.addRow(new Object[]{p.getId(), p.getTituloLibro(), p.getNombreEstudiante(),
                    p.getFechaPrestamo(), p.getFechaVencimiento(), p.estado(hoy)});
        }
        lblEnPrestamo.setText("Libros en préstamo: " + lista.size() + "   |   Atrasados: "
                + app.reportes.contarAtrasados(lista, hoy));
    }
}
