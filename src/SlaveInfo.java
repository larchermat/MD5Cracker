public class SlaveInfo {
    boolean updated;
    boolean waiting;
    int slaveNumber;
    SlaveIF slaveIF;

    public SlaveInfo(boolean updated, boolean waiting, int slaveNumber, SlaveIF slaveIF) {
        this.updated = updated;
        this.waiting = waiting;
        this.slaveNumber = slaveNumber;
        this.slaveIF = slaveIF;
    }

}
