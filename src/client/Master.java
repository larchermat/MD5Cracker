package client;

import server.ServerCommInterface;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Master extends UnicastRemoteObject implements MasterIF, ClientCommInterface {
    ServerCommInterface server;
    String hash;
    final BSTree hashTree;
    final List<SlaveInfo> slaves;
    final List<Boolean> slavesUpdatedList;
    final List<Boolean> slavesWaitingList;
    boolean slavesUpdated;
    boolean slavesWaiting;
    int current;
    int increment;
    MessageDigest md;
    boolean waiting;
    boolean updated;
    boolean isNewProblem;
    boolean isUpdate;
    private CountDownLatch latch1;
    int problemSize;

    public Master() throws RemoteException {
        super();
        hash = "";
        slaves = new ArrayList<>();
        slavesUpdatedList = new ArrayList<>();
        slavesWaitingList = new ArrayList<>();
        hashTree = new BSTree();
        current = 0;
        increment = 1;
        updated = true;
        waiting = false;
        slavesUpdated = true;
        slavesWaiting = false;
        isNewProblem = false;
        isUpdate = false;
        problemSize = 5000000;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        Master master = null;
        try {
            System.setProperty("java.security.policy", "security.policy");
            System.setProperty("java.rmi.server.hostname", args[0]);
            LocateRegistry.createRegistry(1099);
            master = new Master();
            Naming.rebind("rmi://" + args[0] + ":1099/CrackerMasterService", master);
            System.out.println("client.Master started correctly");
            master.server = (ServerCommInterface) Naming.lookup("rmi://" + args[1] + ":1099/server");
            master.server.register("FSociety", master);
            master.lifecycle();
        } catch (RemoteException | NotBoundException | MalformedURLException e) {
            System.out.println("Cracker master failed");
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } catch (OutOfMemoryError error) {
            if (master != null)
                System.out.println("Current " + master.current);
            throw new RuntimeException(error);
        }
        System.out.println("Closing main");
    }

    @Override
    public void receiveSolution(String hash, int solution, String thread) throws RemoteException {
        System.out.println("Solution received from " + thread);
        if (this.hash.equals(hash)) {
            try {
                server.submitSolution("FSociety", String.valueOf(solution));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void registerSlave(SlaveIF slaveIF) throws RemoteException {
        System.out.println("New slave");
        synchronized (slaves) {
            int slaveNumber = slaves.size();
            slaves.add(new SlaveInfo(slaveNumber, slaveIF));
        }
        synchronized (slavesUpdatedList) {
            slavesUpdatedList.add(true);
        }
        synchronized (slavesWaitingList) {
            slavesWaitingList.add(true);
        }
        isUpdate = true;
    }

    public void updateSlaves() {
        System.out.println("Slaves update started");
        setSlavesUpdated();
        int size;
        synchronized (slaves) {
            size = slaves.size();
        }
        while (!slavesUpdated) {
        }
        latch1 = new CountDownLatch(1);
        waiting = true;
        setSlavesWaiting();
        //System.out.println("Slaves are up-to-date on the last update");
        synchronized (slaves) {
            for (SlaveInfo s : slaves) {
                try {
                    s.slaveIF.setWaiting(true);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        while (!slavesWaiting) {
        }
        int max = current;
        //System.out.println("Slaves are all currently waiting");
        synchronized (slaves) {
            for (SlaveInfo s : slaves) {
                int slaveCurrent;
                try {
                    slaveCurrent = s.slaveIF.getCurrent();
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
                if (slaveCurrent > max)
                    max = slaveCurrent;
            }
            updated = false;
            update(size + 1, max);
            waiting = false;
            latch1.countDown();
            for (SlaveInfo s : slaves) {
                try {
                    s.slaveIF.setWaiting(false);
                    if (s.slaveIF.isRunning()) {
                        slavesUpdatedList.set(slaves.indexOf(s), false);
                        s.slaveIF.update(size + 1, max);
                    } else {
                        s.slaveIF.start(max + size, size + 1, hash);
                    }
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
            //System.out.println("Slaves update finished");
        }
        isUpdate = false;
    }

    @Override
    public void slaveUpdated(int slaveNumber) {
        //System.out.println("client.Slave " + slaveNumber + " is up to date");
        synchronized (slavesUpdatedList) {
            slavesUpdatedList.set(slaveNumber, true);
        }
        setSlavesUpdated();
    }

    @Override
    public void slaveWaiting(int slaveNumber, boolean slaveWaiting) {
        //System.out.println("client.Slave " + slaveNumber + " is " + (slaveWaiting ? "waiting" : "not waiting"));
        synchronized (slavesWaitingList) {
            slavesWaitingList.set(slaveNumber, slaveWaiting);
        }
        setSlavesWaiting();
    }

    public void lifecycle() {
        System.out.println("lifecycle started");
        while (true) {
            if (isUpdate)
                updateSlaves();
            if (!waiting && current <= problemSize) {
                run();
            } else {
                try {
                    latch1.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            if (isNewProblem)
                newProblemReceived();
        }
    }

    public void setSlavesUpdated() {
        boolean temp = true;
        synchronized (slavesUpdatedList) {
            for (Boolean b : slavesUpdatedList)
                temp = temp && b;
        }
        slavesUpdated = temp && updated;
        //System.out.println("In method slaves are " + (slavesUpdated ? "updated" : "not updated"));
    }

    public void setSlavesWaiting() {
        //System.out.println("setSlavesWaiting is being executed");
        boolean temp = true;
        synchronized (slavesWaitingList) {
            for (Boolean b : slavesWaitingList)
                temp = temp && b;
        }
        slavesWaiting = temp && waiting;
        //System.out.println("In method slaves are " + (slavesWaiting ? "waiting" : "not waiting"));
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

            synchronized (hash) {
                if (hashStr.equals(hash)) {
                    try {
                        System.out.println("Solution found in run");
                        receiveSolution(hashStr, current, "master");
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
            current = current + increment;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void search() {
        //System.out.println("Searching");
        synchronized (hash) {
            int checksum = 0;
            for (int j = 0; j < hash.length(); j++) {
                checksum += hash.charAt(j);
            }
            TreeNode node = hashTree.find(checksum);
            if (node != null) {
                Integer solution = node.getNumberForHash(hash);
                if (solution != null) {
                    try {
                        System.out.println("Solution found");
                        receiveSolution(hash, solution, "master");
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        //System.out.println("Done search");
        //waiting = false;
        //latch1.countDown();
    }

    public void update(int newIncrement, int changingPoint) {
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
        updated = true;
        //System.out.println("Current: " + current + " , increment: " + increment);
    }

    @Override
    public void publishProblem(byte[] hash, int problemsize) throws Exception {
        this.problemSize = problemsize;
        System.out.println("Receiving new problem");
        synchronized (this.hash) {
            this.hash = new String(hash, "UTF-8");
        }
        isNewProblem = true;
        //latch1 = new CountDownLatch(1);
        //waiting = true;
    }

    public void newProblemReceived() {
        isNewProblem = false;
        synchronized (slaves) {
            slaves.forEach(slaveInfo ->
            {
                try {
                    slaveInfo.slaveIF.receiveTask(this.hash);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        search();
    }
}