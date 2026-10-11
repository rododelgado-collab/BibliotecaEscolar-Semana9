package vista;

import dao.DAOException;
import main.Aplicacion;
import modelo.Estudiante;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * VISTA: gestión de estudiantes (solo bibliotecario). Al registrar un estudiante también se crea su cuenta de
 * usuario (el correo es su nombre de usuario). El RUT no se puede cambiar una vez registrado.
 *
 * @author Rodolfo Delgado
 */
public class PanelEstudiantes extends PanelBase {

    private static final String[] COLUMNAS = {"ID", "Nombre", "RUT", "Curso", "Correo"};

    private final Aplicacion app;

    private final JTextField txtId = new JTextField(5);
    private final JTextField txtNombre = new JTextField(24);
    private final JTextField txtRut = new JTextField(12);
    private final JTextField txtCurso = new JTextField(14);
    private final JTextField txtCorreo = new JTextField(24);
    private final JPasswordField txtClave = new JPasswordField(14);
    private final JTextField txtBuscar = new JTextField(22);

    private final DefaultTableModel modeloTabla = Estilo.modeloSoloLectura(COLUMNAS);
    private final JTable tabla = Estilo.tabla(modeloTabla, 0, 2, 3);
    private final JLabel lblTotal = new JLabel(" ");

    private final JButton btnRegistrar = Estilo.boton("Registrar");
    private final JButton btnActualizar = Estilo.boton("Modificar");
    private final JButton btnEliminar = Estilo.boton("Eliminar");
    private final JButton btnLimpiar = Estilo.boton("Limpiar");

    private int idSeleccionado = 0;
    private boolean recargando = false;

    public PanelEstudiantes(Aplicacion app) {
        this.app = app;
        setLayout(new BorderLayout(10, 10));
        add(Estilo.encabezado("Gestión de estudiantes",
                "Registrar estudiantes como usuarios del sistema, modificarlos y eliminarlos"), BorderLayout.NORTH);

        txtId.setEditable(false);
        txtId.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel campos = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 6, 3, 6);
        Estilo.agregarCampo(campos, c, 0, 0, "ID:", txtId);
        Estilo.agregarCampo(campos, c, 0, 1, "RUT:", txtRut);
        Estilo.agregarCampo(campos, c, 1, 0, "Nombre:", txtNombre);
        Estilo.agregarCampo(campos, c, 1, 1, "Curso:", txtCurso);
        Estilo.agregarCampo(campos, c, 2, 0, "Correo (usuario):", txtCorreo);
        Estilo.agregarCampo(campos, c, 2, 1, "Contraseña inicial:", txtClave);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        botones.add(btnRegistrar);
        botones.add(btnActualizar);
        botones.add(btnEliminar);
        botones.add(btnLimpiar);
        JPanel form = new JPanel(new BorderLayout());
        form.setBorder(BorderFactory.createTitledBorder("Datos del estudiante"));
        form.add(campos, BorderLayout.CENTER);
        form.add(botones, BorderLayout.SOUTH);

        JButton btnBuscar = Estilo.boton("Buscar");
        JButton btnTodos = Estilo.boton("Ver todos");
        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        busqueda.setBorder(BorderFactory.createTitledBorder("Buscar por nombre, RUT, curso o correo"));
        busqueda.add(txtBuscar);
        busqueda.add(btnBuscar);
        busqueda.add(btnTodos);

