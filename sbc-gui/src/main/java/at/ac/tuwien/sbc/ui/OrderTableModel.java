package at.ac.tuwien.sbc.ui;

import at.ac.tuwien.sbc.model.ClockType;
import at.ac.tuwien.sbc.model.Order;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.swing.table.AbstractTableModel;

/**
 * A simple table model for the factory order GUI that takes snapshots of the {@link OrderList} container on data change events.
 */
public class OrderTableModel extends AbstractTableModel {

    private final OrderList orderList;
    private List<Order> orders;
    private final String[] columnNames = { "ID", "Priorität", "Klassische Uhr", "Sportuhr", "Sportuhr 2 Zeitzonen" };
    private final Class<?>[] columnTypes = { String.class, String.class, String.class, String.class, String.class };

    public OrderTableModel(OrderList orderList) {
        this.orderList = orderList;
        this.orders = orderList.getList();
    }

    @Override
    public int getRowCount() {
        return orders.size();
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
        Order order = orders.get(rowIndex);
        Map<ClockType, Integer[]> neededClocks = order.getNeededClocks();
        switch (columnIndex) {
            case 0:
                return order.getId()
                    .toString();
            case 1:
                return order.getPriority()
                    .toString();
            case 2:
                return (neededClocks.get(ClockType.KLASSISCH)) == null ? 0 : Arrays.toString(neededClocks.get(
                    ClockType.KLASSISCH));
            case 3:
                return (neededClocks.get(ClockType.SPORT)) == null ? 0 : Arrays.toString(neededClocks.get(ClockType.SPORT));
            case 4:
                return (neededClocks.get(ClockType.ZEITZONEN_SPORT)) == null ? 0 : Arrays.toString(neededClocks.get(
                    ClockType.ZEITZONEN_SPORT));
        }
        return null;
    }

    @Override
    public void fireTableDataChanged() {
        this.orders = orderList.getList();
        super.fireTableDataChanged();
    }

}
