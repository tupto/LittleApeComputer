public interface CPUWatcher {
    void OnStep(LittleApeComputer cpu);
    void OnHBLANK(LittleApeComputer cpu, int line);
    void OnVBLANK(LittleApeComputer cpu);
    void OnRefresh(LittleApeComputer cpu);
}
