/*
 * Copyright 2017 José Miguel Moreno
 * josemiguel.moreno@alumnos.uva.es
 */

package tk.josemmo.cutrecoin;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;

/**
 * Block
 * 
 * Define un bloque del `blockchain`. Contiene transacciones.
 * 
 * @author josemmo
 * @author carlos
 */
public class Block implements Serializable {
    
    private final long index;
    private final ArrayList<Transaction> transactions;
    private long nonce = 0;
    private String hash;
    private final String previous_hash;
    private final PublicKey miner;
    private final float fee;
    
    
    /**
     * Block
     * @param index
     * @param transactions
     * @param previous_hash 
     * @param miner 
     * @param fee 
     */
    public Block(long index, ArrayList<Transaction> transactions,
                 String previous_hash, PublicKey miner, float fee) {
        this.index = index;
        this.transactions = transactions;
        this.previous_hash = previous_hash;
        this.miner = miner;
        this.fee = fee;
        recalculateHash();
    }
    
    
    /**
     * Get index
     * @return index
     */
    public long getIndex() {
        return index;
    }
    
    
    /**
     * Get transactions
     * @return transactions
     */
    public ArrayList<Transaction> getTransactions() {
        return transactions;
    }
    
    
    /**
     * Get hash
     * @return hash
     */
    public String getHash() {
        return hash;
    }
    
    
    /**
     * Get previous hash
     * @return previousHash
     */
    public String getPreviousHash() {
        return previous_hash;
    }
    
    
    /**
     * Get miner
     * @return miner
     */
    public PublicKey getMiner() {
        return miner;
    }
    
    
    /**
     * Get fee
     * @return fee
     */
    public float getFee() {
        return fee;
    }
    
    
    /**
     * Get nonce
     * @return nonce
     */
    public long getNonce() {
        return nonce;
    }
    
    
    /**
     * Recalculate hash
     * 
     * Calcula el atributo hash del bloque en función de su información
     */
    public void recalculateHash() {
        String signature = index + transactions.toString() + nonce +
            ((previous_hash == null) ? "null" : previous_hash) + miner + fee;
        hash = Utils.sha256(signature);
    }
    
    
    /**
     * Increment nonce
     * 
     * Suma una unidad al nonce del bloque
     */
    public void incrementNonce() {
        nonce++;
        recalculateHash();
    }
    
    
    /**
     * Is valid hash
     * 
     * Comprueba que el hash de este bloque cumple la dificultad de la cadena.
     * 
     * @return isValid
     */
    public boolean isValidHash() {
        String validStr = String.format("%0" + Cutrecoin.DIFFICULTY + "d", 0);
        String blockStr = getHash().substring(0, Cutrecoin.DIFFICULTY);
        return validStr.equals(blockStr);
    }
    
    
    /**
     * Mine
     * 
     * Busca el `nonce` que cumpla la dificultad del hash del bloque.
     */
    public void mine() {
        while (!isValidHash()) incrementNonce();
    }
    
}
