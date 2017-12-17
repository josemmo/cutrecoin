package tk.josemmo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

/**
 * This class consists exclusively of static methods
 * that handle device data inputted by keyboard
 */
public class MyInput {
    
   /**
    * Read String
    * @return Keyboard input as a String
    */
   public static String readString() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in), 1);
        String string = "";
        try {
            string = br.readLine();
        } catch (IOException ex) {}
        return string;
   }
   

   /**
    * Read Integer
    * @return Keyboard input as an Int
    */
   public static int readInt() {
      int toReturn = 0;
      try {
          toReturn = Integer.parseInt(readString());
      } catch (NumberFormatException e) {}
      return toReturn;
   }
   

   /**
    * Read Double
    * @return Keyboard input as a Double
    */
   public static double readDouble() {
      return Double.parseDouble(readString());
   }
   

   /**
    * Read Byte
    * @return Keyboard input as a Byte
    */
   public static byte readByte() {
      return Byte.parseByte(readString());
   }
   

   /**
    * Read Short
    * @return Keyboard input as a Short
    */
   public static short readShort() {
      return Short.parseShort(readString());
   }

   /**
    * Read Long
    * @return Keyboard input as a Long
    */
   public static long readLong() {
      return Long.parseLong(readString());
   }
   

   /**
    * Read Float
    * @return Keyboard input as a Float
    */
   public static float readFloat() {
      return Float.parseFloat(readString());
   }
   

   /**
    * Read Character
    * @return Keyboard input as a Char
    */
   public static char readChar() {
      return readString().charAt(0);
   }
   
   
   /**
    * Read File
    * @param file
    * @return File input as a String
    */
   public static String readFile(File file) {
       String output = "";
       try {
        FileInputStream in = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) out.append(line).append("\n");
        output = out.toString();
        reader.close();
       } catch (Exception e) {}
       return output;
   }
   
   
   /**
    * Read File
    * @param path
    * @return File input as a String
    */
   public static String readFile(String path) {
       return readFile(new File(path));
   }
   
   
   /**
    * Read File
    * @param uri
    * @return File input as a String
    */
   public static String readFile(URI uri) {
       return readFile(new File(uri));
   }

}