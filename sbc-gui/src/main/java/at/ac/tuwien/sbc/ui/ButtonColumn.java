package at.ac.tuwien.sbc.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.AbstractCellEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

/**
 * The ButtonColumn class provides a renderer and an editor that looks like a
 * JButton. The renderer and editor will then be used for a specified column
 * in the table. The TableModel will contain the String to be displayed on
 * the button.
 *
 * The button can be invoked by a mouse click or by pressing the space bar
 * when the cell has focus. Optionally a mnemonic can be set to invoke the
 * button. When the button is invoked the provided Action is invoked. The
 * source of the Action will be the table. The action command will contain
 * the model row number of the button that was clicked.
 *
 */
public class ButtonColumn extends AbstractCellEditor
    implements TableCellRenderer, TableCellEditor, MouseListener {

    private final JTable table;
    private final Listener action;
    private final Border originalBorder;
    private final Border focusBorder;

    private final JButton renderButton;
    private final JButton editButton;
    private Object editorValue;
    private boolean isButtonColumnEditor;

    public static interface Listener {

        public void buttonClicked(JButton editButton, JButton renderButton, int row);
    }

    private final ActionListener buttonClickListener = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            int row = table.convertRowIndexToModel(table.getEditingRow());
            fireEditingStopped();

            action.buttonClicked(editButton, renderButton, row);
        }
    };

    /**
     * Create the ButtonColumn to be used as a renderer and editor. The
     * renderer and editor will automatically be installed on the TableColumn
     * of the specified column.
     *
     * @param table  the table containing the button renderer/editor
     * @param action the Action to be invoked when the button is invoked
     * @param column the column to which the button renderer/editor is added
     */
    public ButtonColumn(JTable table, Listener action, int column) {
        this.table = table;
        this.action = action;
        this.renderButton = new JButton();
        this.editButton = new JButton();
        this.editButton.setFocusPainted(false);
        this.editButton.addActionListener(buttonClickListener);
        this.originalBorder = editButton.getBorder();
        this.focusBorder = new LineBorder(Color.BLUE);
        this.editButton.setBorder(focusBorder);
    }

    public static void register(JTable table, int column, Listener action) {
        ButtonColumn buttonColumn = new ButtonColumn(table, action, column);

        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(column)
            .setCellRenderer(buttonColumn);
        columnModel.getColumn(column)
            .setCellEditor(buttonColumn);
        table.addMouseListener(buttonColumn);
    }

    @Override
    public Component getTableCellEditorComponent(
        JTable table, Object value, boolean isSelected, int row, int column) {
        if (value == null) {
            editButton.setText("");
            editButton.setIcon(null);
        } else if (value instanceof Icon) {
            editButton.setText("");
            editButton.setIcon((Icon) value);
        } else {
            editButton.setText(value.toString());
            editButton.setIcon(null);

            if ("Start".equals(value.toString())) {
                editButton.setEnabled(true);
            } else {
                editButton.setEnabled(false);
            }
        }

        this.editorValue = value;
        return editButton;
    }

    @Override
    public Object getCellEditorValue() {
        return editorValue;
    }

//
//  Implement TableCellRenderer interface
//
    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (isSelected) {
            renderButton.setForeground(table.getSelectionForeground());
            renderButton.setBackground(table.getSelectionBackground());
        } else {
            renderButton.setForeground(table.getForeground());
            renderButton.setBackground(UIManager.getColor("Button.background"));
        }

        if (hasFocus) {
            renderButton.setBorder(focusBorder);
        } else {
            renderButton.setBorder(originalBorder);
        }

//		renderButton.setText( (value == null) ? "" : value.toString() );
        if (value == null) {
            renderButton.setText("");
            renderButton.setIcon(null);
        } else if (value instanceof Icon) {
            renderButton.setText("");
            renderButton.setIcon((Icon) value);
        } else {
            renderButton.setText(value.toString());
            renderButton.setIcon(null);

            if ("Start".equals(value.toString())) {
                renderButton.setEnabled(true);
            } else {
                renderButton.setEnabled(false);
            }
        }

        return renderButton;
    }

//
//  Implement MouseListener interface
//
	/*
     * When the mouse is pressed the editor is invoked. If you then then drag
     * the mouse to another cell before releasing it, the editor is still
     * active. Make sure editing is stopped when the mouse is released.
     */
    @Override
    public void mousePressed(MouseEvent e) {
        if (table.isEditing()
            && table.getCellEditor() == this) {
            isButtonColumnEditor = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (isButtonColumnEditor
            && table.isEditing()) {
            table.getCellEditor()
                .stopCellEditing();
        }

        isButtonColumnEditor = false;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}
