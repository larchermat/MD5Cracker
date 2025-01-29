package client;

import server.ServerCommInterface;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class Client extends UnicastRemoteObject implements ClientCommInterface {
    Master master;
    ServerCommInterface server;
    boolean start;

    protected Client() throws RemoteException {
        super();
        start = false;
    }

    public static void main(String[] args) {
        Client client;
        try {
            System.setProperty("java.security.policy", "security.policy");
            System.setProperty("java.rmi.server.hostname", args[0]);
            //LocateRegistry.createRegistry(1099);
            client = new Client();
            client.master = new Master();
            client.master.server = client;
            Naming.rebind("rmi://" + args[0] + ":1099/FSociety", client);
            Naming.rebind("rmi://" + args[0] + ":1099/CrackerMasterService", client.master);
            System.out.println("client.Master started correctly");
            client.server = (ServerCommInterface) Naming.lookup("rmi://" + args[1] + ":1099/server");
            client.server.register("FSociety", client);
            System.out.println("Entering wait for first problem");
            while (!client.start) {
            }
            System.out.println("Starting lifecycle");
            client.master.lifecycle();
        } catch (RemoteException | NotBoundException | MalformedURLException e) {
            System.out.println("Cracker master failed");
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("Closing main");
    }

    @Override
    public void publishProblem(byte[] hash, int problemsize) throws Exception {
        System.out.println("New problem received in client");
        master.problemSize = problemsize;
        synchronized (master.hash) {
            master.hash = new String(hash, "UTF-8");
        }
        master.isNewProblem = true;
        start = true;
    }

    public void submitSolution(String solution) {
        try {
            server.submitSolution("FSociety", solution);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
