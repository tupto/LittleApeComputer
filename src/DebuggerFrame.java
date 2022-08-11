import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;

public class DebuggerFrame extends JFrame implements ActionListener, CPUWatcher {
    JMenuBar menuBar;
    JTextArea asmArea;
    JEditorPane lineNums;
    JTextArea registersArea;
    JCheckBox zeroCheck;
    JCheckBox carryCheck;
    JCheckBox signCheck;
    JMenuItem pauseUnpauseMi;
    JMenuItem stepMi;
    JScrollPane asmPane;
    GfxPanel computerPanel;

    final LittleApeComputer cpu;
    Thread cpuThread;

    ArrayList<Short> breakpoints;

    String biosText;

    AssemblyOutputWrapper biosData;
    AssemblyOutputWrapper programData;

    public DebuggerFrame() {
        super("Little Ape Computer");
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        //this.setResizable(false);

        Container container = this.getContentPane();
        GridBagLayout layout = new GridBagLayout();
        container.setLayout(layout);
        GridBagConstraints layoutConstraints = new GridBagConstraints();

        breakpoints = new ArrayList<>();
        breakpoints.add((short) 0x1002);

        JTextPane lines = new JTextPane();
        lines.setBackground(Color.lightGray);
        lines.setEditable(false);
        lines.setMargin(new Insets(2, 2, 2, 8));
        ((DefaultCaret)lines.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        StyledDocument doc = lines.getStyledDocument();
        Style linesStyle = doc.addStyle("lines", null);
        StyleConstants.setFontFamily(linesStyle, "Monospaced");
        StyleConstants.setFontSize(linesStyle, 13);

        Style red = doc.addStyle("red", linesStyle);
        StyleConstants.setForeground(red, Color.red);

        Style black = doc.addStyle("black", linesStyle);
        StyleConstants.setForeground(black, Color.black);

        lines.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int offset = lines.viewToModel(e.getPoint());
                int lineNum = 0;
                while (offset > 0) {
                    try {
                        offset = Utilities.getRowStart(lines, offset) - 1;
                    } catch (BadLocationException ex) {
                        ex.printStackTrace();
                    }
                    lineNum++;
                }
                lineNum--;

                short currPc = cpu.getRegister(LittleApeComputer.PC);
                if (currPc < LittleApeComputer.IC_ADDRESS) {
                    if (biosData.lineNumPCLookup.containsKey(lineNum)) {
                        short breakpointPc = biosData.lineNumPCLookup.get(lineNum);
                        if (breakpoints.contains(breakpointPc))
                            breakpoints.remove(new Short(breakpointPc));
                        else
                            breakpoints.add(breakpointPc);
                    }
                }
                else if (currPc >= LittleApeComputer.RAM_ADDRESS && currPc < LittleApeComputer.VRAM_ADDRESS) {
                    if (programData.lineNumPCLookup.containsKey(lineNum)) {
                        short breakpointPc = programData.lineNumPCLookup.get(lineNum);
                        if (breakpoints.contains(breakpointPc))
                            breakpoints.remove(new Short(breakpointPc));
                        else
                            breakpoints.add(breakpointPc);
                    }
                }

                updateText();
            }
        });

        asmArea = new JTextArea();
        asmArea.setFont(new Font("Monospaced", 0, 13));
        asmArea.setMargin(new Insets(2, 5, 2, 5));

        asmArea.getDocument().addDocumentListener(new DocumentListener() {
            public void updateLineNums() throws BadLocationException {
                int caretPosition = asmArea.getDocument().getLength();
                Element root = asmArea.getDocument().getDefaultRootElement();

                int pcLineNum = -1;
                short lineNumPC = -1;
                short pc = cpu.getRegister(LittleApeComputer.PC);
                if (programData != null) {
                    if (pc < LittleApeComputer.IC_ADDRESS) {
                        pcLineNum = biosData.pcLineNumLookup.get(pc);
                        lineNumPC = biosData.lineNumPCLookup.containsKey(0) ? biosData.lineNumPCLookup.get(0) : -1;
                    }
                    else if (pc >= LittleApeComputer.RAM_ADDRESS && pc < LittleApeComputer.VRAM_ADDRESS) {
                        pcLineNum = programData.pcLineNumLookup.get(pc);
                        lineNumPC = programData.lineNumPCLookup.containsKey(0) ? programData.lineNumPCLookup.get(0) : -1;
                    }
                }

                lines.setText("");
                StyledDocument linesDoc = lines.getStyledDocument();

                linesDoc.insertString(0, "1\n", breakpoints.contains(lineNumPC) ? red : black);
                for (int i = 2 ; i < root.getElementIndex(caretPosition) + 2; i++) {
                    lineNumPC = programData != null && programData.lineNumPCLookup.containsKey(i-1) ? programData.lineNumPCLookup.get(i-1) : -1;
                    linesDoc.insertString(doc.getLength(), i + ((i == pcLineNum + 1 && !cpu.getHalted() && cpu.getPaused()) ? " ->\n" : "\n"), breakpoints.contains(lineNumPC) ? red : black);
                }
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                try {
                    updateLineNums();
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                try {
                    updateLineNums();
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e)  {
                try {
                    updateLineNums();
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        });

        asmPane = new JScrollPane(asmArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        asmPane.setPreferredSize(new Dimension(512, 200));
        asmPane.setRowHeaderView(lines);
        ((DefaultCaret)asmArea.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        registersArea = new JTextArea("0x0000\n0x0000\n0x0000\n0x0000\n0x0000\n0x0000\n0x0000");
        registersArea.setEditable(false);
        registersArea.setFont(new Font("Monospaced", 0, 13));
        registersArea.setMargin(new Insets(2, 5, 2, 5));

        JTextArea registerLabels = new JTextArea("a\nb\nc\nd\ne\nsp\npc");
        registerLabels.setEditable(false);
        registerLabels.setFont(new Font("Monospaced", 0, 13));
        registerLabels.setBackground(Color.LIGHT_GRAY);
        registerLabels.setMargin(new Insets(2, 2, 2, 8));

        JScrollPane registersScroll = new JScrollPane(registersArea, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        registersScroll.setPreferredSize(new Dimension(92, 135));
        registersScroll.setRowHeaderView(registerLabels);

        JPanel statusPane = new JPanel();
        statusPane.setLayout(new BoxLayout(statusPane, BoxLayout.X_AXIS));

        zeroCheck = new JCheckBox();
        zeroCheck.setEnabled(false);
        statusPane.add(new JLabel("Z:"));
        statusPane.add(zeroCheck);

        carryCheck = new JCheckBox();
        carryCheck.setEnabled(false);
        statusPane.add(new JLabel("C:"));
        statusPane.add(carryCheck);

        signCheck = new JCheckBox();
        signCheck.setEnabled(false);
        statusPane.add(new JLabel("S:"));
        statusPane.add(signCheck);

        JPanel registersPane = new JPanel();
        registersPane.setLayout(new BoxLayout(registersPane, BoxLayout.Y_AXIS));
        registersPane.add(new JLabel("Registers"));
        registersPane.add(registersScroll);
        registersPane.add(statusPane);

        menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem newMi = new JMenuItem("New");
        JMenuItem openMi = new JMenuItem("Open");
        JMenuItem saveMi = new JMenuItem("Save");
        newMi.addActionListener(this);
        openMi.addActionListener(this);
        saveMi.addActionListener(this);

        fileMenu.add(newMi);
        fileMenu.add(openMi);
        fileMenu.add(saveMi);

        JMenu editMenu = new JMenu("Edit");
        JMenuItem copyMi = new JMenuItem("Copy");
        JMenuItem cutMi = new JMenuItem("Cut");
        JMenuItem pasteMi = new JMenuItem("Paste");
        copyMi.addActionListener(this);
        cutMi.addActionListener(this);
        pasteMi.addActionListener(this);

        editMenu.add(copyMi);
        editMenu.add(cutMi);
        editMenu.add(pasteMi);

        JMenu debugMenu = new JMenu("Debug");
        JMenuItem resetMi = new JMenuItem("Reset");
        resetMi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        JMenuItem haltMi = new JMenuItem("Halt");
        haltMi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
        pauseUnpauseMi = new JMenuItem("Pause");
        pauseUnpauseMi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
        pauseUnpauseMi.setEnabled(false);
        stepMi = new JMenuItem("Step");
        stepMi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));
        stepMi.setEnabled(false);
        resetMi.addActionListener(this);
        pauseUnpauseMi.addActionListener(this);
        haltMi.addActionListener(this);
        stepMi.addActionListener(this);

        JMenu interruptMenu = new JMenu("Interrupts");
        JMenuItem vblankMi = new JMenuItem("Generate VBLANK");
        vblankMi.addActionListener(this);

        interruptMenu.add(vblankMi);

        debugMenu.add(resetMi);
        debugMenu.add(pauseUnpauseMi);
        debugMenu.add(stepMi);
        debugMenu.add(haltMi);
        debugMenu.add(interruptMenu);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(debugMenu);

        cpu = new LittleApeComputer(this);
        computerPanel = new GfxPanel(cpu);

        this.setJMenuBar(menuBar);

        layoutConstraints.fill = GridBagConstraints.BOTH;
        layoutConstraints.gridheight = 2;
        layoutConstraints.gridx = 0;
        layoutConstraints.gridy = 0;
        layoutConstraints.weightx = 1;
        layoutConstraints.weighty = 1;
        layoutConstraints.insets = new Insets(5, 5, 5, 5);
        this.add(asmPane, layoutConstraints);

        layoutConstraints.fill = GridBagConstraints.NONE;
        layoutConstraints.gridheight = 1;
        layoutConstraints.gridx = 1;
        layoutConstraints.gridy = 0;
        layoutConstraints.weightx = 0;
        layoutConstraints.weighty = 0;
        this.add(computerPanel, layoutConstraints);

        layoutConstraints.fill = GridBagConstraints.NONE;
        layoutConstraints.anchor = GridBagConstraints.NORTHWEST;
        layoutConstraints.gridheight = 1;
        layoutConstraints.gridx = 1;
        layoutConstraints.gridy = 1;
        layoutConstraints.weightx = 0;
        layoutConstraints.weighty = 0;
        this.add(registersPane, layoutConstraints);

        //computerPanel.setBounds(900-512, 0, computerPanel.getWidth(), computerPanel.getHeight());
        this.pack();
        this.setVisible(true);

        File fi = new File("bios.asm");

        try {
            // String
            String s1 = "", sl = "";

            // File reader
            FileReader fr = new FileReader(fi);

            // Buffered reader
            BufferedReader br = new BufferedReader(fr);

            // Initialize sl
            sl = br.readLine();

            // Take the input from the file
            while ((s1 = br.readLine()) != null) {
                sl = sl + "\n" + s1;
            }

            // Set the text
            biosText = sl;
        }
        catch (Exception evt) {
            JOptionPane.showMessageDialog(this, evt.getMessage());
        }

        biosData = new Assembler().assemble(biosText, (short) 0);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String s = e.getActionCommand();

        if (s.equals("Cut")) {
            asmArea.cut();
        }
        else if (s.equals("Copy")) {
            asmArea.copy();
        }
        else if (s.equals("Paste")) {
            asmArea.paste();
        }
        else if (s.equals("Save")) {
            // Create an object of JFileChooser class
            JFileChooser j = new JFileChooser(".");

            // Invoke the showsSaveDialog function to show the save dialog
            int r = j.showSaveDialog(null);

            if (r == JFileChooser.APPROVE_OPTION) {

                // Set the label to the path of the selected directory
                File fi = new File(j.getSelectedFile().getAbsolutePath());

                try {
                    // Create a file writer
                    FileWriter wr = new FileWriter(fi, false);

                    // Create buffered writer to write
                    BufferedWriter w = new BufferedWriter(wr);

                    // Write
                    w.write(asmArea.getText());

                    w.flush();
                    w.close();
                }
                catch (Exception evt) {
                    JOptionPane.showMessageDialog(this, evt.getMessage());
                }
            }
            // If the user cancelled the operation
            else
                JOptionPane.showMessageDialog(this, "the user cancelled the operation");
        }
        else if (s.equals("Open")) {
            // Create an object of JFileChooser class
            JFileChooser j = new JFileChooser(".");

            // Invoke the showsOpenDialog function to show the save dialog
            j.showOpenDialog(null);

            // Set the label to the path of the selected directory
            File fi = new File(j.getSelectedFile().getAbsolutePath());

            try {
                // String
                String s1 = "", sl = "";

                // File reader
                FileReader fr = new FileReader(fi);

                // Buffered reader
                BufferedReader br = new BufferedReader(fr);

                // Initialize sl
                sl = br.readLine();

                // Take the input from the file
                while ((s1 = br.readLine()) != null) {
                    sl = sl + "\n" + s1;
                }

                // Set the text
                asmArea.setText(sl);
            }
            catch (Exception evt) {
                JOptionPane.showMessageDialog(this, evt.getMessage());
            }
        }
        else if (s.equals("New")) {
            asmArea.setText("");
        }
        else if (s.equals("Reset")) {
            asmArea.setEditable(false);
            pauseUnpauseMi.setEnabled(true);
            programData = new Assembler().assemble(asmArea.getText(), (short) LittleApeComputer.RAM_ADDRESS);
            cpu.reset(biosData.program, programData.program);

            if (cpuThread == null) {
                cpuThread = new Thread(cpu);
                cpuThread.setPriority(10);
                cpuThread.start();
            }
        }
        else if (s.equals("Pause")) {
            stepMi.setEnabled(true);
            cpu.setPaused(true);
            pauseUnpauseMi.setText("Resume");
            updateText();
        }
        else if (s.equals("Resume")) {
            stepMi.setEnabled(false);
            cpu.setPaused(false);
            pauseUnpauseMi.setText("Pause");
            updateText();

            synchronized (cpu) {
                cpu.notifyAll();
            }
        }
        else if (s.equals("Step")) {
            cpu.doStep();
            updateText();
        }
        else if (s.equals("Halt")) {
            cpu.setHalted(true);
            updateText();
        }
        else if (s.equals("Generate VBLANK")) {
            cpu.generateInterrupt((short) 0x1);
        }
    }

    private void updateText() {
        //If PC is in the bios
        if (cpu.getRegister(LittleApeComputer.PC) < LittleApeComputer.IC_ADDRESS) {
            asmArea.setText(biosData.asm);
        }
        else {
            asmArea.setText(programData.asm);
        }
        String registers = "";
        for (int i = 0; i < 7; i++) {
            registers += String.format("0x%04X", cpu.getRegister(i)) + ((i < 6) ? "\n" : "");
        }
        registersArea.setText(registers);

        zeroCheck.setSelected(cpu.isZero());
        signCheck.setSelected(cpu.isSign());
        carryCheck.setSelected(cpu.isCarry());
    }

    @Override
    public void OnStep(LittleApeComputer cpu) {
        if (breakpoints.contains(cpu.getRegister(LittleApeComputer.PC))) {
            actionPerformed(new ActionEvent(this, 0, "Pause"));
        }
    }

    @Override
    public void OnRefresh(LittleApeComputer cpu) {
        computerPanel.drawLine(0);
    }

    public void OnHBLANK(LittleApeComputer cpu, int line) {
        computerPanel.drawLine(line+1);
    }

    @Override
    public void OnVBLANK(LittleApeComputer cpu) {
    }
}
