package vista;

import dao.DAOException;
import main.Aplicacion;
import modelo.Categoria;
import modelo.Libro;
import modelo.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * VISTA: gestión de libros. El bibliotecario puede registrar, modificar y eliminar; el estudiante solo ve y busca
 * en el catálogo (el formulario se oculta según {@code Usuario.puedeGestionarCatalogo()}).
 *
 * @author Rodolfo Delgado
 */
public class PanelLibros extends PanelBase {

    private static final String[] COLUMNAS = {"ID", "Título", "Autor", "ISBN", "Editorial", "Categoría", "Stock"};

    private final Aplicacion app;
    private final boolean puedeGestionar;

    private final JTextField txtId = new JTextField(5);
    private final JTextField txtTitulo = new JTextField(24);
    private final JTextField txtAutor = new JTextField(20);
    private final JTextField txtIsbn = new JTextField(14);
    private final JTextField txtEditorial = new JTextField(16);
    private final JTextField txtStock = new JTextField(5);
    private final JComboBox<Categoria> cmbCategoria = new JComboBox<>();
    private final JTextField txtBuscar = new JTextField(22);

    private final DefaultTableModel modeloTabla = Estilo.modeloSoloLectura(COLUMNAS);
    private final JTable tabla = Estilo.tabla(modeloTabla, 0, 3, 6);
    private final JLabel lblTotal = new JLabel(" ");

    private final JButton btnRegistrar = Estilo.boton("Registrar");
    private final JButton btnActualizar = Estilo.boton("Modificar");
    private final JButton btnEliminar = Estilo.boton("Eliminar");
    private final JButton btnLimpiar = Estilo.boton("Limpiar");

    private int idSeleccionado = 0;
    private boolean recargando = false;

    public PanelLibros(Aplicacion app, Usuario usuario) {
        this.app = app;
        this.puedeGestionar = usuario.puedeGestionarCatalogo();
        crearComponentes();
    }

    private void crearComponentes() {
        setLayout(new BorderLayout(10, 10));
        add(Estilo.encabezado(puedeGestionar ? "Gestión de libros" : "Catálogo de libros",
                puedeGestionar ? "Registrar, modificar, eliminar y visualizar el inventario"
                        : "Consulte los libros disponibles"), BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 8));

        JPanel arriba = new JPanel(new BorderLayout(0, 6));
        if (puedeGestionar) {
            txtId.setEditable(false);
            txtId.setHorizontalAlignment(SwingConstants.CENTER);
            JPanel campos = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(3, 6, 3, 6);
            Estilo.agregarCampo(campos, c, 0, 0, "ID:", txtId);
            Estilo.agregarCampo(campos, c, 0, 1, "ISBN:", txtIsbn);
            Estilo.agregarCampo(campos, c, 1, 0, "Título:", txtTitulo);
            Estilo.agregarCampo(campos, c, 1, 1, "Editorial:", txtEditorial);
            Estilo.agregarCampo(campos, c, 2, 0, "Autor:", txtAutor);
            Estilo.agregarCampo(campos, c, 2, 1, "Categoría:", cmbCategoria);
            Estilo.agregarCampo(campos, c, 3, 0, "Stock:", txtStock);

            JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            botones.add(btnRegistrar);
            botones.add(btnActualizar);
            botones.add(btnEliminar);
            botones.add(btnLimpiar);

            JPanel form = new JPanel(new BorderLayout());
            form.setBorder(BorderFactory.createTitledBorder("Datos del libro"));
            form.add(campos, BorderLayout.CENTER);
            form.add(botones, BorderLayout.SOUTH);
            arriba.add(form, BorderLayout.NORTH);
        }

        JButton btnBuscar = Estilo.boton("Buscar");
        JButton btnTodos = Estilo.boton("Ver todos");
        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        busqueda.setBorder(BorderFactory.createTitledBorder("Buscar por título, autor, ISBN o editorial"));
        busqueda.add(txtBuscar);
        busqueda.add(btnBuscar);
        busqueda.add(btnTodos);
        arriba.add(busqueda, BorderLayout.SOUTH);

