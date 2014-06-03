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
public class DisassembledClocksTableModel extends AbstractTableModel {

    private final ClockList clockList;
    private List<Clock> clocks;
    private final String[] columnNames = { "Seriennummer", "Zerlegt von"};
    private final Class<?>[] columnTypes = { String.class, String.class };

    public DisassembledClocksTableModel(ClockList clockList) {
        this.clockList = clockList;
        this.clocks = clockList.getAssembledClocks();
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
            case 0: return String.valueOf(clock.getSerialId());
            case 1: return clock.getDelivererId().toString();
        }
        
        return null;
    }

    @Override
    public void fireTableDataChanged() {
        this.clocks = clockList.getDisassembledClocks();
        super.fireTableDataChanged();
    }

}
