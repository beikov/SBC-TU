package at.ac.tuwien.sbc.ui;

import at.ac.tuwien.sbc.model.Clock;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 * A simple table model for the factory clock GUI that takes snapshots of the {@link ClockList} container on data change events.
 */
public class DeliveredClocksTableModel extends AbstractTableModel {

    private final ClockList clockList;
    private List<Clock> clocks;
    private final String[] columnNames = { "Seriennummer", "Qualität", "Geliefert von" };
    private final Class<?>[] columnTypes = { String.class, Integer.class, String.class };

    public DeliveredClocksTableModel(ClockList clockList) {
        this.clockList = clockList;
        this.clocks = clockList.getDeliveredClocks();
    }

    @Override
    public int getRowCount() {
        return clocks.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnTypes[columnIndex];
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Clock clock = clocks.get(rowIndex);

        switch (columnIndex) {
            case 0:
                return String.valueOf(clock.getSerialId());
            case 1:
                return clock.getQuality();
            case 2:
                return clock.getDelivererId() == null ? "" : clock.getDelivererId()
                    .toString();
        }

        return null;
    }

    @Override
    public void fireTableDataChanged() {
        this.clocks = clockList.getDeliveredClocks();
        super.fireTableDataChanged();
    }

}
