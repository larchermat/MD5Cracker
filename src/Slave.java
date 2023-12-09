import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import client.ClientCommInterface;

public class Slave implements SlaveIF, ClientCommInterface{
    boolean running;
    String hash;
    Map<String, Integer> wordsMap;
    int increment;
    int current;
    MessageDigest md;
    MasterIF master;
    int slaveNumber;
    boolean waiting;

    public Slave() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        super();
        md = MessageDigest.getInstance("MD5");
        hash = "";
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
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
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
        if (wordsMap != null) {
            try {
                int num = wordsMap.get(hash);
                master.receiveSolution(hash, num);
            } catch (RuntimeException e) {
                e.printStackTrace();
            } catch (RemoteException e) {
                e.printStackTrace();
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
        byte[] hash = new byte[0];
        try {
            hash = md.digest((String.valueOf(current).getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            String hashStr = new String(hash, "UTF-8");
            wordsMap = new HashMap<>();
            wordsMap.put(hashStr, current);
            // tree = new BSTree(checksum, hashStr, current);
            current = current + increment;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        while (running) {
            while(waiting){}
            run();
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
    public void update(int newIncrement, int changingPoint) {
        while(waiting){}
        for (int i = 0; i < increment; i++) {
            if((changingPoint - i - current) % increment == 0){
                changingPoint -= i;
                break;
            }
        }
        while (current <= changingPoint){
            run();
        }
        increment = newIncrement;
        master.slaveUpdated(slaveNumber);
    }

    @Override
    public void setWaiting(boolean waiting) {
        this.waiting = waiting;
        master.slaveWaiting(slaveNumber, waiting);
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
            wordsMap.put(hashStr, current);
            if (hash.equals(hashStr)) {
                master.receiveSolution(hashStr, current);
            }
            current = current + increment;
        } catch (UnsupportedEncodingException | RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void publishProblem(byte[] hash, int problemsize) throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'publishProblem'");
    }
}
