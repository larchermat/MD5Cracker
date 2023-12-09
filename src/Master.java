import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.List;

import client.ClientCommInterface;
import server.ServerCommInterface;

public class Master implements MasterIF, ServerCommInterface{
    String hash;
    List<SlaveInfo> slaves;
    boolean slavesUpdated;
    boolean slavesWaiting;
    public Master(){
        super();
        slaves = new ArrayList<>();
    }

    public static void main(String[] args){
        try {
            System.setProperty("java.security.policy","security.policy");
            LocateRegistry.createRegistry(1099);
            MasterIF master = new Master();
            Naming.rebind("rmi://localhost/CrackerMasterService", master);
            System.out.println("Server started correctly");
        } catch (RemoteException | MalformedURLException e) {
            System.out.println("Chat server failed");
            throw new RuntimeException(e);
        }
    }
    @Override
    public void receiveSolution(String hash, int solution) throws RemoteException {
        if(this.hash.equals(hash)){
            /*TODO*/
        }
        getNewHash();
        slaves.forEach(slaveInfo ->
                {
                    try {
                        slaveInfo.slaveIF.receiveTask(this.hash);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Override
    public void registerSlave(String[] details) throws RemoteException {
        try {
            SlaveIF newSlave = (SlaveIF) Naming.lookup("rmi://" + details[0] + "/" + details[1]);
            int slaveNumber = slaves.size();
            slaves.add(new SlaveInfo(true, false, slaveNumber, newSlave));
            updateSlaves();
        } catch (NotBoundException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void slaveUpdated(int slaveNumber) {
        slaves.get(slaveNumber).updated = true;
        setSlavesUpdated();
    }

    @Override
    public void slaveWaiting(int slaveNumber, boolean slaveWaiting) {
        slaves.get(slaveNumber).waiting = slaveWaiting;
        setSlavesWaiting();
    }

    public String getNewHash(){
        return "";
    }

    public void lifecycle(){

    }

    public void updateSlaves(){
        int size = slaves.size();
        while (!slavesUpdated){}
        for(SlaveInfo s: slaves)
            s.slaveIF.setWaiting(true);
        int max = 0;
        while(!slavesWaiting){}
        for (SlaveInfo s: slaves){
            int slaveCurrent = s.slaveIF.getCurrent();
            if(slaveCurrent > max)
                max = slaveCurrent;
        }
        for(SlaveInfo s: slaves) {
            if(s.slaveIF.isRunning()) {
                s.slaveIF.update(size, max);
                s.slaveIF.setWaiting(false);
            }else{
                s.slaveIF.start(max+1,size,"localhost",slaves.indexOf(s));
            }
        }
    }

    public void setSlavesUpdated(){
        slavesUpdated = true;
        for(SlaveInfo s: slaves)
            slavesUpdated = slavesUpdated && s.updated;
    }

    public void setSlavesWaiting(){
        for(SlaveInfo s: slaves)
            slavesWaiting = slavesWaiting && s.waiting;
    }

    @Override
    public void register(String teamName, ClientCommInterface cc) throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'register'");
    }

    @Override
    public void submitSolution(String name, String sol) throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'submitSolution'");
    }


}
