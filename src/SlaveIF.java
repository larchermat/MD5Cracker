import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SlaveIF extends Remote {
    void receiveTask(String hash) throws RemoteException;
    void start(int base, int increment, String hash) throws RemoteException;
    boolean isRunning() throws RemoteException;
    int getCurrent() throws RemoteException;
    void update(int newIncrement, int changingPoint) throws RemoteException;
    void setWaiting(boolean waiting) throws RemoteException;
}
