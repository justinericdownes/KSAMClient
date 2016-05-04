package com.ksam.client;

import java.io.*;
import java.util.Properties;
import java.util.UUID;

/**
 * Created by jdownes on 4/10/2016.
 */
public class SystemProperties {
    //location for storing files - records and queries
    //needs to load these from a prop file that is relative from something
    private static SystemProperties sp = new SystemProperties();
    private static final String propFile ="./config.properties";
    public static final String SCAN_DIR="scanDir";
    public static final String POLL_RATE="pollRate";
    public static final String WRITE_DIR="writeDir";
    public static final String SERVER_URL="server";
    public static final String CLIENT_ID="clientID";
    public static SystemProperties getInstance(){
        return sp;
    }
    private Properties props;
    private SystemProperties(){
        //read from prop file and load info
        props = new Properties();
        try {
            props.load(new FileInputStream(propFile));
        } catch (IOException e) {
            e.printStackTrace();
            props = null;
        }
    }
    private File recordDir;
    public File getRecordDir(){
        if(recordDir==null){
            recordDir = new File("./recordsDir");
            if(!recordDir.exists()){
                recordDir.mkdir();
            }
        }
        return recordDir;
    }
    private File operationsDir;
    public File getOperationsDir(){
        if(operationsDir==null){
            operationsDir = new File("./operationsDir");
            if(!operationsDir.exists()){
                operationsDir.mkdir();
            }
        }
        return operationsDir;
    }
    private File scanDir;
    public File getScanDir(){
        if(scanDir == null){
            scanDir = new File(props.getProperty(SCAN_DIR));
        }
        return scanDir;
    }
    public long getPollRate(){
        if(props.contains(POLL_RATE)){
            return Integer.parseInt(props.getProperty(POLL_RATE))*1000;
        }else{
            return 5000;
        }

    }
    private String serverURL;
    public String getServerUrl(){
        if(serverURL == null){
            serverURL = props.get(SERVER_URL).toString();
        }
        return serverURL;
    }

    private String clientId=null;
    public String getClientId(){
        if(clientId==null){
            if(props.containsKey(CLIENT_ID)){
                clientId = props.get(CLIENT_ID).toString();
            }else{
                clientId = UUID.randomUUID().toString();
                props.setProperty(CLIENT_ID, clientId);
                try {
                    props.store(new FileOutputStream(propFile),null);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return  clientId;
    }


}
