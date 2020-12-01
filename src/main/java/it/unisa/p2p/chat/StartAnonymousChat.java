/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unisa.p2p.chat;

import it.unisa.p2p.gui.MainFrame;
import org.apache.log4j.xml.DOMConfigurator;
import org.kohsuke.args4j.Option;

/**
 *
 * @author raffaeledragone
 */
public class StartAnonymousChat {
    @Option(name="-m", aliases="--masterip", usage="the master peer ip address", required=true)
    private static String master;

    @Option(name="-id", aliases="--identifierpeer", usage="the unique identifier for this peer", required=true)
    private static int id;
    
    //@Option(name="-showgui", aliases="--showgui", usage="the unique identifier for this peer", required=false)
    //private static String showGui;
    
    public static void main(String[] args) throws Exception {
        //if(showGui!=null && showGui.equalsIgnoreCase("yes")){
            DOMConfigurator.configure("log_structure");
            MainFrame gui=new MainFrame(master,id);
            gui.setVisible(true);
        //}
    }
    
    
}
