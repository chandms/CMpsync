package main;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Scanner;

public class Discovery
{
    String BROADCAST_IP;
    int PORT;
    String PEER_ID;

    final Thread[] thread = new Thread[3];
    final BroadcastThread broadcastThread;
    final ListenThread listenThread;
    final PeerExpiryThread peerExpiryThread;

    public HashMap<String, Integer> priorityMap;

    // peerList format PEER_IP : [PEER_ID, timeAfterLastBroadcast]
    public volatile ConcurrentHashMap<String, ArrayList<String>> priorityPeerList;
    public volatile ConcurrentHashMap<String, ArrayList<String>> originalPeerList;

    public Discovery(String BROADCAST_IP, String PEER_ID, int PORT) {
        this.BROADCAST_IP = BROADCAST_IP;
        this.PORT = PORT;
        this.PEER_ID = PEER_ID;


        // Initialize priorities (lower int = higher priority)
        // The peers whose ID starts with these keywords will have the priority
        priorityMap = new HashMap<String, Integer>();
        priorityMap.put("offlineMcs", 1);
        priorityMap.put("mule", 2);
        priorityMap.put("DB", 3);
        priorityMap.put("others", 4);


        // Initialize peer lists
        originalPeerList = new ConcurrentHashMap<String, ArrayList<String>>();
        priorityPeerList = new ConcurrentHashMap<String, ArrayList<String>>();


        // Initialize threads
        broadcastThread = new BroadcastThread(BROADCAST_IP, PORT);
        listenThread = new ListenThread();
        peerExpiryThread = new PeerExpiryThread();
        thread[0] = new Thread(broadcastThread);
        thread[1] = new Thread(listenThread);
        thread[2] = new Thread(peerExpiryThread);
    }

    public void startBroadcast(){
        if (!broadcastThread.isRunning) {
            thread[0] = new Thread(broadcastThread);
            thread[0].start();
        }
    }

    public void stopBroadcast() {
        if(broadcastThread.isRunning) {
            broadcastThread.stop();
        }
    }

    public void startListener() {
        /*
        Check for any zombie thread waiting for broadcast
        If there is no zombie thread start a new thread to
        listen for broadcast
        else revive zombie thread
        */
        if (!listenThread.isRunning) {
            if (thread[1].isAlive()) {
                System.out.println("REVIVING");
                listenThread.revive();
            } else {
                System.out.println("STARTING");
                thread[1] = new Thread(listenThread);
                thread[1].start();
            }
        }
    }

    public void stopListener() {
        System.out.println("STOPPING");
        if (!listenThread.exit) {
            System.out.println("STOPPING2");
            listenThread.stop();
        }
    }

    public void startPeerExpiry(){
        if (!peerExpiryThread.isRunning) {
            thread[2] = new Thread(peerExpiryThread);
            thread[2].start();
        }
    }

    public void stopPeerExpiry() {
        if(peerExpiryThread.isRunning) {
            peerExpiryThread.stop();
        }
    }

    public void startDiscoverer(){
        startBroadcast();
        startListener();
        startPeerExpiry();
    }

    public void stopDiscoverer(){
        stopBroadcast();
        stopListener();
        stopPeerExpiry();
    }


    public int getPeerPriority(String peerIP) {
        /*
        Get priority value of peer from peerIP
         */
        ArrayList<String> peerDetails = originalPeerList.get(peerIP);
        String peerID = peerDetails.get(0);
        for(String priorityId : priorityMap.keySet()) {
            if(peerID.startsWith(priorityId) && !peerID.equals(this.PEER_ID)){
                return priorityMap.get(priorityId);
            }
        }
        return priorityMap.get("others");

    }


    public int getHighestPeerPriority() {
        /*
        Get priority value of highest priority peer
         */
        int maxPriority = priorityMap.get("others");
        for (String s : originalPeerList.keySet()) {
            int priority = getPeerPriority(s);
            if(priority < maxPriority){
                maxPriority = priority;
            }
        }
        return  maxPriority;
    }


    public void updatePriorityPeerList() {
        /*
        Generate the priority peer list from the original peer list
        */

        System.out.println("Generating Priority Peer List");

        int maxPriority = getHighestPeerPriority();

        ConcurrentHashMap<String, ArrayList<String>> newPriorityPeerList = new ConcurrentHashMap<String, ArrayList<String>>();

        for (String s : originalPeerList.keySet()) {
            int spriority = getPeerPriority(s);
            if(spriority <= maxPriority){
                newPriorityPeerList.put(s, originalPeerList.get(s));
            }
        }
        this.priorityPeerList = newPriorityPeerList;

        // Log priority peer list
        for (String s : priorityPeerList.keySet()) {
            String peerID = priorityPeerList.get(s).get(0);
            System.out.println("PRIORITY PEER: " + peerID);
        }
    }



