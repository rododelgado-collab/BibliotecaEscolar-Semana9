package vista;

import dao.DAOException;
import main.Aplicacion;
import modelo.Categoria;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * VISTA: gestión de las categorías de los libros (solo bibliotecario). CRUD completo.
 *
 * @author Rodolfo Delgado
 */
public class PanelCategorias extends PanelBase {

    private final Aplicacion app;

    private final JTextField txtNombre = new JTextField(24);
    private final DefaultTableModel modeloTabla = Estilo.modeloSoloLectura("ID", "Nombre");
    private final JTable tabla = Estilo.tabla(modeloTabla, 0);
    private final JButton btnRegistrar = Estilo.boton("Registrar");
    private final JButton btnActualizar = Estilo.boton("Modificar");
    private final JButton btnEliminar = Estilo.boton("Eliminar");
    private final JButton btnLimpiar = Estilo.boton("Limpiar");

    private int idSeleccionado = 0;
    private boolean recargando = false;

    public PanelCategorias(Aplicacion app) {
        this.app = app;
        setLayout(new BorderLayout(10, 10));
        add(Estilo.encabezado("Categorías", "Agrupan los libros del inventario"), BorderLayout.NORTH);

        JPanel campos = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        Estilo.agregarFila(campos, c, 0, "Nombre:", txtNombre);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        botones.add(btnRegistrar);
        botones.add(btnActualizar);
        botones.add(btnEliminar);
        botones.add(btnLimpiar);
        JPanel form = new JPanel(new BorderLayout());
        form.setBorder(BorderFactory.createTitledBorder("Datos de la categoría"));
        form.add(campos, BorderLayout.CENTER);
        form.add(botones, BorderLayout.SOUTH);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.add(form, BorderLayout.NORTH);
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        btnRegistrar.addActionListener(e -> ejecutar(() -> app.categorias.crear(txtNombre.getText()), "Categoría registrada"));
        btnActualizar.addActionListener(e -> ejecutar(() -> app.categorias.actualizar(idSeleccionado, txtNombre.getText()),
                "Categoría modificada"));
        btnEliminar.addActionListener(e -> {
            if (idSeleccionado <= 0) {
                Mensajes.advertencia(this, "Seleccione una categoría de la tabla.");
            } else if (Mensajes.confirmar(this, "¿Eliminar la categoría #" + idSeleccionado + "?")) {
                ejecutar(() -> {
                    app.categorias.eliminar(idSeleccionado);
                    return null;
                }, "Categoría eliminada");
            }
        });
        btnLimpiar.addActionListener(e -> limpiar());
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !recargando) {
                int fila = tabla.getSelectedRow();
                if (fila >= 0) {
                    int m = tabla.convertRowIndexToModel(fila);
                    idSeleccionado = (int) modeloTabla.getValueAt(m, 0);
                    txtNombre.setText((String) modeloTabla.getValueAt(m, 1));
                } else {
                    idSeleccionado = 0;
                }
                actualizarModo();
            }
        });
        actualizarModo();
    }

    private void ejecutar(java.util.function.Supplier<Object> accion, String titulo) {
        try {
            accion.get();
            Mensajes.exito(this, titulo, "La operación se realizó correctamente.");
            limpiar();
            actualizar();
        } catch (IllegalArgumentException | DAOException ex) {
            Mensajes.error(this, ex);
        }
    }

    private void limpiar() {
        recargando = true;
        tabla.clearSelection();
        recargando = false;
        idSeleccionado = 0;
        txtNombre.setText("");
        actualizarModo();
        txtNombre.requestFocus();
    }

    private void actualizarModo() {
        boolean editando = idSeleccionado > 0;
        btnRegistrar.setEnabled(!editando);
        btnActualizar.setEnabled(editando);
        btnEliminar.setEnabled(editando);
    }

    @Override
    public void actualizar() {
        recargando = true;
        try {
            modeloTabla.setRowCount(0);
            for (Categoria cat : app.categorias.listar()) {
                modeloTabla.addRow(new Object[]{cat.getId(), cat.getNombre()});
            }
        } catch (DAOException ex) {
            Mensajes.errorBD(this, ex.getMessage());
        } finally {
            recargando = false;
        }
    }
}
