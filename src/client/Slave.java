package client;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;

public class Slave extends UnicastRemoteObject implements SlaveIF {
    boolean running;
    String hash;
    final BSTree hashTree;
    int increment;
    int current;
    MessageDigest md;
    MasterIF master;
    int slaveNumber;
    boolean waiting;
    boolean isNewProblem;
    boolean isUpdate;
    int newIncrement;
    int changingPoint;
    CountDownLatch latch;

    public Slave() throws NoSuchAlgorithmException, UnsupportedEncodingException, RemoteException {
        super();

        latch = new CountDownLatch(1);
        running = false;
        isNewProblem = false;
        isUpdate = false;
        md = MessageDigest.getInstance("MD5");
        hash = "";
        current = 0;
        hashTree = new BSTree();
    }

    public static void main(String[] args) {
        try {
            System.setProperty("java.rmi.server.hostname", args[1]);
            //LocateRegistry.createRegistry(1099);
            Slave slave = new Slave();

            System.out.println("Input the name of the client");
            String name = args[0];
            slave.slaveNumber = Integer.parseInt(name);
            System.out.println("Input the hostname");
            String hostName = args[1] + ":1099";
            String masterName = args[2];
            String slaveServiceName = "CrackerSlaveService_" + name;
            try {
                Naming.rebind("rmi://" + hostName + "/" + slaveServiceName, slave);
            } catch (RemoteException | MalformedURLException e) {
                throw new RuntimeException(e);
            }

            try {
                slave.master = (MasterIF) Naming.lookup("rmi://" + masterName + ":1099/CrackerMasterService");
            } catch (NotBoundException | MalformedURLException | RemoteException e) {
                throw new RuntimeException(e);
            }
            System.out.println("client.Master was fetched");
            try {
                slave.master.registerSlave(slave);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
            try {
                slave.latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            slave.lifecycle();
            System.out.println("Closing main");
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void search() {
        isNewProblem = false;
        int checksum = 0;
        for (int j = 0; j < hash.length(); j++) {
            checksum += hash.charAt(j);
        }
        TreeNode node = hashTree.find(checksum);
        if (node != null) {
            Integer solution = node.getNumberForHash(hash);
            if (solution != null) {
                System.out.println("Solution found");
                try {
                    master.receiveSolution(hash, solution, "client.Slave " + slaveNumber);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public void receiveTask(String hash) throws RemoteException {
        System.out.println("client.Slave " + slaveNumber + " received task");
        this.hash = hash;
        isNewProblem = true;
    }

    @Override
    public void start(int base, int increment, String hash) {
        System.out.println("client.Slave running");
        running = true;
        current = base;
        this.increment = increment;
        this.hash = hash;
        System.out.println("Current: " + current + ", increment: " + increment);
        latch.countDown();
    }

    public void lifecycle() {
        System.out.println("lifecycle started");
        while (running) {
            if (isUpdate)
                updateSelf();
            if (!waiting) {
                if (current <= 6000000)
                    run();
            } else {
                try {
                    master.slaveWaiting(slaveNumber, true);
                    latch.await();
                    master.slaveWaiting(slaveNumber, false);
                } catch (InterruptedException | RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
            if (isNewProblem)
                search();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getCurrent() {
        return current;
    }

    @Override
    synchronized public void update(int newIncrement, int changingPoint) {
        this.newIncrement = newIncrement;
        this.changingPoint = changingPoint;
        isUpdate = true;
        latch.countDown();
    }

    public void updateSelf() {
        if (current != changingPoint) {
            for (int i = 0; i < increment; i++) {
                if ((changingPoint - i - current) % increment == 0) {
                    changingPoint -= i;
                    break;
                }
            }
            while (current <= changingPoint) {
                run();
            }
        }
        increment = newIncrement;
        try {
            master.slaveUpdated(slaveNumber);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        isUpdate = false;
        System.out.println("Current: " + current + " , increment: " + increment);
    }

    @Override
    public void setWaiting(boolean waiting) {
        System.out.println("client.Slave " + slaveNumber + " is " + (waiting ? "waiting" : "not waiting"));
        this.waiting = waiting;
        if (waiting && running) {
            latch = new CountDownLatch(1);
        }
    }

    public void run() {
        byte[] bytes = new byte[0];
        try {
            bytes = md.digest((String.valueOf(current).getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            String hashStr = new String(bytes, "UTF-8");
            int checksum = 0;
            for (int j = 0; j < hashStr.length(); j++) {
                checksum += hashStr.charAt(j);
            }
            hashTree.add(checksum, current);
            if (hash.equals(hashStr)) {
                System.out.println("Found solution " + current);
                master.receiveSolution(hashStr, current, "slave" + slaveNumber);
            }
            current = current + increment;
        } catch (UnsupportedEncodingException | RemoteException e) {
            e.printStackTrace();
        }
    }
}
