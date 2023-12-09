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
    private CountDownLatch latch;

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
    }

    @Override
    public void receiveSolution(String hash, int solution, String thread) throws RemoteException {
        if (this.hash.equals(hash)) {
            try {
                server.submitSolution(thread, String.valueOf(solution));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    synchronized public void registerSlave(String[] details) throws RemoteException {
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

    @Override
    public void slaveUpdated(int slaveNumber) {
        synchronized (slavesUpdatedList) {
            slavesUpdatedList.set(slaveNumber, true);
        }
        setSlavesUpdated();
    }

    @Override
    public void slaveWaiting(int slaveNumber, boolean slaveWaiting) {
        synchronized (slavesWaitingList) {
            slavesWaitingList.set(slaveNumber, slaveWaiting);
        }
        setSlavesWaiting();
    }

    public void lifecycle() {
        System.out.println("lifecycle started");
        while (true) {
            if (!waiting) {
                run();
            } else {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    synchronized public void updateSlaves() {
        latch = new CountDownLatch(1);
        waiting = true;
        int size;
        synchronized (slaves) {
            size = slaves.size();
        }
        while (!slavesUpdated) {
        }
        synchronized (slaves) {
            for (SlaveInfo s : slaves) {
                try {
                    s.slaveIF.setWaiting(true);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        int max = current;
        while (!slavesWaiting) {
        }
        synchronized (slaves) {
            for (SlaveInfo s : slaves) {
                int slaveCurrent = 0;
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
            for (SlaveInfo s : slaves) {
                try {
                    if (s.slaveIF.isRunning()) {
                        slavesUpdatedList.set(slaves.indexOf(s), false);
                        s.slaveIF.update(size + 1, max);
                        s.slaveIF.setWaiting(false);
                    } else {
                        s.slaveIF.start(max + increment, size + 1, "localhost", slaves.indexOf(s));
                    }
                } catch (RemoteException e){
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void setSlavesUpdated() {
        boolean temp = true;
        synchronized (slavesUpdatedList) {
            for (Boolean b : slavesUpdatedList)
                temp = temp && b;
        }
        slavesUpdated = temp && updated;
    }

    public void setSlavesWaiting() {
        boolean temp = true;
        synchronized (slavesWaitingList) {
            for (Boolean b : slavesWaitingList)
                temp = temp && b;
        }
        slavesWaiting = temp && waiting;
    }

    synchronized public void run() {
        byte[] bytes = new byte[0];
        try {
            bytes = md.digest((String.valueOf(current).getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            String hashStr = new String(bytes, "UTF-8");
            synchronized (hashMap) {
                hashMap.put(hashStr, current);
            }
            synchronized (hash) {
                if (hashStr.equals(hash)) {
                    try {
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
        synchronized (hash) {
            Integer solution;
            synchronized (hashMap) {
                solution = hashMap.getOrDefault(hash, null);
            }
            if (solution != null) {
                try {
                    receiveSolution(hash, solution, "master");
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void update(int newIncrement, int changingPoint) {
        for (int i = 0; i < increment; i++) {
            if ((changingPoint - i - current) % increment == 0) {
                changingPoint -= i;
                break;
            }
        }
        while (current < changingPoint) {
            run();
        }
        increment = newIncrement;
        updated = true;
        latch.countDown();
    }

    @Override
    public void publishProblem(byte[] hash, int problemsize) throws Exception {
        synchronized (this.hash) {
            this.hash = new String(hash, "UTF-8");
        }
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
