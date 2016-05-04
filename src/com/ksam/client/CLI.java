package com.ksam.client;

/**
 * Created by jdownes on 4/28/2016.
 */
public class CLI {

    public static void main(String[] args){
        //load props- file dir/poll time
        System.out.println("launching cli");
        SystemProperties props = SystemProperties.getInstance();
        if(props == null){
            System.out.println("Could not load system properties, exiting");
            System.exit(1);
        }
        //start file scanner
        FileScanner fs = new FileScanner();
        fs.start();

    }
}
