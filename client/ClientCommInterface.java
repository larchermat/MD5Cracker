package client;
import java.rmi.Remote;

public interface ClientCommInterface extends Remote {

    void publishProblem(byte[] hash, int problemsize) throws Exception;
}