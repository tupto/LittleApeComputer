import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LittleApeComputer implements Runnable {
    public static final int HLT = 0x0;
    public static final int ADD = 0x1;
    public static final int SUB = 0x2;
    public static final int AND = 0x3;
    public static final int OR  = 0x4;
    public static final int XOR = 0x5;
    public static final int LSL = 0x6;
    public static final int LSR = 0x7;
    public static final int MOV = 0x8;
    public static final int LDR = 0x9;
    public static final int STR = 0xa;
    public static final int B = 0xb;
    public static final int BL  = 0xc;
    public static final int PUSH  = 0xd;
    public static final int POP  = 0xe;
    public static final int RET = 0xf;

    public static final int BRANCH = 0x0;
    public static final int BRANCH_SIGN = 0x1;
    public static final int BRANCH_ZERO = 0x2;
    public static final int BRANCH_CARRY = 0x3;

    public static final int REG_A = 0x0;
    public static final int REG_B = 0x1;
    public static final int REG_C = 0x2;
    public static final int REG_D = 0x3;
    public static final int REG_E = 0x4;
    public static final int SP = 0x5;
    public static final int PC = 0x6;
    public static final int FLAGS = 0x7;

    public static final int BIOS_ADDRESS = 0x0000;
    public static final int IC_ADDRESS = 0x0F00;
    public static final int RAM_ADDRESS = 0x1000;
    public static final int VRAM_ADDRESS = 0xC000;

    public static final int IME_ADDRESS = 0x0F00;
    public static final int IRQ_ADDRESS = 0x0F01;
    public static final int IE_ADDRESS = 0x0F02;
    public static final int IF_ADDRESS = 0x0F03;
    public static final int IH_ADDRESS = 0x0F10;
    public static final int VM_ADDRESS = 0x0F20;

    public static final int BG0_ADDRESS = 0xC800;
    public static final int BG1_ADDRESS = 0xD000;
    public static final int BG2_ADDRESS = 0xD800;
    public static final int OAM_ADDRESS = 0xF000;
    public static final int PAL_ADDRESS = 0xF080;
    public static final int BG_ENABLE_ADDRESS = 0xFFF9;
    public static final int BG0_X_ADDRESS = 0xFFFA;
    public static final int BG0_Y_ADDRESS = 0xFFFB;
    public static final int BG1_X_ADDRESS = 0xFFFC;
    public static final int BG1_Y_ADDRESS = 0xFFFD;
    public static final int BG2_X_ADDRESS = 0xFFFE;
    public static final int BG2_Y_ADDRESS = 0xFFFF;

    public static final int HBLANK_CYCLES = 64 * 4;
    public static final int HDRAW_CYCLES = 256 * 4;
    public static final int LINE_CYCLES = HDRAW_CYCLES + HBLANK_CYCLES;
    public static final int VDRAW_CYCLES = 128 * LINE_CYCLES;
    public static final int VBLANK_CYCLES = 64 * LINE_CYCLES;
    public static final int REFRESH_CYCLES = VDRAW_CYCLES + VBLANK_CYCLES;

    private short[] bios = new short[0x0F00];
    private short[] ram = new short[0xB000];
    private short[] vram = new short[0x4000];
    private short[] registers = new short[0x7];

    private boolean interruptMasterEnable;
    private boolean interruptRequestMode;
    private short videoMode;
    private short interruptEnableFlags;
    private short interruptRequestFlags;
    private short interruptHandler;

    private boolean sign;
    private boolean carry;
    private boolean zero;

    private boolean paused;
    private boolean halted = true;
    private boolean step;

    private int cycleCount;

    public boolean getPaused() {
        return this.paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean getHalted() {
        return this.halted;
    }

    public void setHalted(boolean halted) {
        this.halted = halted;
    }

    public boolean getStep() {
        return this.step;
    }

    public void setStep(boolean step) {
        this.step = step;
    }

    public short getRegister(int register) {
        if (register >= 0x7 || register < 0)
            throw new IllegalArgumentException("Register values from 0-6 please");
        return registers[register];
    }

    List<CPUWatcher> watchers;

    public LittleApeComputer() {
        this.watchers = new ArrayList<>();
    }

    public LittleApeComputer(CPUWatcher watcher) {
        this.watchers = Arrays.asList(watcher);
    }

    public void addWatcher(CPUWatcher watcher) {
        this.watchers.add(watcher);
    }

    public void reset(List<Short> bios, List<Short> program) {
        this.bios = new short[0x0F00];
        this.ram = new short[0xB000];
        this.vram = new short[0x4000];
        this.registers = new short[0x7];

        interruptMasterEnable = false;
        interruptRequestMode = false;

        interruptEnableFlags = 0x0000;
        interruptRequestFlags = 0x0000;
        interruptHandler = 0x0000;

        halted = false;
        paused = false;

        cycleCount = 0;

        registers[SP] = (short) 0xBFFF;
        registers[PC] = (short) 0x1000;

        for (int i = 0; i < bios.size(); i++) {
            this.bios[i] = bios.get(i);
        }

        for (int i = 0; i < program.size(); i++) {
            ram[i] = program.get(i);
        }
    }

    public void run() {
        while (!halted) {
            if (!paused) {
                doStep();
            } else {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void doStep() {
        int lineCycleCount = cycleCount % LINE_CYCLES;

        if (cycleCount == VDRAW_CYCLES) {
            //Entered VBLANK
            generateInterrupt((short) 0x1);
            for (CPUWatcher watcher : watchers)
                if (watcher != null)
                    watcher.OnVBLANK(this);
        } else if (lineCycleCount == HDRAW_CYCLES) {
            //Entered HBLANK
            generateInterrupt((short) 0x2);

            for (CPUWatcher watcher : watchers)
                if (watcher != null)
                    watcher.OnHBLANK(this, cycleCount / LINE_CYCLES);
        }

        //Reached end of a screen refresh cycle.
        if (cycleCount == REFRESH_CYCLES) {
            cycleCount = 0;

            for (CPUWatcher watcher : watchers)
                if (watcher != null)
                    watcher.OnRefresh(this);

            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        // If interrupts are enabled and an interrupt request was made
        if (interruptMasterEnable && !interruptRequestMode && (interruptEnableFlags & interruptRequestFlags) > 0) {
            beginInterrupt();
        }

        short pc = registers[PC]++;
        //Fetch instruction
        short instruction = busRead(pc);

        //Decode the instruction
        byte opcode = (byte) ((instruction & 0xF000) >> 12);
        boolean immediateMode = (instruction & 0x0800) == 0x0800;
        boolean immediateWord = (instruction & 0x0400) == 0x0400;

        short sourceRegister = (short) (instruction & 0x0007);
        short targetRegister;
        short value = 0;

        if (immediateMode) {
            if (immediateWord) {
                targetRegister = (short) (instruction & 0x0007);
                value = busRead(registers[PC]++);
            }
            else {
                targetRegister = (short) ((instruction & 0x0300) >> 8);
                value = (short) (instruction & 0x00FF);
            }
        }
        else {
            targetRegister = (short) ((instruction & 0x0070) >> 4);
            if (opcode != PUSH && opcode != POP)
                value = registers[sourceRegister];
        }

        //Please do not try and target the flags register, it is not a real register
        if (targetRegister == FLAGS)
            return;

        //Execute
        switch (opcode) {
            case HLT:
                halted = true;
                break;
            case ADD:
                setCarry((int)registers[targetRegister] + (int)value > Short.MAX_VALUE);

                registers[targetRegister] += value;
                break;
            case SUB:
                setCarry((int)registers[targetRegister] - (int)value < Short.MIN_VALUE);

                registers[targetRegister] -= value;
                break;
            case AND:
                registers[targetRegister] &= value;
                break;
            case OR:
                registers[targetRegister] |= value;
                break;
            case XOR:
                registers[targetRegister] ^= value;
                break;
            case LSL:
                registers[targetRegister] <<= value;
                break;
            case LSR:
                registers[targetRegister] >>= value;
                break;
            case MOV:
                registers[targetRegister] = value;
                break;
            case LDR:
                registers[targetRegister] = busRead(value);
                break;
            case STR:
                busWrite(registers[targetRegister], value);
                break;
            case B:
                int cond = targetRegister & 0x3;

                targetRegister = PC;

                if (cond == BRANCH_SIGN && !isSign()) {
                    break;
                }
                else if (cond == BRANCH_ZERO && !isZero()) {
                    break;
                }
                else if (cond == BRANCH_CARRY && !isCarry()) {
                    break;
                }

                //Immediate bytes are treated as signed PC relative values for jumps
                if (immediateMode && !immediateWord)
                    registers[PC] += (byte)value;
                else
                    registers[PC] = value;
                break;
            case BL:
                cond = targetRegister & 0x3;

                targetRegister = PC;

                if (cond == BRANCH_SIGN && !isSign()) {
                    break;
                }
                else if (cond == BRANCH_ZERO && !isZero()) {
                    break;
                }
                else if (cond == BRANCH_CARRY && !isCarry()) {
                    break;
                }

                //PUSH pc
                busWrite(--registers[SP], registers[PC]);

                //Immediate bytes are treated as signed PC relative values for jumps
                if (immediateMode && !immediateWord)
                    registers[PC] += (byte)value;
                else
                    registers[PC] = value;
                break;
            case PUSH:
                int registersMask = instruction & 0x007F;
                for (int bit = 0; bit < 6; bit++) {
                    if ((registersMask & (1 << bit)) > 0) {
                        busWrite(--registers[SP], registers[bit]);
                    }
                }
                break;
            case POP:
                registersMask = instruction & 0x007F;
                for (int bit = 6; bit >= 0; bit--) {
                    if ((registersMask & (1 << bit)) > 0) {
                        registers[bit] = busRead(registers[SP]++);
                    }
                }
                break;
            case RET:
                targetRegister = PC;
                //POP pc
                registers[PC] = busRead(registers[SP]++);
                break;
            default:
                System.out.println("help");
                break;
        }
        setZero(registers[targetRegister] == 0);
        setSign((registers[targetRegister] & 0x8000) == 0x8000);

        cycleCount++;


        for (CPUWatcher watcher : watchers)
            if (watcher != null)
                watcher.OnStep(this);
    }

    public void generateInterrupt(short interrupt) {
        interruptRequestFlags |= interrupt;
    }

    private void beginInterrupt() {
        //Whack PC on the stack so we can RET later
        busWrite(--registers[SP], registers[PC]);

        //Set PC to the interrupt vector (BIOS function)
        registers[PC] = 0x0002;
        interruptRequestMode = true;
    }

    public short busRead(short address) {
        //Use & 0xFFFF to convert shorts to ints without messing with the bits
        int addr = address & 0xFFFF;
        if (addr < IC_ADDRESS) {
            return bios[addr];
        }

        if (addr < RAM_ADDRESS) {
            if (addr == IME_ADDRESS) {
                return (short) (interruptMasterEnable ? 1 : 0);
            }
            else if (addr == IRQ_ADDRESS) {
                return (short) (interruptRequestMode ? 1 : 0);
            }
            else if (addr == IE_ADDRESS) {
                return interruptEnableFlags;
            }
            else if (addr == IF_ADDRESS) {
                return interruptRequestFlags;
            }
            else if (addr == IH_ADDRESS) {
                return interruptHandler;
            }
            else if (addr == VM_ADDRESS) {
                return videoMode;
            }
            return 0;
        }

        if (addr < VRAM_ADDRESS) {
            return ram[addr-RAM_ADDRESS];
        }

        return vram[addr-VRAM_ADDRESS];
    }

    public void busWrite(short address, short val) {
        int addr = address & 0xFFFF;
        if (addr < IC_ADDRESS) {
            bios[addr] = val;
            return;
        }

        if (addr < RAM_ADDRESS) {
            if (addr == IME_ADDRESS) {
                interruptMasterEnable = (val & 1) == 1;
            }
            else if (addr == IRQ_ADDRESS) {
                interruptRequestMode = (val & 1) == 1;
            }
            else if (addr == IE_ADDRESS) {
                interruptEnableFlags = val;
            }
            else if (addr == IF_ADDRESS) {
                interruptRequestFlags = val;
            }
            else if (addr == IH_ADDRESS) {
                interruptHandler = val;
            }
            else if (addr == VM_ADDRESS) {
                videoMode = (short) ((val & 1) == 1 ? 1 : val & 0x2);
            }
            return;
        }

        if (addr < VRAM_ADDRESS) {
            ram[addr-RAM_ADDRESS] = val;
            return;
        }

        vram[addr-VRAM_ADDRESS] = val;
    }

    public boolean isSign() {
        return sign;
    }

    public void setSign(boolean sign) {
        this.sign = sign;
    }

    public boolean isCarry() {
        return carry;
    }

    public void setCarry(boolean carry) {
        this.carry = carry;
    }

    public boolean isZero() {
        return zero;
    }

    public void setZero(boolean zero) {
        this.zero = zero;
    }
}