        JPanel arriba = new JPanel(new BorderLayout(0, 6));
        arriba.add(form, BorderLayout.NORTH);
        arriba.add(busqueda, BorderLayout.SOUTH);
        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.add(arriba, BorderLayout.NORTH);
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);
        add(lblTotal, BorderLayout.SOUTH);

        btnRegistrar.addActionListener(e -> registrar());
        btnActualizar.addActionListener(e -> actualizarEstudiante());
        btnEliminar.addActionListener(e -> eliminar());
        btnLimpiar.addActionListener(e -> limpiar());
        btnBuscar.addActionListener(e -> cargarEstudiantes());
        txtBuscar.addActionListener(e -> cargarEstudiantes());
        btnTodos.addActionListener(e -> {
            txtBuscar.setText("");
            cargarEstudiantes();
        });
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !recargando) {
                cargarFormularioDesdeTabla();
            }
        });
        actualizarModo();
    }

    @Override
    public void actualizar() {
        cargarEstudiantes();
    }

    private void registrar() {
        try {
            Estudiante e = app.estudiantes.registrar(txtNombre.getText(), txtRut.getText(), txtCurso.getText(),
                    txtCorreo.getText(), new String(txtClave.getPassword()));
            Mensajes.exito(this, "Estudiante registrado", e.getNombre() + " quedó registrado y ya puede ingresar con su correo.");
            limpiar();
            cargarEstudiantes();
        } catch (IllegalArgumentException | DAOException ex) {
            Mensajes.error(this, ex);
        }
    }

    private void actualizarEstudiante() {
        try {
            Estudiante e = app.estudiantes.actualizar(idSeleccionado, txtNombre.getText(), txtCurso.getText(), txtCorreo.getText());
            Mensajes.exito(this, "Estudiante modificado", "Los datos de " + e.getNombre() + " fueron actualizados.");
            cargarEstudiantes();
        } catch (IllegalArgumentException | DAOException ex) {
            Mensajes.error(this, ex);
        }
    }

    private void eliminar() {
        if (idSeleccionado <= 0) {
            Mensajes.advertencia(this, "Seleccione un estudiante de la tabla.");
            return;
        }
        if (!Mensajes.confirmar(this, "¿Eliminar al estudiante #" + idSeleccionado + " y su usuario?\nEsta acción no se puede deshacer.")) {
            return;
        }
        try {
            app.estudiantes.eliminar(idSeleccionado);
            Mensajes.exito(this, "Estudiante eliminado", "El estudiante y su usuario fueron eliminados.");
            limpiar();
            cargarEstudiantes();
        } catch (IllegalArgumentException | DAOException ex) {
            Mensajes.error(this, ex);
        }
    }

    private void limpiar() {
        recargando = true;
        tabla.clearSelection();
        recargando = false;
        idSeleccionado = 0;
        txtId.setText("");
        txtNombre.setText("");
        txtRut.setText("");
        txtCurso.setText("");
        txtCorreo.setText("");
        txtClave.setText("");
        actualizarModo();
        txtNombre.requestFocus();
    }

    private void cargarEstudiantes() {
        recargando = true;
        try {
            List<Estudiante> lista = app.estudiantes.buscar(txtBuscar.getText());
            modeloTabla.setRowCount(0);
            for (Estudiante e : lista) {
                modeloTabla.addRow(new Object[]{e.getId(), e.getNombre(), e.getRut(), e.getCurso(), e.getCorreo()});
            }
            lblTotal.setText("Estudiantes mostrados: " + lista.size());
        } catch (DAOException ex) {
            lblTotal.setText("No se pudieron leer los estudiantes de la base de datos.");
            Mensajes.errorBD(this, ex.getMessage());
        } finally {
            recargando = false;
        }
        idSeleccionado = 0;
        actualizarModo();
    }

    private void cargarFormularioDesdeTabla() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            idSeleccionado = 0;
        } else {
            int m = tabla.convertRowIndexToModel(fila);
            idSeleccionado = (int) modeloTabla.getValueAt(m, 0);
            txtId.setText(String.valueOf(idSeleccionado));
            txtNombre.setText((String) modeloTabla.getValueAt(m, 1));
            txtRut.setText((String) modeloTabla.getValueAt(m, 2));
            txtCurso.setText((String) modeloTabla.getValueAt(m, 3));
            txtCorreo.setText((String) modeloTabla.getValueAt(m, 4));
            txtClave.setText("");
        }
        actualizarModo();
    }

    private void actualizarModo() {
        boolean editando = idSeleccionado > 0;
        btnRegistrar.setEnabled(!editando);
        btnActualizar.setEnabled(editando);
        btnEliminar.setEnabled(editando);
        txtRut.setEditable(!editando);       // el RUT vincula al estudiante con su usuario: no se cambia
        txtClave.setEnabled(!editando);      // la contraseña solo se define al registrar
    }
}
