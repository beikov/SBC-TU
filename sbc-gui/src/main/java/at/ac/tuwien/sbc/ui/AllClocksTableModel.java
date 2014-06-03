/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.ui;

import at.ac.tuwien.sbc.model.Clock;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author Christian
 */
public class AllClocksTableModel extends AbstractTableModel {

    private final ClockList clockList;
    private List<Clock> clocks;
    private final String[] columnNames = { "Seriennummer","Uhrtyp", "Genauigkeit", "Gehäuse", "Uhrwerk", "Zeiger 1", "Zeiger 2",
        "Armband", "Gehäuse-Lieferant", "Uhrwerk-Lieferant", "Zeiger 1-Lieferant", "Zeiger 2-Lieferant", "Armband-Lieferant", "Montage", "Qualität", "Logistik" };
    private final Class<?>[] columnTypes = { String.class, String.class, Integer.class, String.class, String.class, String.class, String.class,
        String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class };

    public AllClocksTableModel(ClockList clockList) {
        this.clockList = clockList;
        this.clocks = clockList.getList();
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
            case 0: return clock.getSerialId();
            case 1: return clock.getType().toString();
            case 2: return clock.getQuality();
            case 3: return clock.getChassis().getId().toString();
            case 4: return clock.getClockWork().getId().toString();
            case 5: return clock.getClockHand1().getId().toString();
            case 6: return clock.getClockHand2().getId().toString();
            case 7: return clock.getWristband().getId().toString();
            case 8: return clock.getChassis().getSupplierId().toString();
            case 9: return clock.getClockWork().getSupplierId().toString();
            case 10: return clock.getClockHand1().getSupplierId().toString();
            case 11: return clock.getClockHand2().getSupplierId().toString();
            case 12: return clock.getWristband().getSupplierId().toString();
            case 13: return clock.getAssemblerId().toString();
            case 14: return clock.getQualityCheckerId() == null ? "" : clock.getQualityCheckerId().toString();
            case 15: return clock.getDelivererId() == null ? "" : clock.getDelivererId().toString();
        }
        
        return null;
    }

    @Override
    public void fireTableDataChanged() {
        this.clocks = clockList.getList();
        super.fireTableDataChanged();
    }

}
