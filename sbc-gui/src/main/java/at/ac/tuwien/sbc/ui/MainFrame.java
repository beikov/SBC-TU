/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.ui;

import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.actor.SupplierActor;
import at.ac.tuwien.sbc.model.ClockPartType;
import at.ac.tuwien.sbc.model.ClockType;
import at.ac.tuwien.sbc.model.Order;
import at.ac.tuwien.sbc.model.OrderPriority;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import javax.swing.JButton;

/**
 *
 * @author Christian
 */
public class MainFrame extends javax.swing.JFrame {

    private final Connector connector;
    private final ExecutorService threadPool;

    private final ClockPartCounter counter;
    private final ClockList clockList;
    private final OrderList orderList;

    private final SupplierTableModel supplierTableModel;
    private final AssembledClocksTableModel assembledTableModel;
    private final CheckedClocksTableModel checkedTableModel;
    private final DeliveredClocksTableModel deliveredTableModel;
    private final DisassembledClocksTableModel disassembledTableModel;
    private final AllClocksTableModel clockTableModel;
    private final OrderTableModel orderTableModel;

    /**
     * Creates new form App
     *
     * @param connector
     * @param threadPool
     */
    public MainFrame(Connector connector, final ExecutorService threadPool) {
        this.orderList = new OrderList();

        this.connector = connector;
        this.threadPool = threadPool;
        this.counter = new ClockPartCounter();
        this.clockList = new ClockList();
        this.supplierTableModel = new SupplierTableModel();
        this.clockTableModel = new AllClocksTableModel(clockList);
        this.assembledTableModel = new AssembledClocksTableModel(clockList);
        this.checkedTableModel = new CheckedClocksTableModel(clockList);
        this.deliveredTableModel = new DeliveredClocksTableModel(clockList);
        this.disassembledTableModel = new DisassembledClocksTableModel(clockList);
        this.orderTableModel = new OrderTableModel(orderList);
        initComponents();

        // Add cell renderer for supplier table
        new ButtonColumn(supplierTable, new ButtonColumn.Listener() {

            @Override
            public void buttonClicked(final JButton editButton, final JButton renderButton, final int row) {
                supplierTableModel.setValueAt("Running...", row, 3);
                // We need a repaint because the cell renderer will change appearance
                supplierTable.repaint();

                threadPool.submit(new ActorRunner(supplierTableModel.get(row), new Runnable() {

                    @Override
                    public void run() {
                        supplierTableModel.setValueAt("Start", row, 3);
                        // We need a repaint because the cell renderer will change appearance
                        supplierTable.repaint();
                    }
                }));
            }
        }, 3);

        CountingClockPartListener clockPartListener = new CountingClockPartListener(counter, new Runnable() {

            @Override
            public void run() {
                java.awt.EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        gehaeuseValue.setText(counter.getChassisCounter()
                            .toString());
                        uhrwerkeValue.setText(counter.getClockWorkCounter()
                            .toString());
                        zeigerValue.setText(counter.getClockHandCounter()
                            .toString());
                        lederArmbaenderValue.setText(counter.getLeatherWristbandCounter()
                            .toString());
                        metallArmbaenderValue.setText(counter.getMetalWristbandCounter()
                            .toString());
                    }
                });
            }
        });
        // Subscribe before retrieving data or else we might miss notifications
        connector.subscribeForClockParts(clockPartListener);
        clockPartListener.setCurrentClockParts(connector.getClockParts());

        final CollectingClockListener clockListener = new CollectingClockListener(clockList, new Runnable() {

            @Override
            public void run() {
//                clockTableModel.fireTableDataChanged();
                java.awt.EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        assembledValue.setText(String.valueOf(
                            clockList.getAssembledClocks()
                            .size()));
                        checkedValue.setText(String.valueOf(
                            clockList.getCheckedClocks()
                            .size()));
                        deliveredValue.setText(String.valueOf(
                            clockList.getDeliveredClocks()
                            .size()));
                        disassembledValue.setText(String.valueOf(
                            clockList.getDisassembledClocks()
                            .size()));
                        clockTableModel.fireTableDataChanged();
                        assembledTableModel.fireTableDataChanged();
                        checkedTableModel.fireTableDataChanged();
                        deliveredTableModel.fireTableDataChanged();
                        disassembledTableModel.fireTableDataChanged();
                    }
                });
            }
        });

        // Subscribe before retrieving data or else we might miss notifications
        connector.subscribeForClocks(clockListener);
        clockListener.onClocksUpdated(connector.getClocks());

        final CollectingOrderListener orderListener = new CollectingOrderListener(orderList, new Runnable() {
            @Override
            public void run() {
                java.awt.EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        orderTableModel.fireTableDataChanged();
                    }
                });
            }
        });
        connector.subscribeForOrders(orderListener);
        orderListener.onOrderAdded(connector.getOrders());

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        statusPanel1 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        gehaeuseValue = new javax.swing.JLabel();
        uhrwerkeValue = new javax.swing.JLabel();
        zeigerValue = new javax.swing.JLabel();
        lederArmbaenderValue = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        metallArmbaenderValue = new javax.swing.JLabel();
        jTabbedPane2 = new javax.swing.JTabbedPane();
        jScrollPane3 = new javax.swing.JScrollPane();
        clockTable1 = new javax.swing.JTable();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        clockTable = new javax.swing.JTable();
        jLabel8 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        clockTable2 = new javax.swing.JTable();
        jLabel9 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        clockTable3 = new javax.swing.JTable();
        jLabel10 = new javax.swing.JLabel();
        jScrollPane6 = new javax.swing.JScrollPane();
        clockTable4 = new javax.swing.JTable();
        jLabel11 = new javax.swing.JLabel();
        assembledValue = new javax.swing.JLabel();
        checkedValue = new javax.swing.JLabel();
        disassembledValue = new javax.swing.JLabel();
        deliveredValue = new javax.swing.JLabel();
        supplierPanel1 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jComboBox1 = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        supplierTable = new javax.swing.JTable();
        orderPanel1 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jLabel14 = new javax.swing.JLabel();
        classicAmountTextField = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        sportAmountTextField = new javax.swing.JTextField();
        timeZoneSportAmountTextField = new javax.swing.JTextField();
        priorityComboBox = new javax.swing.JComboBox();
        jLabel16 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jScrollPane7 = new javax.swing.JScrollPane();
        orderTable1 = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.LINE_AXIS));

        statusPanel1.setLayout(new javax.swing.BoxLayout(statusPanel1, javax.swing.BoxLayout.Y_AXIS));

        jPanel3.setMaximumSize(new java.awt.Dimension(1000, 116));

        jLabel1.setText("Bestandteile");

        jLabel4.setText("Gehäuse");

        jLabel5.setText("Uhrwerke");

        jLabel6.setText("Zeiger");

        jLabel7.setText("Lederarmbänder");

        gehaeuseValue.setText("0");

        uhrwerkeValue.setText("0");

        zeigerValue.setText("0");

        lederArmbaenderValue.setText("0");

        jLabel12.setText("Metallarmbänder");

        metallArmbaenderValue.setText("0");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(822, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel7)
                            .addComponent(jLabel12))
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGap(17, 17, 17)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(uhrwerkeValue, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(gehaeuseValue, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(zeigerValue, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lederArmbaenderValue, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(metallArmbaenderValue, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(804, 804, 804))))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addGap(17, 17, 17)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(gehaeuseValue))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(uhrwerkeValue))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(zeigerValue))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(lederArmbaenderValue))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 8, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(metallArmbaenderValue)))
        );

        clockTable1.setModel(clockTableModel);
        jScrollPane3.setViewportView(clockTable1);

        jTabbedPane2.addTab("Alle Uhren", jScrollPane3);

        clockTable.setModel(assembledTableModel);
        jScrollPane2.setViewportView(clockTable);

        jLabel8.setText("Zusammengebaute Uhren:");

        clockTable2.setModel(checkedTableModel);
        jScrollPane4.setViewportView(clockTable2);

        jLabel9.setText("Überprüfte Uhren:");

        clockTable3.setModel(deliveredTableModel);
        jScrollPane5.setViewportView(clockTable3);

        jLabel10.setText("Gelieferte Uhren:");

        clockTable4.setModel(disassembledTableModel);
        jScrollPane6.setViewportView(clockTable4);

        jLabel11.setText("Zerlegte Uhren:");

        assembledValue.setText("0");

        checkedValue.setText("0");

        disassembledValue.setText("0");

        deliveredValue.setText("0");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 635, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(jLabel8)
                                .addGap(18, 18, 18)
                                .addComponent(assembledValue)))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(jLabel9)
                                .addGap(18, 18, 18)
                                .addComponent(checkedValue))
                            .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 635, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 635, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(jLabel10)
                                .addGap(18, 18, 18)
                                .addComponent(deliveredValue)))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(jLabel11)
                                .addGap(18, 18, 18)
                                .addComponent(disassembledValue))
                            .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 635, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(326, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel9)
                            .addComponent(checkedValue))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 227, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel8)
                            .addComponent(assembledValue))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 227, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(47, 47, 47)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(jLabel11)
                    .addComponent(disassembledValue)
                    .addComponent(deliveredValue))
                .addGap(18, 18, 18)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 227, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 227, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(105, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Uhren nach Status", jPanel5);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPane2)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jTabbedPane2))
        );

        statusPanel1.add(jPanel4);

        jTabbedPane1.addTab("Status", statusPanel1);

        supplierPanel1.setLayout(new javax.swing.BoxLayout(supplierPanel1, javax.swing.BoxLayout.Y_AXIS));

        jPanel1.setMaximumSize(new java.awt.Dimension(1671, 120));
        jPanel1.setPreferredSize(new java.awt.Dimension(1671, 100));

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Gehäuse", "Uhrwerk", "Zeiger", "Lederarmband", "Metallarmband" }));
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        jLabel2.setText("Anzahl");

        jTextField1.setText("0");

        jButton1.setText("Anlegen");
        jButton1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jButton1MouseReleased(evt);
            }
        });

        jLabel3.setText("Bestandteil");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, 115, Short.MAX_VALUE)
                            .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jComboBox1, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jTextField1, javax.swing.GroupLayout.DEFAULT_SIZE, 137, Short.MAX_VALUE)))
                    .addComponent(jButton1))
                .addContainerGap(1399, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        supplierPanel1.add(jPanel1);

        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.LINE_AXIS));

        supplierTable.setModel(supplierTableModel);
        jScrollPane1.setViewportView(supplierTable);

        jPanel2.add(jScrollPane1);

        supplierPanel1.add(jPanel2);

        jTabbedPane1.addTab("Lieferanten", supplierPanel1);

        orderPanel1.setLayout(new javax.swing.BoxLayout(orderPanel1, javax.swing.BoxLayout.Y_AXIS));

        jPanel6.setMaximumSize(new java.awt.Dimension(1671, 120));
        jPanel6.setPreferredSize(new java.awt.Dimension(1671, 100));

        jLabel13.setText("Sportuhren:");

        jButton2.setText("anlegen");
        jButton2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jButton2MouseReleased(evt);
            }
        });

        jLabel14.setText("Klassische Uhren:");

        classicAmountTextField.setText("0");
        classicAmountTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                classicAmountTextFieldActionPerformed(evt);
            }
        });

        jLabel15.setText("Sportuhren mit zweiter Zeitzone:");

        sportAmountTextField.setText("0");
        sportAmountTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sportAmountTextFieldActionPerformed(evt);
            }
        });

        timeZoneSportAmountTextField.setText("0");
        timeZoneSportAmountTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                timeZoneSportAmountTextFieldActionPerformed(evt);
            }
        });

        priorityComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "niedrig", "mittel", "hoch" }));

        jLabel16.setText("Priorität:");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel16)
                        .addGap(18, 18, 18)
                        .addComponent(priorityComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jButton2))
                .addGap(34, 34, 34)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel13, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel15, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 168, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sportAmountTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(classicAmountTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(timeZoneSportAmountTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(1263, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(classicAmountTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel14)
                        .addComponent(priorityComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel16)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(sportAmountTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel15)
                            .addComponent(timeZoneSportAmountTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGap(17, 17, 17)
                        .addComponent(jButton2)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        orderPanel1.add(jPanel6);

        jPanel7.setLayout(new javax.swing.BoxLayout(jPanel7, javax.swing.BoxLayout.LINE_AXIS));

        orderTable1.setModel(orderTableModel);
        jScrollPane7.setViewportView(orderTable1);

        jPanel7.add(jScrollPane7);

        orderPanel1.add(jPanel7);

        jTabbedPane1.addTab("Aufträge", orderPanel1);

        getContentPane().add(jTabbedPane1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton1MouseReleased
        ClockPartType type = null;
        switch (jComboBox1.getSelectedIndex()) {
            case 0:
                type = ClockPartType.GEHAEUSE;
                break;
            case 1:
                type = ClockPartType.UHRWERK;
                break;
            case 2:
                type = ClockPartType.ZEIGER;
                break;
            case 3:
                type = ClockPartType.LEDERARMBAND;
                break;
            case 4:
                type = ClockPartType.METALLARMBAND;
                break;
        }
        int amount;

        try {
            amount = Integer.parseInt(jTextField1.getText());
        } catch (NumberFormatException e) {
            return;
        }

        if (amount > 0) {
            supplierTableModel.addSupplier(new SupplierActor(connector, type, amount));
        }
    }//GEN-LAST:event_jButton1MouseReleased

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBox1ActionPerformed

    private void timeZoneSportAmountTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_timeZoneSportAmountTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_timeZoneSportAmountTextFieldActionPerformed

    private void sportAmountTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sportAmountTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_sportAmountTextFieldActionPerformed

    private void classicAmountTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_classicAmountTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_classicAmountTextFieldActionPerformed

    private void jButton2MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton2MouseReleased
        Map<ClockType, Integer[]> neededClocks = new HashMap<ClockType, Integer[]>();
        neededClocks.put(ClockType.KLASSISCH, new Integer[]{ Integer.parseInt(classicAmountTextField.getText()), 0 });
        neededClocks.put(ClockType.SPORT, new Integer[]{ Integer.parseInt(sportAmountTextField.getText()), 0 });
        neededClocks
            .put(ClockType.ZEITZONEN_SPORT, new Integer[]{ Integer.parseInt(timeZoneSportAmountTextField.getText()), 0 });

        OrderPriority priority = null;

        switch (priorityComboBox.getSelectedIndex()) {
            case 0:
                priority = OrderPriority.NIEDRIG;
                break;
            case 1:
                priority = OrderPriority.MITTEL;
                break;
            case 2:
                priority = OrderPriority.HOCH;
                break;
        }

        Order order = new Order(neededClocks, priority);

        connector.addOrder(order);
    }//GEN-LAST:event_jButton2MouseReleased

    public static void start(final Connector connector, final ExecutorService threadPool) {
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MainFrame(connector, threadPool).setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel assembledValue;
    private javax.swing.JLabel checkedValue;
    private javax.swing.JTextField classicAmountTextField;
    private javax.swing.JTable clockTable;
    private javax.swing.JTable clockTable1;
    private javax.swing.JTable clockTable2;
    private javax.swing.JTable clockTable3;
    private javax.swing.JTable clockTable4;
    private javax.swing.JLabel deliveredValue;
    private javax.swing.JLabel disassembledValue;
    private javax.swing.JLabel gehaeuseValue;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JLabel lederArmbaenderValue;
    private javax.swing.JLabel metallArmbaenderValue;
    private javax.swing.JPanel orderPanel1;
    private javax.swing.JTable orderTable1;
    private javax.swing.JComboBox priorityComboBox;
    private javax.swing.JTextField sportAmountTextField;
    private javax.swing.JPanel statusPanel1;
    private javax.swing.JPanel supplierPanel1;
    private javax.swing.JTable supplierTable;
    private javax.swing.JTextField timeZoneSportAmountTextField;
    private javax.swing.JLabel uhrwerkeValue;
    private javax.swing.JLabel zeigerValue;
    // End of variables declaration//GEN-END:variables
}