    /**
     * Thread to broadcast Datagram packets
     */
    public class BroadcastThread implements Runnable {
        String BROADCAST_IP;
        int PORT;
        DatagramSocket datagramSocket;
        byte buffer[] = null;
        DatagramPacket datagramPacket;
        volatile boolean exit;
        volatile boolean isRunning;

        public BroadcastThread(String BROADCAST_IP, int PORT) {
            this.BROADCAST_IP = BROADCAST_IP;
            this.PORT = PORT;
            this.exit = false;
            this.isRunning = false;
        }

        @Override
        public void run() {
            try {
                datagramSocket = new DatagramSocket();
                datagramSocket.setBroadcast(true);
                buffer = PEER_ID.getBytes("UTF-8");
                this.isRunning = true;
                while(!this.exit) {
                    datagramPacket = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(BROADCAST_IP), PORT);
                    try {
                        datagramSocket.send(datagramPacket);
                        System.out.println("Broadcast Packet Sent");
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        System.out.println("Broadcast Packet Sending Failed");
                    }
                    Thread.sleep(3000);
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch(UnknownHostException e){
                e.printStackTrace();
            }catch(IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                datagramSocket.close();
            }
            this.exit = false;
            this.isRunning = false;

            System.out.println("Broadcasting Stopped");
        }

        public void stop() {
            this.exit = true;
        }
    }


    /**
     * Thread to listen for broadcasts
     */
    public class ListenThread implements Runnable{
        DatagramPacket datagramPacket;
        byte buffer[];
        DatagramSocket datagramSocket;
        volatile boolean exit;
        volatile boolean isRunning;


        public ListenThread(){
            this.exit = false;
            this.isRunning = false;
        }

        @Override
        public void run() {
            try{
                datagramSocket = new DatagramSocket(PORT, InetAddress.getByName("0.0.0.0"));
                datagramSocket.setBroadcast(true);
                datagramSocket.setSoTimeout(200);

                System.out.println("ListenerThread Start");
                this.isRunning = true;
                while(!this.exit) {
                    buffer = new byte[15000];
                    boolean willUpdatePeer = false;
                    datagramPacket = new DatagramPacket(buffer, buffer.length);
                    try {
                        datagramSocket.receive(datagramPacket);
                        willUpdatePeer = true;          // datagram packet received will update peer list
                    }catch (IOException e) {
                        /*
                        This exception will be caught when we do not receive a datagram packet
                         */
                    }

                    if(willUpdatePeer) {
                        String peerID = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                        updatePeers(datagramPacket.getAddress().getHostAddress(), peerID);
                    }
                } // end of while
            }catch (UnknownHostException e){


            }catch (IOException e){


            } finally {
                try {
                    datagramSocket.close();
                }catch (NullPointerException e){

                }
            }
            this.exit = false;
            this.isRunning = false;
            System.out.println("ListenerThread Stop");

        }

        public void stop() {
            this.exit = true;
        }

        /**
         * Handle zombie state of listening thread
         */
        public void revive() {
            this.exit = false;
        }

        /**
         * Update list of peers
         * @param peerIP the ip address of the discovered peer
         * @param peerID id of the current peer
         */
        public void updatePeers(String peerIP, String peerID ) {
            /*
            Put the ip address in the table
            Set its counter to 0
             */
            ArrayList<String> l = new ArrayList<String>();
            l.add(peerID);
            l.add(0 + "");
            System.out.println("ListenerThread Receive Broadcaset:" + peerID);
            if(originalPeerList.get(peerIP) == null){
                System.out.println("PEER_DISCOVERED, " + peerID + ", " + peerIP);
            }
            originalPeerList.put(peerIP, l);
            updatePriorityPeerList();
        }
    }

    /*
     * Thread to expire peers after T seconds of inactivity
     */
    public class PeerExpiryThread implements Runnable {
        boolean exit = true;
        boolean isRunning = false;

        @Override
        public void run() {
            exit = false;
            isRunning = true;
            while (!exit) {
                for (String s : originalPeerList.keySet()) {
                    ArrayList<String> l = originalPeerList.get(s);
                    int time = Integer.parseInt(l.get(1));
                    if(time >= 10) {
                        System.out.println("PEER_LOST, " + l.get(0) + ", " + s);
                        originalPeerList.remove(s);
                        System.out.println( "PeerExpiryThread Remove:" + l.get(0));
                        updatePriorityPeerList();
                    } else {
                        l.set(1, String.valueOf(time + 1));
                        originalPeerList.put(s, l);
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            exit = false;
            isRunning = false;
        }

        public void stop() {
            exit = true;
        }
    }

    public static void main(String args[])
    {

        String buf= new String();
        String peer= new String();
        Scanner scan = new Scanner(System.in);
        System.out.println("Enter the peer name");
        peer = scan.nextLine();
        Discovery discovery = new Discovery("172.16.63.255",peer,5678);
        discovery.startDiscoverer();
        scan.close();

    }

}
