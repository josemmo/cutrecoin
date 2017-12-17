/*
 * Copyright 2017 José Miguel Moreno
 * josemiguel.moreno@alumnos.uva.es
 */

package tk.josemmo;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import tk.josemmo.cutrecoin.Block;
import tk.josemmo.cutrecoin.Cutrecoin;
import tk.josemmo.cutrecoin.Transaction;

/**
 *
 * @author josemmo
 * @author carlos
 */
public class ConnectionManager {
    private Cutrecoin cutrecoin;
    private ServerSocket server;
    private final Thread discoverThread;
    private final ArrayList<InetAddress> peers = new ArrayList<>();
    private boolean isRunning = true;
    private boolean isSynced = false;
    
    
    /**
     * Cutrecoin
     * @param cc 
     */
    public ConnectionManager(Cutrecoin cc) {
        cutrecoin = cc;
        
        /* HILO DE SERVIDOR */
        new Thread(() -> {
            try {
                server = new ServerSocket(Cutrecoin.PORT);
                while (isRunning) acceptClient(server.accept());
            } catch (Exception e) {}
        }).start();
        
        /* HILO DE BÚSQUEDA DE NODOS */
        discoverThread = new Thread(() -> {
            killThread:
            while (isRunning) {
                try {
                    // Buscar nodos en la red local
                    Enumeration<NetworkInterface> interfaces =
                        NetworkInterface.getNetworkInterfaces();
                    while (interfaces.hasMoreElements()) {
                        NetworkInterface iface = interfaces.nextElement();
                        if (iface.isLoopback() || !iface.isUp()) continue;

                        // Obtener IP(s) de la interfaz de red
                        Enumeration<InetAddress> addrs = iface.getInetAddresses();
                        while (addrs.hasMoreElements()) {
                            InetAddress localhost = addrs.nextElement();

                            // Buscar nodos en toda la subred
                            byte[] ip = localhost.getAddress();
                            for (int i=1; i<255; i++) {
                                if (!isRunning) break killThread;
                                ip[3] = (byte) i;
                                InetAddress address = InetAddress.getByAddress(ip);
                                
                                // Comprobar que se añade esta máquina
                                if (address.equals(localhost)) continue;

                                // Intentar abrir socket TCP
                                connectToServer(address);
                            }
                        }
                    }
                    
                    // Sincronizar si es necesario
                    if (!isSynced) sync();
                    
                    // Esperar hasta la siguiente iteración
                    Thread.sleep(30*1000);
                } catch (Exception e) {}
            }
        });
        discoverThread.start();
    }
    
    
    /**
     * On new message
     * 
     * Método que es llamado al recibir un mensaje. Interpreta qué acción
     * deberá tomarse en función del contenido y responde al nodo emisor si
     * es necesario.
     * 
     * @param outputStream
     * @param message 
     */
    private void onNewMessage(ObjectOutputStream outputStream, Object message) {
        if (message instanceof Transaction) {
            cutrecoin.addPendingTransaction((Transaction) message);
        } else if (message instanceof Block) {
            Block b = (Block) message;
            if (cutrecoin.addCandidateBlock(b)) {
                send(outputStream, "getBlock=" + b.getPreviousHash());
            }
        } else if (message instanceof String) {
            String cmd = (String) message;
            if (cmd.equals("getPendingTransactions")) {
                for (Transaction t : cutrecoin.getPendingTransactions()) {
                    send(outputStream, t);
                }
            } else if (cmd.startsWith("getBlock=")) {
                String hash = cmd.split("=")[1];
                Block b = hash.equals("latest") ?
                    cutrecoin.getLastBlock() : cutrecoin.getBlock(hash);
                if (b != null) send(outputStream, b);
            }
        }
    }
    
    
    /**
     * Accept client
     * 
     * Cada vez que se conecta un nuevo nodo cliente con este servidor, se
     * prepara un nuevo hilo para recibir sus mensaje y luego se cierra.
     * 
     * @param s 
     */
    private void acceptClient(Socket s) {
        new Thread(() -> {
            try {
                // Obtener mensaje
                ObjectOutputStream outputStream = new ObjectOutputStream(s.getOutputStream());
                ObjectInputStream inputStream = new ObjectInputStream(s.getInputStream());
                Object message = inputStream.readObject();
                
                // Interpretar mensaje
                onNewMessage(outputStream, message);
                
                // Cerrar conexión
                s.close();
            } catch (Exception e) {}
        }).start();
    }
    
    
    /**
     * Connect to server
     * 
     * Cada vez que es encontrado un servidor, se intenta conectar con él y,
     * en caso de éxito, guarda su IP para futuras comunicaciones.
     * 
     * @param ip 
     */
    private void connectToServer(InetAddress ip) {
        try {
            Socket s = new Socket();
            s.connect(new InetSocketAddress(ip, Cutrecoin.PORT), Cutrecoin.TIMEOUT);
            s.close();
            
            // Añadir a la lista de nodos
            for (InetAddress ia : peers) {
                if (ia.equals(ip)) return;
            }
            peers.add(ip);
        } catch (Exception e) {
            peers.remove(ip);
        }
    }
    
    
    /**
     * Sync
     * 
     * Pide al resto de nodos de la red que "le pongan al día" enviándole
     * los últimos bloques de la cadena y transacciones pendientes.
     */
    private void sync() {
        if (peers.isEmpty()) return;
        propagate("getBlock=latest");
        propagate("getPendingTransactions");
        isSynced = true;
    }
    
    
    /**
     * Send
     * @param stream
     * @param message 
     */
    private void send(ObjectOutputStream stream, Object message) {
        try {
            stream.writeObject(message);
            stream.flush();
        } catch (Exception e) {}
    }
    
    
    /**
     * Propagate
     * 
     * Crea un nuevo hilo desde el que intenta conectar crear un socket con cada
     * uno de los nodos conocidos y envía un mensaje.
     * 
     * @param message 
     */
    public void propagate(Object message) {
        new Thread(() -> {
            for (InetAddress ip : peers) {
                try {
                    // Abrir socket hacia el nodo
                    Socket s = new Socket();
                    s.connect(new InetSocketAddress(ip, Cutrecoin.PORT),
                        Cutrecoin.TIMEOUT);
                    ObjectOutputStream outputStream = new ObjectOutputStream(s.getOutputStream());
                    ObjectInputStream inputStream = new ObjectInputStream(s.getInputStream());
                    
                    // Enviar mensaje
                    send(outputStream, message);
                    
                    // Esperar una posible respuesta
                    try {
                        Object response = inputStream.readObject();
                        onNewMessage(outputStream, response);
                    } catch (Exception e) {}
                    
                    // Cerrar socket
                    s.close();
                } catch (Exception e) {}
            }
        }).start();
    }
    
    
    /**
     * Get node count
     * @return nodeCount
     */
    public int getNodeCount() {
        return peers.size();
    }
    
    
    /**
     * Is synced
     * @return synced
     */
    public boolean isSynced() {
        return isSynced;
    }
    
    
    /**
     * Kill
     */
    public void kill() {
        isRunning = false;
        try {
            server.close();
        } catch (Exception e) {}
        discoverThread.interrupt();
    }
    
}
