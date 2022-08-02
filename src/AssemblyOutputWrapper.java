import java.util.HashMap;
import java.util.List;

public class AssemblyOutputWrapper {
    public String asm;
    public List<Short> program;
    public HashMap<Short, Integer> pcLineNumLookup;
    public HashMap<Integer, Short> lineNumPCLookup;

    public AssemblyOutputWrapper(String asm, List<Short> program, HashMap<Short, Integer> pcLineNumLookup, HashMap<Integer, Short> lineNumPCLookup) {
        this.asm = asm;
        this.program = program;
        this.pcLineNumLookup = pcLineNumLookup;
        this.lineNumPCLookup = lineNumPCLookup;
    }
}
