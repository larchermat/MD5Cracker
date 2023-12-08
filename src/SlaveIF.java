import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SlaveIF extends Remote {
    void receiveTask(String hash) throws RemoteException;
    void start(int base, int increment, String masterHostname, int slaveNumber);
    boolean isRunning();
    int getCurrent();
    void update(int newIncrement, int changingPoint);
    void setWaiting(boolean waiting);
}
