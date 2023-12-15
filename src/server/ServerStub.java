package server;

import client.ClientCommInterface;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;

public class ServerStub extends UnicastRemoteObject implements ServerCommInterface {
    final static String[] hashes = {"34", "19857", "98753", "234928", "89832", "3", "153233", "153234", "153235", "153236",
            "253236", "253237", "253238", "253239", "553240", "553236", "553237", "553238", "553239", "553240"};
    static String currentWord = "";
    static MessageDigest md;
    static ClientCommInterface client;
    static CountDownLatch latch;
    static String teamName;

    public ServerStub() throws RemoteException {
        super();
    }

    public static void main(String[] args) {
        latch = new CountDownLatch(1);

        try {
            System.setProperty("java.rmi.server.hostname", "192.168.1.1");
            System.setProperty("java.security.policy", "security.policy");
            LocateRegistry.createRegistry(1099);
            ServerCommInterface server = new ServerStub();
            Naming.rebind("rmi://192.168.1.1:1099/server", server);
            System.out.println("Server started correctly");
        } catch (RemoteException | MalformedURLException e) {
            throw new RuntimeException(e);
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        long start = System.currentTimeMillis();

        for (int i = 0; i < hashes.length; i++) {
            try {
                byte[] bytes = md.digest(hashes[i].getBytes("UTF-8"));
                String word = new String(bytes, "UTF-8");
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                synchronized (currentWord) {
                    currentWord = word;
                }
                client.publishProblem(bytes, hashes[i].length());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            latch = new CountDownLatch(1);
        }
        long timeNeeded = System.currentTimeMillis() - start;
        System.out.println("Finished hashes in " + (timeNeeded / 1000.00) + " seconds");
    }

    @Override
    public void register(String teamName, ClientCommInterface cc) {
        ServerStub.teamName = teamName;
        client = cc;
        latch.countDown();
    }

    @Override
    synchronized public void submitSolution(String name, String sol) throws Exception {
        synchronized (currentWord) {
            String digestedSol = new String(md.digest(sol.getBytes("UTF-8")), "UTF-8");
            if (currentWord.equals(digestedSol)) {
                System.out.print("Correct answer");
            } else {
                System.out.print("Wrong answer");
            }
            System.out.println(" by " + name);
            latch.countDown();
        }
    }
}
