package at.ac.tuwien.sbc.ui;

import at.ac.tuwien.sbc.actor.SupplierActor;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 * A simple table model for the factory supplier GUI contains the suppliers created in the GUI.
 */
public class SupplierTableModel extends AbstractTableModel {

    private final List<SupplierActor> suppliers = new ArrayList<SupplierActor>();
    private final List<String> buttonTexts = new ArrayList<String>();

    public void addSupplier(final SupplierActor supplier) {
        suppliers.add(supplier);
        buttonTexts.add("Start");
        fireTableRowsInserted(suppliers.size() - 1, suppliers.size() - 1);
    }

    @Override
    public int getRowCount() {
        return suppliers.size();
    }

    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 3;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "Id";
            case 1:
                return "Bestandteil";
            case 2:
                return "Anzahl";
            case 3:
                return "Aktion";
        }

        return null;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return String.class;
            case 1:
                return String.class;
            case 2:
                return Integer.class;
            case 3:
                return String.class;
        }

        return Object.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        final SupplierActor actor = suppliers.get(rowIndex);

        switch (columnIndex) {
            case 0:
                return actor.getId()
                    .toString();
            case 1:
                return actor.getPartType()
                    .toString();
            case 2:
                return actor.getAmount();
            case 3:
                return buttonTexts.get(rowIndex);
        }

        return null;
    }

    public SupplierActor get(int row) {
        return suppliers.get(row);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == 3) {
            buttonTexts.set(rowIndex, aValue.toString());
        }
    }

}
