/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unisa.p2p.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author raffaeledragone
 */
public class UtilDate {
    
    public static String formattaOra(Date d) {
      DateFormat df = new SimpleDateFormat("HH:mm");
      return df.format(d);
   }
    
    public static String formattaData(Calendar d) {
      DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
      return df.format(d.getTime());
   }
    
    
    
    public static String formattaMillisecondi(Long mills) {
      int seconds = (int) (mills / 1000);
      
      DateFormat df = new SimpleDateFormat("HH:mm");
      return df.format(new Date().getTime());
   }
    public static String formatSecondsIn_sTime(Long secs) {
        long hours = secs / 3600;
        long minutes = (secs % 3600) / 60;
        long seconds = secs % 60;
        
        return hours+":"+minutes+":"+seconds;
   }
    
   public static long differenceDateInSeconds(Date d1, Date d2){
       return (d2.getTime() - d1.getTime()) / 1000;
   }
    
    
    
}
