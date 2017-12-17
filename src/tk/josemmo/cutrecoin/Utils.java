/*
 * Copyright 2017 José Miguel Moreno
 * josemiguel.moreno@alumnos.uva.es
 */

package tk.josemmo.cutrecoin;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Utils
 * 
 * Clase con métodos estáticos para ser reutilizados.
 * 
 * @author josemmo
 * @author carlos
 */
public class Utils {

    /**
     * Digest
     * @param algo
     * @param input
     * @return hash
     */
    public static String digest(String algo, String input) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance(algo);
            sha256.update(input.getBytes("UTF-8"));
            byte[] digest = sha256.digest();
            StringBuffer sb = new StringBuffer();
            for (int i=0;i<digest.length; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
    
    
    /**
     * SHA-256
     * @param input
     * @return hash
     */
    public static String sha256(String input) {
        return digest("SHA-256", input);
    }
    
    
    /**
     * Generate key pair
     * 
     * Genera un par clave privada y clave pública, siendo ésta última la
     * dirección de pagos del usuario.
     * 
     * @return keyPair
     */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            return kpg.genKeyPair();
        } catch (Exception e) {
            return null;
        }
    }
    
    
    /**
     * Key to Base64
     * 
     * Representa una clave en un string en Base64
     * 
     * @param key
     * @return base64
     */
    public static String keyToBase64(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
    
    
    /**
     * Base64 to key
     * @param b64
     * @return key
     */
    public static PublicKey base64ToKey(String b64) {
        try {
            byte[] publicBytes = Base64.getDecoder().decode(b64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            return null;
        }
    }
    
    
    /**
     * Get public key from private key
     * 
     * Dada una clave privada, obtiene su clave pública
     * @param pk
     * @return publicKey
     */
    public static PublicKey getPublicKeyFromPrivate(PrivateKey pk) {
        RSAPrivateCrtKey pvt = (RSAPrivateCrtKey) pk;
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(pvt.getModulus(),
            pvt.getPublicExponent());
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
            return publicKey;
        } catch (Exception e) {
            return null;
        } 
    }
    
    
    /**
     * Add line breaks
     * @param input
     * @param length
     * @return output
     */
    public static String addLineBreaks(String input, int length) {
       String[] lines = input.split("(?<=\\G.{" + length + "})");
       return String.join("\r\n", lines);
    }
    
}
