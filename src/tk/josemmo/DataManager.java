/*
 * Copyright 2017 José Miguel Moreno
 * josemiguel.moreno@alumnos.uva.es
 */

package tk.josemmo;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Data Manager
 * 
 * @author josemmo
 * @author carlos
 */
public class DataManager {
    
    public static final String BLOCKCHAIN_PATH = "blockchain.dat";
    public static final String PENDING_PATH = "pending.dat";
    public static final String CREDENTIALS_PATH = "credentials.dat";
    
    
    /**
     * Save serialized
     * @param path
     * @param object
     * @return success
     */
    public static boolean saveSerialized(String path, Object object) {
        try {
            FileOutputStream fileOut = new FileOutputStream(path);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(object);
            out.close();
            fileOut.close();
            return true;
        } catch (Exception i) {
            return false;
        }
    }
    
    
    /**
     * Read serialized
     * @param path
     * @return object
     */
    public static Object readSerialized(String path) {
        try {
            FileInputStream fileIn = new FileInputStream(path);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            Object res = in.readObject();
            in.close();
            fileIn.close();
            return res;
        } catch (Exception i) {
            return null;
        }
    }
    
}