        tabla.getColumnModel().getColumn(0).setPreferredWidth(40);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(240);
        centro.add(arriba, BorderLayout.NORTH);
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);
        add(lblTotal, BorderLayout.SOUTH);

        btnBuscar.addActionListener(e -> cargarLibros());
        txtBuscar.addActionListener(e -> cargarLibros());
        btnTodos.addActionListener(e -> {
            txtBuscar.setText("");
            cargarLibros();
        });
        if (puedeGestionar) {
            btnRegistrar.addActionListener(e -> registrar());
            btnActualizar.addActionListener(e -> actualizarLibro());
            btnEliminar.addActionListener(e -> eliminar());
            btnLimpiar.addActionListener(e -> limpiar());
            tabla.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && !recargando) {
                    cargarFormularioDesdeTabla();
                }
            });
            actualizarModo();
        }
    }

    @Override
    public void actualizar() {
        if (puedeGestionar) {
            cargarCategorias();
        }
        cargarLibros();
    }

    private void cargarCategorias() {
        try {
            Categoria previa = (Categoria) cmbCategoria.getSelectedItem();
            cmbCategoria.removeAllItems();
            for (Categoria c : app.categorias.listar()) {
                cmbCategoria.addItem(c);
                if (previa != null && previa.getId() == c.getId()) {
                    cmbCategoria.setSelectedItem(c);
                }
            }
        } catch (DAOException ex) {
            Mensajes.errorBD(this, ex.getMessage());
        }
    }

    // ------------------------------------------------------------ Acciones

    private void registrar() {
        try {
            Libro l = app.libros.crear(txtTitulo.getText(), txtAutor.getText(), txtIsbn.getText(),
                    txtEditorial.getText(), txtStock.getText(), (Categoria) cmbCategoria.getSelectedItem());
            Mensajes.exito(this, "Libro registrado", "\"" + l.getTitulo() + "\" quedó guardado (ID " + l.getId() + ").");
            limpiar();
            cargarLibros();
        } catch (IllegalArgumentException | DAOException ex) {
            Mensajes.error(this, ex);
        }
    }

    private void actualizarLibro() {
        try {
            Libro l = app.libros.actualizar(idSeleccionado, txtTitulo.getText(), txtAutor.getText(), txtIsbn.getText(),
                    txtEditorial.getText(), txtStock.getText(), (Categoria) cmbCategoria.getSelectedItem());
            Mensajes.exito(this, "Libro modificado", "\"" + l.getTitulo() + "\" fue actualizado.");
            cargarLibros();
        } catch (IllegalArgumentException | DAOException ex) {
            Mensajes.error(this, ex);
        }
    }

    private void eliminar() {
        if (idSeleccionado <= 0) {
            Mensajes.advertencia(this, "Seleccione un libro de la tabla.");
            return;
        }
        if (!Mensajes.confirmar(this, "¿Eliminar el libro #" + idSeleccionado + "?\nEsta acción no se puede deshacer.")) {
            return;
        }
        try {
            app.libros.eliminar(idSeleccionado);
            Mensajes.exito(this, "Libro eliminado", "El libro fue eliminado del inventario.");
            limpiar();
            cargarLibros();
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
        txtTitulo.setText("");
        txtAutor.setText("");
        txtIsbn.setText("");
        txtEditorial.setText("");
        txtStock.setText("");
        if (cmbCategoria.getItemCount() > 0) {
            cmbCategoria.setSelectedIndex(0);
        }
        actualizarModo();
        txtTitulo.requestFocus();
    }

    // ---------------------------------------------------------- Tabla <-> formulario

    private void cargarLibros() {
        recargando = true;
        try {
            List<Libro> libros = app.libros.buscar(txtBuscar.getText());
            modeloTabla.setRowCount(0);
            for (Libro l : libros) {
                modeloTabla.addRow(new Object[]{l.getId(), l.getTitulo(), l.getAutor(), l.getIsbn(), l.getEditorial(),
                        l.getCategoria() == null ? "-" : l.getCategoria().getNombre(), l.getStock()});
            }
            lblTotal.setText("Libros mostrados: " + libros.size());
        } catch (DAOException ex) {
            lblTotal.setText("No se pudieron leer los libros de la base de datos.");
            Mensajes.errorBD(this, ex.getMessage());
        } finally {
            recargando = false;
        }
        idSeleccionado = 0;
        if (puedeGestionar) {
            actualizarModo();
        }
    }

    private void cargarFormularioDesdeTabla() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            idSeleccionado = 0;
        } else {
            int m = tabla.convertRowIndexToModel(fila);
            idSeleccionado = (int) modeloTabla.getValueAt(m, 0);
            txtId.setText(String.valueOf(idSeleccionado));
            txtTitulo.setText((String) modeloTabla.getValueAt(m, 1));
            txtAutor.setText((String) modeloTabla.getValueAt(m, 2));
            txtIsbn.setText((String) modeloTabla.getValueAt(m, 3));
            txtEditorial.setText((String) modeloTabla.getValueAt(m, 4));
            txtStock.setText(String.valueOf(modeloTabla.getValueAt(m, 6)));
            Object nombreCategoria = modeloTabla.getValueAt(m, 5);
            for (int i = 0; i < cmbCategoria.getItemCount(); i++) {
                if (cmbCategoria.getItemAt(i).getNombre().equals(nombreCategoria)) {
                    cmbCategoria.setSelectedIndex(i);
                    break;
                }
            }
        }
        actualizarModo();
    }

    private void actualizarModo() {
        boolean editando = idSeleccionado > 0;
        btnRegistrar.setEnabled(!editando);
        btnActualizar.setEnabled(editando);
        btnEliminar.setEnabled(editando);
    }
}
