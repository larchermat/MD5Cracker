import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;

public class ServerStub extends UnicastRemoteObject implements ServerCommInterface {
    final static String[] hashes = {"1532", "34", "19857", "12", "1", "27", "98753", "1234555", "89832", "3"};
    static String currentWord = "";
    static MessageDigest md;
    static ClientCommInterface client;
    static CountDownLatch latch;

    public ServerStub() throws RemoteException {
        super();
    }

    public static void main(String[] args) {
        latch = new CountDownLatch(1);
        try {
            LocateRegistry.createRegistry(1099);
            ServerCommInterface server = new ServerStub();
            Naming.rebind("rmi://localhost/ServerCommService", server);
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
        /*long start = System.currentTimeMillis();
        long current = System.currentTimeMillis();
        while(start - current < 5000){
            current = System.currentTimeMillis();
        }*/
        for (int i = 0; i < hashes.length; i++) {
            latch = new CountDownLatch(1);
            try {
                byte[] bytes = md.digest(hashes[i].getBytes("UTF-8"));
                synchronized (currentWord) {
                    currentWord = new String(bytes, "UTF-8");
                }
                client.publishProblem(bytes, hashes[i].length());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("Finished hashes");
    }

    @Override
    public void register(String teamName, ClientCommInterface cc) throws Exception {
        client = (ClientCommInterface) Naming.lookup("rmi://localhost/" + teamName);
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
