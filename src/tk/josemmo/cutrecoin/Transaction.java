/*
 * Copyright 2017 José Miguel Moreno
 * josemiguel.moreno@alumnos.uva.es
 */

package tk.josemmo.cutrecoin;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;

/**
 * Transaction
 * 
 * Representa una transacción u operación monetaria.
 * 
 * @author josemmo
 * @author carlos
 */
public class Transaction implements Serializable {
    
    private final PublicKey from;
    private final PublicKey to;
    private final float amount;
    private final Date timestamp;
    private byte[] signature = null;
    
    
    /**
     * Transaction
     * @param from
     * @param to
     * @param amount
     * @param timestamp
     * @param signature 
     */
    public Transaction(PublicKey from, PublicKey to, float amount,
                       Date timestamp, byte[] signature) {
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.timestamp = timestamp;
        this.signature = signature;
    }
    
    
    /**
     * Transaction
     * @param from
     * @param to
     * @param amount
     * @param timestamp 
     */
    public Transaction(PublicKey from, PublicKey to, float amount, Date timestamp) {
        this(from, to, amount, timestamp, null);
    }
    
    
    /**
     * Get data to sign
     * @return signedBytes
     */
    private byte[] getDataToSign() {
        String data = Utils.keyToBase64(from) + Utils.keyToBase64(to) + amount +
            ";" + timestamp.getTime();
        return data.getBytes();
    }
    
    
    /**
     * Private key
     * @param pk
     * @return success
     */
    public boolean sign(PrivateKey pk) {
        // Comprobar que no está firmada la transacción
        if (signature != null) return false;
        
        // Validar que la clave privada se corresponde con esta transacción
        String expectedPublicKey = Utils.keyToBase64(Utils.getPublicKeyFromPrivate(pk));
        String transactionPublicKey = Utils.keyToBase64(from);
        if (!expectedPublicKey.equals(transactionPublicKey)) return false;
        
        // Intentar firmar la transacción
        try {
            Signature sig = Signature.getInstance(Cutrecoin.SIGNATURE_ALG);
            sig.initSign(pk);
            sig.update(getDataToSign());
            signature = sig.sign();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    
    /**
     * Is valid signature
     * 
     * Comprueba que la firma de la transacción es válida
     * 
     * @return isValid
     */
    public boolean isValidSignature() {
        if (signature == null) return false;
        try {
            Signature sig = Signature.getInstance(Cutrecoin.SIGNATURE_ALG);
            sig.initVerify(from);
            sig.update(getDataToSign());
            return sig.verify(signature);
        } catch (Exception e) {
            return false;
        }
    }
    
    
    /**
     * To string
     * 
     * Devuelve una representación de la transacción en String
     * 
     * @return string
     */
    @Override
    public String toString() {
        return Base64.getEncoder().encodeToString(getDataToSign());
    }
    
    
    
    /**
     * Equals
     * @param b
     * @return isEqual
     */
    @Override
    public boolean equals(Object b) {
        if (!(b instanceof Transaction)) return false;
        return toString().equals(b.toString());
    }
    
    
    /**
     * Get from
     * @return from
     */
    public PublicKey getFrom() {
        return from;
    }
    
    
    /**
     * Get to
     * @return to
     */
    public PublicKey getTo() {
        return to;
    }
    
    
    /**
     * Get amount
     * @return amount
     */
    public float getAmount() {
        return amount;
    }
    
    
    /**
     * Get timestamp
     * @return timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }
    
}
