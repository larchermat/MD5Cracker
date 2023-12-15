package client;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfaccia implementata dalla classe Slave
 */
public interface SlaveIF extends Remote {
    /**
     * Metodo che inoltra allo slave il nuovo problema
     * @param hash
     * @param problemSize
     * @throws RemoteException
     */
    void receiveTask(byte[] hash, int problemSize) throws RemoteException;

    /**
     * Metodo che da' il via ad uno slave
     * @param base base da cui inizia la ricerca dello slave
     * @param increment incremento che serve ad aggiornare il valore corrente controllato
     * @param hash hash da trovare
     * @param problemSize dimensione del problema
     * @throws RemoteException
     */
    void start(int base, int increment, byte[] hash, int problemSize) throws RemoteException;

    /**
     * Getter della variabile running
     * @return
     * @throws RemoteException
     */
    boolean isRunning() throws RemoteException;

    /**
     * Getter della variabile current
     * @return
     * @throws RemoteException
     */
    int getCurrent() throws RemoteException;

    /**
     * Metodo che aggiorna lo slave
     * @param newIncrement nuovo incremento da utilizzare
     * @param changingPoint obbiettivo da raggiungere prima di cambiare incremento
     * @param problemSize
     * @throws RemoteException
     */
    void update(int newIncrement, int changingPoint, int problemSize) throws RemoteException;

    /**
     * Metodo che imposta lo status di attesa dello slave
     * @param waiting valore che rappresenta lo status corrente di attesa
     * @throws RemoteException
     */
    void setWaiting(boolean waiting) throws RemoteException;
}
