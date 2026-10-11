package vista;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Estilo visual común para que todas las pantallas se vean coherentes (márgenes, colores y tipografías) y
 * ayudas para crear tablas y formularios.
 *
 * @author Rodolfo Delgado
 */
public final class Estilo {

    public static final Color PRIMARIO = new Color(0, 102, 204);
    public static final Color FONDO_ENCABEZADO = new Color(235, 243, 252);
    public static final Color ALERTA = new Color(192, 40, 40);
    public static final Color FONDO_ALERTA = new Color(253, 232, 232);
    public static final Font TITULO = new Font("SansSerif", Font.BOLD, 18);
    public static final Font SUBTITULO = new Font("SansSerif", Font.PLAIN, 12);

    private Estilo() {
    }

    public static Border margen(int px) {
        return BorderFactory.createEmptyBorder(px, px, px, px);
    }

    /** Franja superior con título y subtítulo. */
    public static JPanel encabezado(String titulo, String subtitulo) {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.setBackground(FONDO_ENCABEZADO);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, PRIMARIO), margen(10)));
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(TITULO);
        lblTitulo.setForeground(PRIMARIO);
        JLabel lblSub = new JLabel(subtitulo);
        lblSub.setFont(SUBTITULO);
        panel.add(lblTitulo);
        panel.add(lblSub);
        return panel;
    }

    public static JButton boton(String texto) {
        JButton b = new JButton(texto);
        b.setFocusPainted(false);
        return b;
    }

    /** Modelo de tabla con celdas de solo lectura: los datos se cambian desde el formulario, no a mano. */
    public static DefaultTableModel modeloSoloLectura(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }

    /** JTable ya configurado: una sola fila seleccionable, ordenable al hacer clic en la cabecera. */
    public static JTable tabla(DefaultTableModel modelo, int... columnasCentradas) {
        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(22);
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.setAutoCreateRowSorter(true);

        DefaultTableCellRenderer centrado = new DefaultTableCellRenderer();
        centrado.setHorizontalAlignment(SwingConstants.CENTER);
        for (int col : columnasCentradas) {
            tabla.getColumnModel().getColumn(col).setCellRenderer(centrado);
        }
        return tabla;
    }

    /** Pinta en rojo las filas cuyo texto en la columna indicada empieza con "ATRASADO". */
    public static void resaltarAtrasados(JTable tabla, int columnaEstado) {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada,
                                                           boolean foco, int fila, int columna) {
                Component c = super.getTableCellRendererComponent(t, valor, seleccionada, foco, fila, columna);
                Object estado = t.getValueAt(fila, columnaEstado);
                boolean atrasado = estado != null && estado.toString().startsWith("ATRASADO");
                if (!seleccionada) {
                    c.setBackground(atrasado ? FONDO_ALERTA : t.getBackground());
                    c.setForeground(atrasado ? ALERTA : t.getForeground());
                }
                return c;
            }
        };
        for (int i = 0; i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    /** Agrega una fila "etiqueta + campo" a un formulario con GridBagLayout. */
    public static void agregarFila(JPanel panel, GridBagConstraints c, int fila, String etiqueta, JComponent campo) {
        c.gridy = fila;
        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(etiqueta), c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.WEST;
        panel.add(campo, c);
    }

    /** Como agregarFila, pero permite poner dos campos por fila (columna 0 o 1) para que el formulario sea más bajo. */
    public static void agregarCampo(JPanel panel, GridBagConstraints c, int fila, int columna, String etiqueta,
                                    JComponent campo) {
        c.gridy = fila;
        c.gridx = columna * 2;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(etiqueta), c);
        c.gridx = columna * 2 + 1;
        c.anchor = GridBagConstraints.WEST;
        panel.add(campo, c);
    }
}
