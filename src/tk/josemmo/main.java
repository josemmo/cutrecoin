/*
 * Copyright 2017 José Miguel Moreno
 * josemiguel.moreno@alumnos.uva.es
 */

package tk.josemmo;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;
import tk.josemmo.cutrecoin.Block;
import tk.josemmo.cutrecoin.Cutrecoin;
import tk.josemmo.cutrecoin.Transaction;
import tk.josemmo.cutrecoin.Utils;

/**
 *
 * @author josemmo
 * @author carlos
 */
public class main {
    
    private static KeyPair userCredentials;
    private static Cutrecoin cutrecoin;
    private static ConnectionManager cm;
    
    /**
     * Pause
     */
    public static void pause() {
        System.out.print("\nPulsa ENTER para continuar . . .");
        MyInput.readInt();
        clear();
    }
    
    
    /**
     * Clear
     */
    public static void clear() {        
        // Limpiar consola
        try {
            final String operatingSystem = System.getProperty("os.name");
            if (operatingSystem.contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                Runtime.getRuntime().exec("clear");
            }
        } catch (Exception e) {}
        
        // Mostrar cabecera
        System.out.println(
                "   _____      _                      _       \n" +
                "  / ____|    | |                    (_)      \n" +
                " | |    _   _| |_ _ __ ___  ___ ___  _ _ __  \n" +
                " | |   | | | | __| '__/ _ \\/ __/ _ \\| | '_ \\ \n" +
                " | |___| |_| | |_| | |  __/ (_| (_) | | | | |\n" +
                "  \\_____\\__,_|\\__|_|  \\___|\\___\\___/|_|_| |_|\n" +
                (cm.isSynced() ? "" :
                    "                  AVISO: nodo no sincronizado"
                ) + "\n");
    }
    
    
    /**
     * Menú "Nueva transacción"
     */
    public static void menuNuevaTransaccion() {
        // Obtener datos del usuario
        float balance = cutrecoin.getBalance(userCredentials.getPublic());
        int addrLength = Utils.keyToBase64(userCredentials.getPublic()).length();
        if (balance <= 0) {
            System.out.println("[!] No tienes dinero suficiente");
            pause();
            return;
        }
        
        // Pedir dirección del destinatario
        String toAddr = "";
        System.out.println("========= DIRECCIÓN DEL DESTINATARIO =========");
        while (toAddr.length() < addrLength) {
            String buff = MyInput.readString();
            toAddr += buff;
            System.out.println(toAddr);
        }
        toAddr = toAddr.trim();
        System.out.println("==============================================");
        
        // Validar dirección
        if (toAddr.length() != addrLength) {
            System.out.println("[!] Dirección no válida. Envío abortado.");
            pause();
            return;
        }
        PublicKey toKey = Utils.base64ToKey(toAddr);
        if (toKey != null) {
            System.out.println("[!] Dirección no válida. Envío abortado.");
            pause();
            return;
        }
        
        // Pedir cantidad a enviar
        System.out.print("CANTIDAD A ENVIAR [0-" + balance + "]: ");
        float amount = MyInput.readFloat();
        if ((amount <=0) || (amount>balance)) {
            System.out.println("[!] Importe no válido. Envío abortado.");
            pause();
            return;
        }
        
        // Crear transacción
        Transaction transaction = new Transaction(userCredentials.getPublic(),
            toKey, amount, new Date());
        transaction.sign(userCredentials.getPrivate());
        
        // Guardar y propagar
        if (cutrecoin.addPendingTransaction(transaction)) {
            cm.propagate(transaction);
        }
        
        // Mostrar mensaje
        System.out.println("[i] Transacción creada con éxito");
        pause();
    }
    
    
    /**
     * Menú "Mis transacciones"
     */
    public static void menuMisTransacciones() {
        // Mostrar datos del usuario
        PublicKey key = userCredentials.getPublic();
        String address = Utils.keyToBase64(key);
        address = Utils.addLineBreaks(address, 46);
        float balance = cutrecoin.getBalance(key);
        System.out.println(
            "================ TU DIRECCIÓN ================\n" +
            address + "\n" +
            "==============================================\n" +
            "TU SALDO: " + balance + " CC\n");
        
        // Mostrar listado de últimas transacciones
        for (Transaction t : cutrecoin.getCustomTransactions(key)) {
            System.out.println(t.getTimestamp() + " - " + t.getAmount() + " CC");
        }
        
        // Pausar
        pause();
    }
    
    
    /**
     * Menú "Minar bloques"
     */
    public static void menuMinarBloques() {
        System.out.println("==== MINAR BLOQUES ====");
        
        // Crear bloque potencial
        Block lastBlock = cutrecoin.getLastBlock();
        long blockIndex = (lastBlock == null) ? 0 : lastBlock.getIndex() + 1;
        String prevHash = (lastBlock == null) ? null : lastBlock.getHash();
        ArrayList<Transaction> transactions = cutrecoin.getPendingTransactions();
        Block block = new Block(blockIndex, transactions, prevHash,
            userCredentials.getPublic(), Cutrecoin.MAX_FEE);
        
        // Minar bloque
        System.out.println("Minando bloque #" + blockIndex + " . . .");
        block.mine();
        
        // Guardar y propagar bloque
        cutrecoin.addBlock(block);
        cm.propagate(block);
        System.out.println("¡Bloque minado! Nonce: " + block.getNonce());
        pause();
    }
    
    
    /**
     * Menú "Ver estado"
     */
    public static void menuVerEstado() {
        Block lastBlock = cutrecoin.getLastBlock();
        long blockNum = (lastBlock == null) ? 0 : lastBlock.getIndex() + 1;
        int pendingNum = cutrecoin.getPendingTransactions().size();
        System.out.println("=== ESTADO DE LA RED ===\n" +
            "Nodos conectados: " + cm.getNodeCount() + "\n" +
            "Sincronizado con la red: " + (cm.isSynced() ? "Sí" : "No") + "\n" +
            "Número de bloques: " + blockNum + "\n" +
            "Número de transacciones pendientes: " + pendingNum + "\n" +
            "Tapa de mercado: " + cutrecoin.getMarketCap() + " CC");
        pause();
    }
    

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // Obtener o crear credenciales del usuario
        Object creds = DataManager.readSerialized(DataManager.CREDENTIALS_PATH);
        if (creds == null) {
            userCredentials = Utils.generateKeyPair();
            DataManager.saveSerialized(DataManager.CREDENTIALS_PATH, userCredentials);
        } else {
            userCredentials = (KeyPair) creds;
        }
        
        // Instanciar Cutrecoin
        cutrecoin = new Cutrecoin();
        
        // Instanciar ConnectionManager
        System.out.println("Conectándose a la red de Cutrecoin . . .");
        cm = new ConnectionManager(cutrecoin);
        
        // Mostrar menú por pantalla
        clear();
        int key;
        do {
            System.out.print("1. Nueva transacción\n" +
                "2. Mis transacciones\n" +
                "3. Minar bloques\n" +
                "4. Ver estado\n" +
                "0. Salir del programa\n" +
                "|> ");
            key = MyInput.readInt();
            clear();
            if (key == 1) {
                menuNuevaTransaccion();
            } else if (key == 2) {
                menuMisTransacciones();
            } else if (key == 3) {
                menuMinarBloques();
            } else if (key == 4) {
                menuVerEstado();
            } else if (key != 0) {
                System.out.println("Opción no válida");
                pause();
            }
        } while (key != 0);
        
        // Salir del programa
        System.out.println("[i] Cerrando conexiones activas...");
        cm.kill();
        System.out.println("[i] Cutrecoin se ha cerrado. ¡Hasta luego!");
    }

}
