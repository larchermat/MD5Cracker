import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MasterIF extends Remote {
    void receiveSolution(String hash, int solution, String thread) throws RemoteException;
    void registerSlave(String[] details) throws RemoteException;
    void slaveUpdated(int slaveNumber) throws RemoteException;
    void slaveWaiting(int slaveNumber, boolean slaveWaiting) throws RemoteException;
}
