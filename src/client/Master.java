package client;

import server.ServerCommInterface;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Classe che rappresenta il Client che comunica con il server. Il suo compito e' quello di produrre hash e cercare
 * soluzioni come gli {@link Slave Slave}, e allo stesso tempo gestirli
 */
public class Master extends UnicastRemoteObject implements MasterIF, ClientCommInterface {
    ServerCommInterface server;
    /**
     * Il corrente problema da risolvere
     */
    byte[] hash;
    /**
     * Struttura dati utilizzata per salvare i numeri gia' hashati in precedenza
     */
    final BSTree hashTree;
    /**
     * Lista di slave registrati
     */
    final List<SlaveIF> slaves;
    /**
     * Lista degli status di update degli slave (esempio: l'elemento 0 di questa lista, indica se lo slave in posizione
     * 0 della lista slaves è aggiornato o no). Uno slave e' aggiornato se il proprio increment corrisponde a quello del
     * master
     */
    final List<Boolean> slavesUpdatedList;
    /**
     * Lista degli status di attesa degli slave (esempio: l'elemento 0 di questa lista, indica se lo slave in posizione
     * 0 della lista slaves è in attesa o no)
     */
    final List<Boolean> slavesWaitingList;
    /**
     * Variabile che indica se tutti gli slave sono aggiornati o no
     */
    boolean slavesUpdated;
    /**
     * Variabile che indica se tutti gli slave sono in attesa o no
     */
    boolean slavesWaiting;
    /**
     * Il numero corrente che sta essendo hashato. Il current inizia a 0 e viene incrementato sommandogli l'increment
     */
    int current;
    /**
     * Incremento che varia il numero corrente. L'incremento dipende dal numero di slaves presenti: inizia ad 1, in
     * quanto il master da solo deve controllare ogni numero; dopo il primo slave sale a 2, quindi ora uno tra slave e
     * master controllera' tutti i numeri pari e l'altro i dispari, ecc...
     */
    int increment;
    /**
     * Algoritmo di digest usato per generare gli hash
     */
    MessageDigest md;
    /**
     * Variabile che indica se e' presente un nuovo problema
     */
    boolean isNewProblem;
    /**
     * Variabile che indica se e' necessario effettuare un update (l'update e' necessario nel caso di arrivo di un nuovo
     * slave)
     */
    boolean isUpdate;
    int problemSize;

