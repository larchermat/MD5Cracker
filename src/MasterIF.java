import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MasterIF extends Remote {
    void receiveSolution(String hash, int solution) throws RemoteException;
    void registerSlave(String[] details) throws RemoteException;
    void slaveUpdated(int slaveNumber);
    void slaveWaiting(int slaveNumber, boolean slaveWaiting);
}
