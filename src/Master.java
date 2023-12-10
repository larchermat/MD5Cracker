import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class Master extends UnicastRemoteObject implements MasterIF, ClientCommInterface {
    ServerCommInterface server;
    String hash;
    final Map<String, Integer> hashMap;
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
    private CountDownLatch latch1;

    public Master() throws RemoteException {
        super();

        try {
            server = (ServerCommInterface) Naming.lookup("rmi://localhost/ServerCommService");
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            throw new RuntimeException(e);
        }

        hash = "";
        slaves = new ArrayList<>();
        slavesUpdatedList = new ArrayList<>();
        slavesWaitingList = new ArrayList<>();
        hashMap = new HashMap<>();
        current = 0;
        increment = 1;
        updated = true;
        waiting = false;
        slavesUpdated = true;
        slavesWaiting = false;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        try {
            Naming.rebind("rmi://localhost/CrackerMasterService", this);
            System.out.println("Master started correctly");
        } catch (RemoteException | MalformedURLException e) {
            System.out.println("Cracker master failed");
            throw new RuntimeException(e);
        }

        try {
            Naming.rebind("rmi://localhost/FSociety", this);
            server.register("FSociety", this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        lifecycle();
    }

    public static void main(String[] args) {
        try {
            new Master();
        } catch (RemoteException e) {
            System.out.println("Cracker master failed");
            throw new RuntimeException(e);
        }
        System.out.println("Closing main");
    }

    @Override
    public void receiveSolution(String hash, int solution, String thread) throws RemoteException {
        System.out.println("Solution received from " + thread);
        if (this.hash.equals(hash)) {
            try {
                server.submitSolution(thread, String.valueOf(solution));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void registerSlave(String[] details) throws RemoteException {
        System.out.println("New slave");
        try {
            SlaveIF newSlave = (SlaveIF) Naming.lookup("rmi://" + details[0] + "/" + details[1]);
            synchronized (slaves) {
                int slaveNumber = slaves.size();
                slaves.add(new SlaveInfo(slaveNumber, newSlave));
            }
            synchronized (slavesUpdatedList) {
                slavesUpdatedList.add(true);
            }
            synchronized (slavesWaitingList) {
                slavesWaitingList.add(false);
            }
            updateSlaves();
        } catch (NotBoundException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateSlaves() {
        System.out.println("Slaves update started");
        latch1 = new CountDownLatch(1);
        waiting = true;
        setSlavesUpdated();
        setSlavesWaiting();
        int size;
        System.out.println("Need slaves row 123");
        synchronized (slaves) {
            System.out.println("Locked slaves row 125");
            size = slaves.size();
        }
        System.out.println("Unlocked slaves row 128");
        while (!slavesUpdated) {
        }
        System.out.println("Slaves are up to date on the last update");
        System.out.println("Need slaves row 131");
        synchronized (slaves) {
            System.out.println("Locked slaves row 133");
            for (SlaveInfo s : slaves) {
                try {
                    s.slaveIF.setWaiting(true);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        System.out.println("Unlocked slaves row 142");
        while (!slavesWaiting) {
            //System.out.println("slaves waiting: " + slavesWaiting);
        }
        int max = current;
        System.out.println("Slaves are all currently waiting");
        System.out.println("Need slaves row 148");
        synchronized (slaves) {
            System.out.println("Locked slaves row 150");
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
                        s.slaveIF.start(max + size, size + 1, "localhost", slaves.indexOf(s), hash);
                    }
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
            System.out.println("Slaves update finished");
        }
        System.out.println("Unlocked slaves row 176");
    }

    @Override
    public void slaveUpdated(int slaveNumber) {
        System.out.println("Slave " + slaveNumber + " is up to date");
        System.out.println("Need slavesUpdatedList row 185");
        synchronized (slavesUpdatedList) {
            System.out.println("Locked slavesUpdatedList row 187");
            slavesUpdatedList.set(slaveNumber, true);
        }
        System.out.println("Unlocked slavesUpdatedList row 190");
        setSlavesUpdated();
    }

    @Override
    public void slaveWaiting(int slaveNumber, boolean slaveWaiting) {
        System.out.println("Slave " + slaveNumber + " is " + (slaveWaiting ? "waiting" : "not waiting"));
        System.out.println("Need slavesWaitingList row 197");
        synchronized (slavesWaitingList) {
            System.out.println("Locked slavesWaitingList row 199");
            slavesWaitingList.set(slaveNumber, slaveWaiting);
        }
        System.out.println("Unlocked slavesWaitingList row 202");
        setSlavesWaiting();
    }

    public void lifecycle() {
        System.out.println("lifecycle started");
        while (true) {
            if (!waiting) {
                run();
            } else {
                try {
                    System.out.println("Waiting");
                    latch1.await();
                    System.out.println("Stopped waiting");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void setSlavesUpdated() {
        boolean temp = true;
        System.out.println("Need slavesUpdatedList row 225");
        synchronized (slavesUpdatedList) {
            System.out.println("Locked slavesUpdatedList row 227");
            for (Boolean b : slavesUpdatedList)
                temp = temp && b;
        }
        System.out.println("Unlocked slavesUpdatedList row 231");
        slavesUpdated = temp && updated;
        System.out.println("In method slaves are " + (slavesUpdated ? "updated" : "not updated"));
    }

    public void setSlavesWaiting() {
        System.out.println("setSlavesWaiting is being executed");
        boolean temp = true;
        System.out.println("Need slavesWaitingList row 238");
        synchronized (slavesWaitingList) {
            System.out.println("Locked slavesWaitingList row 240");
            for (Boolean b : slavesWaitingList)
                temp = temp && b;
        }
        System.out.println("Unlocked slavesWaitingList row 244");
        slavesWaiting = temp && waiting;
        System.out.println("In method slaves are " + (slavesWaiting ? "waiting" : "not waiting"));
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
            //System.out.println("Need hashMap row 258");
            synchronized (hashMap) {
                //System.out.println("Locked hashMap row 260");
                hashMap.put(hashStr, current);
            }
            //System.out.println("Unlocked hashMap row 263");
            //System.out.println("Need hash row 264");
            synchronized (hash) {
                //System.out.println("Locked hash row 266");
                if (hashStr.equals(hash)) {
                    try {
                        receiveSolution(hashStr, current, "master");
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
            //System.out.println("Unlocked hash row 275");
            current = current + increment;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void search() {
        System.out.println("Searching");
        System.out.println("Need hash row 284");
        synchronized (hash) {
            System.out.println("Locked hash row 286");
            Integer solution;
            synchronized (hashMap) {
                solution = hashMap.getOrDefault(hash, null);
            }
            if (solution != null) {
                try {
                    System.out.println("Solution found");
                    receiveSolution(hash, solution, "master");
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        System.out.println("Unlocked hash row 300");
        System.out.println("Done search");
        waiting = false;
        latch1.countDown();
    }

    public void update(int newIncrement, int changingPoint) {
        System.out.println("Started update on master");
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
        System.out.println("Current: " + current + " , increment: " + increment);
        System.out.println("Resumed");
    }

    @Override
    public void publishProblem(byte[] hash, int problemsize) throws Exception {
        System.out.println("Receiving new problem");
        latch1 = new CountDownLatch(1);
        waiting = true;
        System.out.println("Need hash row 329");
        synchronized (this.hash) {
            System.out.println("Locked hash row 331");
            this.hash = new String(hash, "UTF-8");
        }
        System.out.println("Unlocked hash row 334");
        System.out.println("Need slaves row 335");
        synchronized (slaves) {
            System.out.println("Locked slaves row 337");
            slaves.forEach(slaveInfo ->
            {
                try {
                    slaveInfo.slaveIF.receiveTask(this.hash);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        System.out.println("Unlocked slaves row 350");
        System.out.println("Problem passed to slaves");
        search();
    }
}