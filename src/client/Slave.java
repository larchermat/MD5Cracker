package client;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

/**
 * Classe client il cui unico scopo e' cercare di risolvere il problema corrente. I metodi senza doc sono analoghi agli
 * omonimi del master
 */
public class Slave extends UnicastRemoteObject implements SlaveIF {
    /**
     * Variabile che indica se lo slave abbia iniziato a lavorare
     */
    boolean running;
    /**
     * Latch utilizzato per gestire i cicli di attesa dello slave
     */
    CountDownLatch latch;
    int slaveNumber;
    //I seguenti campi corrispondono a quelli presenti nella classe Master
    byte[] hash;
    final BSTree hashTree;
    int increment;
    int current;
    MessageDigest md;
    MasterIF master;
    boolean waiting;
    boolean isNewProblem;
    boolean isUpdate;
    int newIncrement;
    int changingPoint;
    int problemSize;

    public Slave() throws NoSuchAlgorithmException, UnsupportedEncodingException, RemoteException {
        super();

        latch = new CountDownLatch(1);
        running = false;
        isNewProblem = false;
        isUpdate = false;
        md = MessageDigest.getInstance("MD5");
        hash = new byte[]{0x01};
        current = 0;
        hashTree = new BSTree();
    }

    public static void main(String[] args) {
        try {
            System.setProperty("java.rmi.server.hostname", args[1]);
            //LocateRegistry.createRegistry(1099);
            Slave slave = new Slave();
            String name = args[0];
            slave.slaveNumber = Integer.parseInt(name);
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
            System.out.println("Master was fetched");
            try {
                slave.master.registerSlave(slave);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
            try {
                //Attesa prima che il master chiami il metodo start di slave
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

    @Override
    public void receiveTask(byte[] hash, int problemSize) throws RemoteException {
        this.problemSize = problemSize;
        this.hash = hash;
        isNewProblem = true;
    }

    @Override
    public void start(int base, int increment, byte[] hash, int problemSize) {
        running = true;
        current = base;
        this.increment = increment;
        this.hash = hash;
        this.problemSize = problemSize;
        //Chiamata a countDown per far proseguire l'esecuzione nel main
        latch.countDown();
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
    public void setWaiting(boolean waiting) {
        this.waiting = waiting;
        //Nel caso in cui lo slave debba attendere e stava gia' eseguendo il proprio lifecycle, il latch viene impostato
        //ad un countdown di 1, cosi' che venga bloccato nel ciclo while del lifecycle
        if (waiting && running) {
            latch = new CountDownLatch(1);
        }
    }

    @Override
    synchronized public void update(int newIncrement, int changingPoint, int problemSize) {
        this.newIncrement = newIncrement;
        this.changingPoint = changingPoint;
        this.problemSize = problemSize;
        isUpdate = true;
        latch.countDown();
    }

    /**
     * Lifecycle dello slave. Analogo per la maggior parte a quello del master tranne che per la fase di attesa.
     * Quando lo slave entra nel blocco if di attesa segnala il master di stare attendendo, e chiama latch.await(), che
     * verra' sbloccato solo una volta finita l'operazione che ha interrotto il ciclo. Una volta ripresa l'esecuzione lo
     * slave comunica al master di aver finito di attendere
     */
    public void lifecycle() {
        while (running) {
            if (waiting) {
                try {
                    master.slaveWaiting(slaveNumber, true);
                    latch.await();
                    master.slaveWaiting(slaveNumber, false);
                } catch (InterruptedException | RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
            if (isUpdate)
                updateSelf();
            if (current <= problemSize) {
                run();
            }
            if (isNewProblem)
                search();
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
            int sum = 0;
            for (byte b : bytes) {
                sum += b;
            }
            hashTree.add(sum, current);
            if (Arrays.equals(hash, bytes)) {
                master.receiveSolution(bytes, current);
            }
            current = current + increment;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void search() {
        isNewProblem = false;
        int sum = 0;
        for (byte b : hash) {
            sum += b;
        }
        TreeNode node = hashTree.find(sum);
        if (node != null) {
            Integer solution = node.getNumberForHash(hash);
            if (solution != null) {
                try {
                    master.receiveSolution(hash, solution);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
        }
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
    }
}
