/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unisa.p2p.chat;

import it.unisa.p2p.gui.ConsoleChat;
import it.unisa.p2p.gui.MainFrame;
import java.io.File;
import org.apache.log4j.xml.DOMConfigurator;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 *
 * @author raffaeledragone
 */
public class StartAnonymousChat {
    
    public static final String ROOTPATH= new File("").getAbsolutePath();
    
    @Option(name="-m", aliases="--masterip", usage="the master peer ip address", required=true)
    private static String master;

    @Option(name="-id", aliases="--identifierpeer", usage="the unique identifier for this peer", required=true)
    private static int id;
    
    @Option(name="-showgui", aliases="--showgui", usage="the unique identifier for this peer", required=false)
    private static String showGui;
    
    public static void main(String[] args) throws Exception {
        StartAnonymousChat chat=new StartAnonymousChat();
        final CmdLineParser parser = new CmdLineParser(chat);
        parser.parseArgument(args);
        DOMConfigurator.configure("log_structure");
        if(showGui!=null && showGui.equalsIgnoreCase("yes")){
            MainFrame gui=new MainFrame(master,id);
            gui.setVisible(true);
        }else{
            ConsoleChat console=new ConsoleChat(master, id);
        }
    }
    
    
}
