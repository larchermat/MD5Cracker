package client;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfaccia implementata dalla classe {@link Master}
 */
public interface MasterIF extends Remote {
    /**
     * Metodo utilizzato per ricevere una soluzione al corrente problema
     * @param hash l'hash cercato da chi ha inviato la soluzione
     * @param solution la soluzione del problema
     * @throws RemoteException
     */
    void receiveSolution(String hash, int solution) throws RemoteException;

    /**
     * Metodo che registra un nuovo Slave
     * @param slaveIF slave da registrare
     * @throws RemoteException
     */
    void registerSlave(SlaveIF slaveIF) throws RemoteException;

    /**
     * Metodo che segnala che uno slave ha completato l'aggiornamento
     * @param slaveNumber numero dello slave che e' stato aggiornato
     * @throws RemoteException
     */
    void slaveUpdated(int slaveNumber) throws RemoteException;

    /**
     * Metodo che segnala lo status di attesa di uno slave
     * @param slaveNumber numero dello slave chiamante
     * @param slaveWaiting status di attesa dello slave
     * @throws RemoteException
     */
    void slaveWaiting(int slaveNumber, boolean slaveWaiting) throws RemoteException;
}
