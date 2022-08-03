import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Assembler {
    private static final String REGEX_CONSTANT = "ime|irq|ie|if|ih|vm|bios|ic|ram|vram|bg0|bg1|bg2|oam|pal|bg_enable|bg0_x|bg0_y|bg1_x|bg1_y|bg2_x|bg2_y";
    private static final String REGEX_REGISTER = "a|b|c|d|e|sp|pc";
    private static final String REGEX_DECIMAL_NUMBER = "([+-])?([0-9]+)";
    private static final String REGEX_HEX_BYTE = "([+-])?(x|h|0x)([0-9a-fA-F]{1,2})";
    private static final String REGEX_HEX_WORD = "(x|h|0x)([0-9a-fA-F]{3,4})";
    private static final String REGEX_PUSH_POP = "([a-e]|sp|pc)(-([a-e]|sp|pc))?";

    private static final Pattern CONSTANT_PATTERN = Pattern.compile(REGEX_CONSTANT);
    private static final Pattern REGISTER_PATTERN = Pattern.compile(REGEX_REGISTER);
    private static final Pattern DECIMAL_NUMBER_PATTERN = Pattern.compile(REGEX_DECIMAL_NUMBER);
    private static final Pattern HEX_BYTE_PATTERN = Pattern.compile(REGEX_HEX_BYTE);
    private static final Pattern HEX_WORD_PATTERN = Pattern.compile(REGEX_HEX_WORD);
    private static final Pattern PUSH_POP_PATTERN = Pattern.compile(REGEX_PUSH_POP);

    private static final HashMap<String, Integer> CONSTANTS = new HashMap<String, Integer>() {{
        put("ime", LittleApeComputer.IME_ADDRESS);
        put("irq", LittleApeComputer.IRQ_ADDRESS);
        put("ie", LittleApeComputer.IE_ADDRESS);
        put("if", LittleApeComputer.IF_ADDRESS);
        put("ih", LittleApeComputer.IH_ADDRESS);
        put("vm", LittleApeComputer.VM_ADDRESS);
        put("bios", LittleApeComputer.BIOS_ADDRESS);
        put("ic", LittleApeComputer.IC_ADDRESS);
        put("ram", LittleApeComputer.RAM_ADDRESS);
        put("vram", LittleApeComputer.VRAM_ADDRESS);
        put("bg0", LittleApeComputer.BG0_ADDRESS);
        put("bg1", LittleApeComputer.BG1_ADDRESS);
        put("bg2", LittleApeComputer.BG2_ADDRESS);
        put("oam", LittleApeComputer.OAM_ADDRESS);
        put("pal", LittleApeComputer.PAL_ADDRESS);
        put("bg_enable", LittleApeComputer.BG_ENABLE_ADDRESS);
        put("bg0_x", LittleApeComputer.BG0_X_ADDRESS);
        put("bg0_y", LittleApeComputer.BG0_Y_ADDRESS);
        put("bg1_x", LittleApeComputer.BG1_X_ADDRESS);
        put("bg1_y", LittleApeComputer.BG1_Y_ADDRESS);
        put("bg2_x", LittleApeComputer.BG2_X_ADDRESS);
        put("bg3_y", LittleApeComputer.BG2_Y_ADDRESS);
    }};

    private static final HashMap<String, Integer> OPCODE_VALS = new HashMap<String, Integer>() {{
        put("hlt", LittleApeComputer.HLT);
        put("add", LittleApeComputer.ADD);
        put("sub", LittleApeComputer.SUB);
        put("and", LittleApeComputer.AND);
        put("or", LittleApeComputer.OR);
        put("xor", LittleApeComputer.XOR);
        put("lsl", LittleApeComputer.LSL);
        put("lsr", LittleApeComputer.LSR);
        put("mov", LittleApeComputer.MOV);
        put("ldr", LittleApeComputer.LDR);
        put("str", LittleApeComputer.STR);
        put("b", LittleApeComputer.B);
        put("bs", LittleApeComputer.B);
        put("bz", LittleApeComputer.B);
        put("bc", LittleApeComputer.B);
        put("bl", LittleApeComputer.BL);
        put("bls", LittleApeComputer.BL);
        put("blz", LittleApeComputer.BL);
        put("blc", LittleApeComputer.BL);
        put("push", LittleApeComputer.PUSH);
        put("pop", LittleApeComputer.POP);
        put("ret", LittleApeComputer.RET);
    }};

    private static final List<String> OPCODES = Arrays.asList(
            "hlt",
            "add",
            "sub",
            "and",
            "or",
            "xor",
            "lsl",
            "lsr",
            "mov",
            "ldr",
            "str",
            "b",
            "bs",
            "bz",
            "bc",
            "bl",
            "bls",
            "blz",
            "blc",
            "ret",
            "push",
            "pop");

    private static final List<String> REGISTERS = Arrays.asList(
            "a",
            "b",
            "c",
            "d",
            "e",
            "sp",
            "pc");

    private static final List<String> JUMPS = Arrays.asList(
            "b",
            "bs",
            "bz",
            "bc",
            "bl",
            "bls",
            "blz",
            "blc");

    public AssemblyOutputWrapper assemble(String program, short offset) throws IllegalArgumentException {
        List<Short> output = new ArrayList<>();
        HashMap<String, Short> labels = new HashMap<>();
        HashMap<String, List<Integer>> labelUses = new HashMap<>();
        HashMap<Short, Integer> pcLineNumLookup = new HashMap<>();
        HashMap<Integer, Short> lineNumPCLookup = new HashMap<>();

        Scanner scanner = new Scanner(program);
        int lineNum = -1;
        boolean dataMode = false;
        while (scanner.hasNextLine()) {
            lineNum++;
            short pc = (short) output.size();

            String instruction = scanner.nextLine();
            List<String> parts = new ArrayList<>(Arrays.asList(instruction.split(" ")));

            for (int i = parts.size()-1; i >= 0; i--) {
                if (parts.get(i).equals("")) {
                    parts.remove(i);
                }
            }

            if (parts.size() == 0) {
                continue;
            }

            String opcode = parts.get(0).trim().toLowerCase();

            if (opcode.equals(".data")) {
                dataMode = true;
                continue;
            } else if (opcode.equals(".enddata")) {
                dataMode = false;
                continue;
            }

            if (opcode.equals(".file")) {
                File fi = new File(instruction.substring(6));

                try {
                    byte[] data = Files.readAllBytes(fi.toPath());
                    for (int i = 0; i < data.length; i++) {
                        short val = (short) (data[i] << 8);
                        val |= i < data.length ? data[++i] : 0;
                        output.add(val);
                    }
                }
                catch (Exception evt) { }
                continue;
            }

            //Comment
            if (opcode.startsWith(";"))
                continue;

            if (dataMode) {
                List<String> dataParts = new ArrayList<>(Arrays.asList(instruction.split(",")));
                for (String data : dataParts) {
                    short val = parseHexWord(data.trim());
                    output.add(val);
                }
                continue;
            }

            //Label
            if (opcode.endsWith(":")) {
                labels.put(opcode.substring(0, opcode.length()-1), pc);
                continue;
            }

            if (!OPCODES.contains(opcode)) {
                throw new IllegalArgumentException("Unexpected token - invalid opcode");
            }

            short val = (short) (OPCODE_VALS.get(opcode) << 12);
            //Implicit address mode
            if (opcode.equals("hlt") || opcode.equals("ret")) {
                if (parts.size() > 1 && !parts.get(1).startsWith(";")) {
                    throw new IllegalArgumentException("Unexpected token - no token should follow HLT or RET");
                }
                output.add(val);
            }
            else if (opcode.equals("push") || opcode.equals("pop")) {
                String registers = parts.get(1).toLowerCase();
                Matcher m = PUSH_POP_PATTERN.matcher(registers);
                if (m.find()) {
                    int r0 = REGISTERS.indexOf(m.group(1));
                    int r1 = r0;
                    if (m.group(3) != null && m.group(3).length() > 0)
                        r1 = REGISTERS.indexOf(m.group(3));

                    for (int i = r0; i <= r1; i++) {
                        val |= 1 << i;
                    }
                } else if (registers.matches(REGEX_HEX_BYTE)) {
                    short hex = parseHexByte(registers);
                    if ((int) (hex) <= 0x7F) {
                        val |= hex;
                    }
                }
                output.add(val);
            } else if (parts.size() == 1 || parts.get(1).startsWith(";")) {
                throw new IllegalArgumentException("Unexpected token - expected immediate or register");
            }
            else {
                if (JUMPS.contains(opcode)) {
                    if (parts.size() > 2 && !parts.get(2).startsWith(";")) {
                        throw new IllegalArgumentException("Unexpected token - jump instructions are composed of 2 tokens");
                    }

                    String jumpOperand = parts.get(1).toLowerCase();

                    //Indirect
                    if (jumpOperand.matches(REGEX_REGISTER)) {
                        val |= parseRegister(jumpOperand);
                        val |= parseBranch(opcode) << 4;
                        output.add(val);
                    }
                    //Immediate byte
                    else if (jumpOperand.matches(REGEX_HEX_BYTE)) {
                        val |= parseHexByte(jumpOperand);
                        val |= parseBranch(opcode) << 8;
                        val |= 0x0800;
                        output.add(val);
                    }
                    //Immediate word
                    else if (jumpOperand.matches(REGEX_HEX_WORD) || jumpOperand.matches(REGEX_CONSTANT)) {
                        val |= parseBranch(opcode);
                        val |= 0x0C00;
                        output.add(val);

                        if (jumpOperand.matches(REGEX_HEX_WORD)) {
                            val = parseHexWord(jumpOperand);
                        }
                        else {
                            val = CONSTANTS.get(jumpOperand).shortValue();
                        }
                        output.add(val);
                    }
                    //Decimal value
                    else if (jumpOperand.matches(REGEX_DECIMAL_NUMBER)) {
                        short immediate = parseDecimal(jumpOperand);
                        //Immediate word
                        if (immediate > Byte.MAX_VALUE || immediate < Byte.MIN_VALUE) {
                            val |= parseBranch(opcode);
                            val |= 0x0C00;
                            output.add(val);
                            output.add(immediate);
                        }
                        //Immediate byte
                        else {
                            val |= parseBranch(opcode) << 8;
                            val |= immediate & 0xFF;
                            val |= 0x0800;
                            output.add(val);
                        }
                    }
                    else {
                        val |= parseBranch(opcode);
                        val |= 0x0C00;
                        output.add(val);
                        output.add((short) 0); // Add a value for replacing later

                        if (labelUses.containsKey(jumpOperand)) {
                            labelUses.get(jumpOperand).add(output.size()-1);
                        } else {
                            labelUses.put(jumpOperand, new ArrayList<>(Arrays.asList(new Integer[] { output.size()-1 })));
                        }
                    }
                } else {
                    String targetOperand = parts.get(1).toLowerCase();

                    if (!targetOperand.matches(REGEX_REGISTER)) {
                        throw new IllegalArgumentException("Invalid token - expected a register");
                    }

                    byte targetRegister = (byte) REGISTERS.indexOf(targetOperand);

                    String sourceOperand = parts.get(2).toLowerCase();

                    //Indirect
                    if (sourceOperand.matches(REGEX_REGISTER)) {
                        val |= targetRegister << 4;
                        val |= parseRegister(sourceOperand);
                        output.add(val);
                    }
                    //Immediate byte
                    else if (sourceOperand.matches(REGEX_HEX_BYTE) && targetRegister < 0x4) {
                        val |= targetRegister << 8;
                        val |= parseHexByte(sourceOperand);
                        val |= 0x0800;
                        output.add(val);
                    }
                    else if (sourceOperand.matches(REGEX_HEX_BYTE) && targetRegister >= 0x4) {
                        val |= targetRegister;
                        val |= 0x0C00;
                        output.add(val);

                        val = parseHexByte(sourceOperand);

                        output.add(val);
                    }
                    //Immediate word
                    else if (sourceOperand.matches(REGEX_HEX_WORD) || sourceOperand.matches(REGEX_CONSTANT)) {
                        val |= targetRegister;
                        val |= 0x0C00;
                        output.add(val);

                        if (sourceOperand.matches(REGEX_HEX_WORD)) {
                            val = parseHexWord(sourceOperand);
                        }
                        else {
                            val = CONSTANTS.get(sourceOperand).shortValue();
                        }

                        output.add(val);
                    }
                    //Decimal value
                    else if (sourceOperand.matches(REGEX_DECIMAL_NUMBER)) {
                        short immediate = parseDecimal(sourceOperand);
                        //Immediate word
                        if (immediate > Byte.MAX_VALUE || immediate < Byte.MIN_VALUE || targetRegister >= 0x4) {
                            val |= targetRegister;
                            val |= 0x0C00;
                            output.add(val);
                            output.add(immediate);
                        }
                        //Immediate byte
                        else {
                            val |= targetRegister << 8;
                            val |= immediate & 0xFF;
                            val |= 0x0800;
                            output.add(val);
                        }
                    }
                    else {
                        val |= targetRegister;
                        val |= 0x0C00;
                        output.add(val);
                        output.add((short) 0); // Add a value for replacing later

                        if (labelUses.containsKey(sourceOperand)) {
                            labelUses.get(sourceOperand).add(output.size()-1);
                        } else {
                            labelUses.put(sourceOperand, new ArrayList<>(Arrays.asList(new Integer[] { output.size()-1 })));
                        }
                    }
                }
            }

            pcLineNumLookup.put((short) (pc + offset), lineNum);
            lineNumPCLookup.put(lineNum, (short) (pc + offset));
        }

        labelUses.forEach((l, u) -> {
            if (labels.containsKey(l)) {
                for (Integer i : u) {
                    output.set(i, (short) (labels.get(l) | offset));
                }
            }
            else
                throw new IllegalArgumentException("Invalid token - unknown label " + l);
        });

        return new AssemblyOutputWrapper(program, output, pcLineNumLookup, lineNumPCLookup);
    }

    private byte parseRegister(String text) {
        return (byte) REGISTERS.indexOf(text);
    }

    private byte parseBranch(String text) {
        if (text.endsWith("s")) {
            return LittleApeComputer.BRANCH_SIGN;
        }
        if (text.endsWith("z")) {
            return LittleApeComputer.BRANCH_ZERO;
        }
        if (text.endsWith("C")) {
            return LittleApeComputer.BRANCH_CARRY;
        }
        return LittleApeComputer.BRANCH;
    }

    private short parseHexByte(String text) {
        Matcher m = HEX_BYTE_PATTERN.matcher(text);
        m.find();

        short val = (short) (Integer.parseInt(m.group(3), 16) & 0xFF);
        if (m.group(1) == "-") {
            val = (byte) -val;
        }
        return val;
    }

    private short parseHexWord(String text) {
        Matcher m = HEX_WORD_PATTERN.matcher(text);
        m.find();

        short val = (short) (Integer.parseInt(m.group(2), 16) & 0xFFFF);

        return val;
    }

    private short parseDecimal(String text) {
        Matcher m = DECIMAL_NUMBER_PATTERN.matcher(text);
        m.find();

        short val = Short.parseShort(m.group(2));

        if (Objects.equals(m.group(1), "-")) {
            val = (short) -val;
        }

        return val;
    }
}