    public Master() throws RemoteException {
        super();
        hash = new byte[]{0x01};
        slaves = new ArrayList<>();
        slavesUpdatedList = new ArrayList<>();
        slavesWaitingList = new ArrayList<>();
        hashTree = new BSTree();
        current = 0;
        increment = 1;
        slavesUpdated = true;
        slavesWaiting = true;
        isNewProblem = false;
        isUpdate = false;
        problemSize = 20000000;
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
            //LocateRegistry.createRegistry(1099);
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
    public void publishProblem(byte[] hash, int problemsize) throws Exception {
        this.problemSize = problemsize;
        synchronized (this.hash) {
            this.hash = hash;
        }
        isNewProblem = true;
    }

    @Override
    public void receiveSolution(byte[] hash, int solution) throws RemoteException {
        //Il metodo prima si assicura che l'hash soluzione sia uguale a quello ricercato
        if (Arrays.equals(this.hash, hash)) {
            try {
                server.submitSolution("FSociety", String.valueOf(solution));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void slaveUpdated(int slaveNumber) {
        synchronized (slavesUpdatedList) {
            slavesUpdatedList.set(slaveNumber, true);
        }
        //Viene effettuata una chiamata per aggiornare il corrente status di update di tutti gli slave
        setSlavesUpdated();
    }

    /**
     * Metodo che aggiorna la variabile slavesUpdated
     */
    public void setSlavesUpdated() {
        //E' necessaria una variabile temp inizializzata a true
        boolean temp = true;
        synchronized (slavesUpdatedList) {
            //La variabile temp viene aggiornata assegnandole ogni valore update nella lista, e nel caso uno sia falso
            //il ciclo si interrompe. Cosi' facendo, se tutti gli elementi sono true, la variabile slavesUpdated sara'
            //true a sua volta, perche' tutti gli slaves sono aggiornati. In caso anche solo uno slave non fosse
            //aggiornato allora slavesUpdated sara' false
            for (Boolean b : slavesUpdatedList) {
                temp = b;
                if (!b)
                    break;
            }
        }
        slavesUpdated = temp;
    }

    @Override
    public void slaveWaiting(int slaveNumber, boolean slaveWaiting) {
        synchronized (slavesWaitingList) {
            slavesWaitingList.set(slaveNumber, slaveWaiting);
        }
        setSlavesWaiting();
    }

    /**
     * Metodo che aggiorna la variabile slavesWaiting. Il suo funzionamento e' analogo al metodo setSlavesUpdated
     */
    public void setSlavesWaiting() {
        boolean temp = true;
        synchronized (slavesWaitingList) {
            for (Boolean b : slavesWaitingList) {
                temp = b;
                if (!b)
                    break;
            }
        }
        slavesWaiting = temp;
    }

    @Override
    public void registerSlave(SlaveIF slaveIF) throws RemoteException {
        synchronized (slaves) {
            slaves.add(slaveIF);
        }
        synchronized (slavesUpdatedList) {
            slavesUpdatedList.add(true);
        }
        synchronized (slavesWaitingList) {
            slavesWaitingList.add(true);
        }
        isUpdate = true;
    }

    /**
     * Metodo principale di Master che rappresenta il suo ciclo continuo
     */
    public void lifecycle() {
        System.out.println("lifecycle started");
        while (true) {
            if (isUpdate)
                updateSlaves();
            if (current <= problemSize) {
                run();
            } /*else {
                break;
            }*/
            if (isNewProblem)
                newProblemReceived();
        }
        //System.out.println("Reached cap");
        //System.out.println("Current:" + current + ", increment:" + increment);
    }

    /**
     * Metodo che aggiorna gli slave (chiamato solo quando e' presente un nuovo slave)
     */
    public void updateSlaves() {
        //setSlavesUpdated();

        synchronized (slaves) {
            int size = slaves.size();

            //Attendiamo finche' tutti gli slaves non sono aggiornati prima di continuare con l'aggiornamento
            while (!slavesUpdated) {
            }

            //setSlavesWaiting();

            //Mettiamo tutti gli slaves in attesa per aggiornare
            for (SlaveIF s : slaves) {
                try {
                    s.setWaiting(true);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }

            //Attendiamo che tutti gli slave siano in attesa
            while (!slavesWaiting) {
            }

            //Per aggiornare gli slaves impostiamo il current massimo tra slaves e master come il nuovo obbiettivo da
            //raggiungere prima di cambiare increment
            int max = getMaxCurrent();

            //Aggiorniamo tutti gli slaves che stanno gia' runnando e diamo lo start a quelli che devono ancora iniziare
            for (SlaveIF s : slaves) {
                try {
                    s.setWaiting(false);
                    if (s.isRunning()) {
                        slavesUpdatedList.set(slaves.indexOf(s), false);
                        s.update(size + 1, max, problemSize);
                    } else {
                        s.start(max + size, size + 1, hash, problemSize);
                    }
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
            update(size + 1, max);
        }
        isUpdate = false;
    }

    private int getMaxCurrent() {
        int max = current;

        for (SlaveIF s : slaves) {
            int slaveCurrent;
            try {
                slaveCurrent = s.getCurrent();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
            if (slaveCurrent > max)
                max = slaveCurrent;
        }
        return max;
    }

    /**
     * Ciclo di run del Master. In questo metodo viene fatto l'hash del numero current, viene salvato, e confrontato con
     * l'hash corrente da trovare
     */
    public void run() {
        byte[] bytes = new byte[0];
        try {
            bytes = md.digest((String.valueOf(current).getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        int sum = 0;
        for (byte aByte : bytes) {
            sum += aByte;
        }
        //Utilizziamo la somma dei valori di ogni byte come chiave di ricerca all'interno dell'albero
        hashTree.add(sum, current);

        synchronized (hash) {
            if (Arrays.equals(bytes, hash)) {
                try {
                    receiveSolution(bytes, current);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        current = current + increment;
    }

    /**
     * Metodo che cerca l'hash tra i valori gia' hashati in precedenza
     */
    public void search() {
        synchronized (hash) {
            int sum = 0;
            for (byte b : hash) {
                sum += b;
            }
            TreeNode node = hashTree.find(sum);
            if (node != null) {
                Integer solution = node.getNumberForHash(hash);
                if (solution != null) {
                    try {
                        receiveSolution(hash, solution);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    /**
     * Metodo che aggiorna il Master
     *
     * @param newIncrement
     * @param changingPoint
     */
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
    }

    /**
     * Metodo che inoltra agli slaves il nuovo problema ricevuto e da' il via ad una fase di ricerca nel master
     */
    public void newProblemReceived() {
        isNewProblem = false;
        synchronized (slaves) {
            slaves.forEach(slave ->
            {
                try {
                    slave.receiveTask(this.hash, this.problemSize);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        search();
    }
}