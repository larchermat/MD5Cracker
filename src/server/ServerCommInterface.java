package server;

import client.ClientCommInterface;
import java.rmi.Remote;

public interface ServerCommInterface extends Remote {

    void register(String teamName, ClientCommInterface cc) throws Exception;

    void submitSolution(String name, String sol) throws Exception;

}