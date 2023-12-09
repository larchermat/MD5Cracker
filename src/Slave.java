import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class Slave extends UnicastRemoteObject implements SlaveIF {
    boolean running;
    String hash;
    final Map<String, Integer> wordsMap;
    int increment;
    int current;
    MessageDigest md;
    MasterIF master;
    int slaveNumber;
    boolean waiting;
    private CountDownLatch latch;

    public Slave() throws NoSuchAlgorithmException, UnsupportedEncodingException, RemoteException {
        super();
        md = MessageDigest.getInstance("MD5");
        hash = "";
        current = 0;
        wordsMap = new HashMap<>();
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Input the name of the client");
        String name = scanner.nextLine();
        System.out.println("Input the hostname");
        String hostName = scanner.nextLine();
        String slaveServiceName = "SlaveClientService_" + name;

        SlaveIF slave;
        try {
            slave = new Slave();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | RemoteException e) {
            throw new RuntimeException(e);
        }

        try {
            Naming.rebind("rmi://" + hostName + "/" + slaveServiceName, slave);
        } catch (RemoteException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
        String[] details = {hostName, slaveServiceName};
        MasterIF master;
        try {
            master = (MasterIF) Naming.lookup("rmi://" + hostName + "/CrackerMasterService");
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            throw new RuntimeException(e);
        }
        try {
            master.registerSlave(details);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void search() {
        Integer solution;
        synchronized (wordsMap) {
            solution = wordsMap.getOrDefault(hash, null);
        }
        if (solution != null) {
            try {
                master.receiveSolution(hash, solution, "slave" + slaveNumber);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void receiveTask(String hash) throws RemoteException {
        this.hash = hash;
        search();
    }

    @Override
    public void start(int base, int increment, String masterHostName, int slaveNumber) {
        System.out.println("Slave running");
        try {
            master = (MasterIF) Naming.lookup("rmi://" + masterHostName + "/CrackerMasterService");
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            throw new RuntimeException(e);
        }
        running = true;
        waiting = false;
        current = base;
        this.increment = increment;
        this.slaveNumber = slaveNumber;

        while (running) {
            if (!waiting) {
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
        try {
            master.slaveUpdated(slaveNumber);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        latch.countDown();
    }

    @Override
    synchronized public void setWaiting(boolean waiting) {
        latch = new CountDownLatch(1);
        this.waiting = waiting;
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
            synchronized (wordsMap) {
                wordsMap.put(hashStr, current);
            }
            if (hash.equals(hashStr)) {
                master.receiveSolution(hashStr, current, "slave" + slaveNumber);
            }
            current = current + increment;
        } catch (UnsupportedEncodingException | RemoteException e) {
            e.printStackTrace();
        }
    }
}
