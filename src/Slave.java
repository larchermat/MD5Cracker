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
    int slaveNumber = 1;
    boolean waiting;
    private CountDownLatch latch;

    public Slave() throws NoSuchAlgorithmException, UnsupportedEncodingException, RemoteException {
        super();
        running = false;
        md = MessageDigest.getInstance("MD5");
        hash = "";
        current = 0;
        wordsMap = new HashMap<>();

        Scanner scanner = new Scanner(System.in);

        System.out.println("Input the name of the client");
        String name = scanner.nextLine();
        System.out.println("Input the hostname");
        String hostName = scanner.nextLine();
        String slaveServiceName = "SlaveClientService_" + name;
        try {
            Naming.rebind("rmi://" + hostName + "/" + slaveServiceName, this);
        } catch (RemoteException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
        String[] details = {hostName, slaveServiceName};

        try {
            master = (MasterIF) Naming.lookup("rmi://" + hostName + "/CrackerMasterService");
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            throw new RuntimeException(e);
        }
        slaveNumber = Integer.parseInt(name);
        try {
            master.registerSlave(details);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        lifecycle();
    }

    public static void main(String[] args) {
        try {
            new Slave();
            System.out.println("Closing main");
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | RemoteException e) {
            throw new RuntimeException(e);
        }

    }

    public void search() {
        Integer solution;
        //System.out.println("Need wordsMap row 72");
        synchronized (wordsMap) {
            //System.out.println("Locked wordsMap row 74");
            solution = wordsMap.getOrDefault(hash, null);
        }
        //System.out.println("Unlocked wordsMap row 77");
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
        System.out.println("Slave " + slaveNumber + " received task " + hash);
        this.hash = hash;
        search();
    }

    @Override
    public void start(int base, int increment, String masterHostName, int slaveNumber, String hash) {
        System.out.println("Slave running");
        /*try {
            master = (MasterIF) Naming.lookup("rmi://" + masterHostName + "/CrackerMasterService");
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            throw new RuntimeException(e);
        }*/
        running = true;
        //waiting = false;
        current = base;
        this.increment = increment;
        this.slaveNumber = slaveNumber;
        this.hash = hash;
        System.out.println("Current: " + current + " , increment: " + increment);
    }

    public void lifecycle() {
        while (running) {
            if (!waiting) {
                run();
            } else {
                try {
                    //master.slaveWaiting(slaveNumber, waiting);
                    latch.await();
                    //master.slaveWaiting(slaveNumber, waiting);
                } catch (InterruptedException e) {
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
        latch.countDown();
        System.out.println("Current: " + current + " , increment: " + increment);
    }

    @Override
    public void setWaiting(boolean waiting) {
        System.out.println("Slave " + slaveNumber + " is " + (waiting ? "waiting" : "not waiting"));
        if (waiting) {
            latch = new CountDownLatch(1);
            System.out.println("End if");
        }
        try {
            master.slaveWaiting(slaveNumber, waiting);
            System.out.println("End try");
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Out try");
        this.waiting = waiting;
        System.out.println("setWaiting ended");
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
            //System.out.println("Need wordsMap row 178");
            synchronized (wordsMap) {
                //System.out.println("Locked wordsMap row 180");
                wordsMap.put(hashStr, current);
            }
            //System.out.println("Unlocked wordsMap row 183");
            if (hash.equals(hashStr)) {
                System.out.println("Found solution");
                master.receiveSolution(hashStr, current, "slave" + slaveNumber);
            }
            current = current + increment;
        } catch (UnsupportedEncodingException | RemoteException e) {
            e.printStackTrace();
        }
    }
}
