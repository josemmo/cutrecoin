/*
 * Copyright 2017 José Miguel Moreno
 * josemiguel.moreno@alumnos.uva.es
 */

package tk.josemmo.cutrecoin;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import tk.josemmo.DataManager;

/**
 * Cutrecoin
 * 
 * Esta clase define un `blockchain` (cadena de bloques) que recibe el nombre
 * de Cutrecoin (CC).
 * 
 * @author josemmo
 * @author carlos
 */
public class Cutrecoin {
    
    // Número de ceros por los que tiene que empezar el hash de un bloque
    public static final int DIFFICULTY = 3;
    // Algoritmo con el que se firmarán las transacciones
    public static final String SIGNATURE_ALG = "SHA1WithRSA";
    // Número de CC máximo que un minero puede exigir por bloque minado
    public static final float MAX_FEE = 10;
    // Puerto de conexiones para nodos de la red
    public static final int PORT = 10000;
    // Millisegundos de espera antes de dar una conexión por no accesible
    public static final int TIMEOUT = 150;
    
    private ArrayList<Block> chain = new ArrayList<>();
    private ArrayList<Transaction> pendingTransactions = new ArrayList<>();
    private ArrayList<Block> candidateBlocks = new ArrayList<>();
    
    
    /**
     * Cutrecoin
     */
    public Cutrecoin() {
        // Cargar blockchain a la clase
        Object tmpChain = DataManager.readSerialized(DataManager.BLOCKCHAIN_PATH);
        if (tmpChain != null) chain = (ArrayList<Block>) tmpChain;
        
        // Cargar transacciones pendientes
        Object tmpPending = DataManager.readSerialized(DataManager.PENDING_PATH);
        if (tmpPending != null) {
            pendingTransactions = (ArrayList<Transaction>) tmpPending;
        }
    }
    
    
    
    /**
     * Save
     */
    private void save() {
        DataManager.saveSerialized(DataManager.BLOCKCHAIN_PATH, chain);
        DataManager.saveSerialized(DataManager.PENDING_PATH, pendingTransactions);
    }
    
    
    /**
     * Get last block
     * @return block
     */
    public Block getLastBlock() {
        if (chain.isEmpty()) return null;
        return chain.get(chain.size()-1);
    }
    
    
    /**
     * Add block
     * 
     * Añade un bloque al final del `blockchain` siempre y cuando sea válido
     * 
     * @param block
     * @return success
     */
    public boolean addBlock(Block block) {
        Block lastBlock = getLastBlock();
        
        // Validar integridad de la cadena
        if (lastBlock == null) {
            if (block.getPreviousHash() != null) return false;
        } else if (!lastBlock.getHash().equals(block.getPreviousHash())) {
            return false;
        }
        
        // Validar índice correlativo
        if (lastBlock == null) {
            if (block.getIndex() != 0) return false;
        } else if ((lastBlock.getIndex()+1) != block.getIndex()) {
            return false;
        }
        
        // Validar comisión del bloque
        float fee = block.getFee();
        if ((fee < 0) || (fee > MAX_FEE)) return false;
        if (block.getMiner() == null) return false;
        
        // Validar dificultad de bloque
        if (!block.isValidHash()) return false;
        
        // Validar transacciones del bloque
        for (Transaction t : block.getTransactions()) {
            if (!t.isValidSignature()) return false;
        }
        
        // Añadir bloque a la cadena
        chain.add(block);
        
        // Guardar en disco
        save();
        
        return true;
    }
    
    
    /**
     * Add candidate block
     * @param block
     * @return success
     */
    public boolean addCandidateBlock(Block block) {
        // Comprobar que el bloque es válido
        if (!block.isValidHash()) return false;
        Block lastBlock = getLastBlock();
        if (
            (lastBlock != null) &&
            (lastBlock.getIndex() > block.getIndex())
        ) return false;
        
        // Añadir bloque a la cola
        for (Block b : candidateBlocks) {
            if (b.getHash().equals(block.getHash())) return true;
        }
        candidateBlocks.add(block);
        
        // Procesar cola
        boolean finished;
        do {
            finished = true;
            for (Block b : candidateBlocks) {
                boolean success = addBlock(b);
                if (success) finished = false;
            }
        } while (!finished);
        
        return true;
    }
    
    
    /**
     * Get balance
     * 
     * Dada una clave pública, devuelve su saldo actual
     * 
     * @param key
     * @return balance
     */
    public float getBalance(PublicKey key) {
        String targetAddr = (key == null) ? null : Utils.keyToBase64(key);
        float balance = 0;
        for (Block block : chain) {
            // Añadir saldo de transacciones
            if (targetAddr != null) {
                for (Transaction transaction : block.getTransactions()) {
                    String fromAddr = Utils.keyToBase64(transaction.getFrom());
                    String toAddr = Utils.keyToBase64(transaction.getTo());
                    if (targetAddr.equals(fromAddr)) {
                        balance -= transaction.getAmount();
                    } else if (targetAddr.equals(toAddr)) {
                        balance += transaction.getAmount();
                    }
                }
            }
            
            // Añadir saldo de comisiones
            if (targetAddr == null) {
                balance += block.getFee();
            } else {
                String minerAddr = Utils.keyToBase64(block.getMiner());
                if (targetAddr.equals(minerAddr)) balance += block.getFee();
            }
        }
        return balance;
    }
    
    
    /**
     * Get market cap
     * 
     * Devuelve el número total de CC existentes.
     * 
     * @return 
     */
    public float getMarketCap() {
        return getBalance(null);
    }
    
    
    /**
     * Get block
     * @param hash
     * @return block
     */
    public Block getBlock(String hash) {
        for (Block block : chain) {
            if (block.getHash().equals(hash)) return block;
        }
        return null;
    }
    
    
    /**
     * Get block from transaction
     * 
     * Devuelve el bloque que contiene una transacción o null si no existe.
     * 
     * @param t
     * @return block
     */
    public Block getBlockFromTransaction(Transaction t) {
        for (Block block : chain) {
            for (Transaction t2 : block.getTransactions()) {
                if (t.equals(t2)) return block;
            }
        }
        return null;
    }
    
    
    /**
     * Get pending transactions
     * @return pendingTransactions
     */
    public ArrayList<Transaction> getPendingTransactions() {
        return pendingTransactions;
    }
    
    
    /**
     * Add pending transaction
     * @param transaction 
     * @return success
     */
    public boolean addPendingTransaction(Transaction transaction) {
        // Comprobar que la transacción es válida
        if (transaction.isValidSignature()) return false;
        
        // Buscar si ya existe en la cola
        for (Transaction t2 : pendingTransactions) {
            if (transaction.equals(t2)) return false;
        }
        
        // Buscar si la transacción ya existe en algún bloque
        if (getBlockFromTransaction(transaction) != null) return false;
        
        // Añadir transacción a la cola
        pendingTransactions.add(transaction);
        return true;
    }
    
    
    /**
     * Get custom transactions
     * @param key
     * @return 
     */
    public ArrayList<Transaction> getCustomTransactions(PublicKey key) {
        String targetAddr = Utils.keyToBase64(key);
        ArrayList<Transaction> queue = new ArrayList<>();
        for (Block block : chain) {
            for (Transaction t : block.getTransactions()) {
                String fromAddr = Utils.keyToBase64(t.getFrom());
                String toAddr = Utils.keyToBase64(t.getTo());
                if (fromAddr.equals(targetAddr) || toAddr.equals(targetAddr)) {
                    queue.add(t);
                }
            }
        }
        Collections.reverse(queue);
        return queue;
    }
    
}
