package com.ksam.client;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jdownes on 4/10/2016.
 */
public class FileScanner extends Thread{

    //scan kml directory for changes

    //inspect for changes to publish records or for changes to queries
    //C:\Users\jdownes\AppData\LocalLow\Google\GoogleEarth\myplaces.kml

    //needs to load hashes of currently seen records
    // seen.idx file
    private SystemProperties props= SystemProperties.getInstance();
    private String myPlacesHash;
    private  Map<String, String> recordsHashes;
    private Map<String, String> workflowHashes;
    private File recordsHashDir= props.getRecordDir();;
    private File workflowsHashDir= props.getOperationsDir();
    public void run(){
        System.out.println("Filescanner started");
        long rate = props.getPollRate();
        File myPlaces = new File(props.getScanDir(),"myplaces.kml");
        if(myPlaces == null || !myPlaces.exists()){
            System.out.println("Filscanner could not find myplaces.kml, in "+myPlaces.getAbsolutePath());
            return;
        }
        //load myplaces hash if exists;
        File myPlacesHashFile = new File(props.getRecordDir(),"myplaces.hash");
        if(myPlacesHashFile.exists()){
            myPlacesHash = readFile(myPlacesHashFile);
        }

        //load records hashes
        recordsHashes = new HashMap<>();

        if(recordsHashDir!=null && recordsHashDir.exists()){
            File[] files = recordsHashDir.listFiles();
            for(File file : files){
                String filename = file.getName();
                if(filename.endsWith(".idx")){
                    recordsHashes.put(filename, hashFile(file));
                }
            }
        }
        //load workflows hashes
        workflowHashes = new HashMap<>();

        if(workflowsHashDir!=null && workflowsHashDir.exists()){
            File[] files = workflowsHashDir.listFiles();
            for(File file : files){
                String filename = file.getName();
                if(filename.endsWith(".idx")){
                    workflowHashes.put(filename, hashFile(file));
                }
            }
        }
        while(true){
            //open my places kml
            //get hash
            String contents = readFile(myPlaces);
            if(contents == null) return;
            String currentHash = hashString(contents);
            //check hash
            if(myPlacesHash == null || !myPlacesHash.equals(currentHash)){
                //process changes
                processFile(contents);

                myPlacesHash = currentHash;
                writeFile(myPlacesHashFile, myPlacesHash);
            }

            try {
                Thread.sleep(rate);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void writeFile(File file ,String contents){
        try {
            PrintWriter writer = new PrintWriter(file);
            writer.write(contents);
            writer.flush();
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
    private void processFile(String contents){
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();

            Document doc = db.parse(new InputSource(new ByteArrayInputStream(contents.getBytes("utf-8"))));
            //get records;
            NodeList recordsList = doc.getElementsByTagName("Folder");
            for(int i = 0;i<recordsList.getLength();i++){
                Node record = recordsList.item(i);
                NodeList children = record.getChildNodes();
                boolean isCorrectFolder =false;
                for(int j=0;j<children.getLength();j++){
                    Node possName = children.item(j);
                    if(possName.getNodeName().equals("name")){
                        if(possName.getChildNodes().item(0).getNodeValue().equals("Records")){
                            isCorrectFolder= true;
                        }
                    }
                }
                if(isCorrectFolder) {
                    for (int j = 0; j < children.getLength(); j++) {
                        Node possPlacemark = children.item(j);
                        if (possPlacemark.getNodeName().equals("Placemark")) {
                            //turn to string and check hash map
                            String placemarkName = null;

                            NodeList placemarkChildren = possPlacemark.getChildNodes();
                            for (int k = 0; k < placemarkChildren.getLength(); k++) {
                                Node placemarkChild = placemarkChildren.item(k);
                                if (placemarkChild.getNodeName().equals("name")) {
                                    placemarkName = placemarkChild.getChildNodes().item(0).getNodeValue();

                                }
                            }
                            System.out.println("PlacemarkName = " + placemarkName);
                            //check hashmap
                            if (placemarkName != null) {
                                placemarkName = placemarkName + ".idx";
                                //calculate hash of placemark
                                String placemarkHash = hashString(nodeToString(possPlacemark));
                                boolean writeToFile = false;
                                if (recordsHashes.containsKey(placemarkName)) {
                                    if (!placemarkHash.equals(recordsHashes.get(placemarkName))) {
                                        System.out.println("Hash difference found ");
                                        writeToFile = true;

                                    }
                                } else {
                                    System.out.println("hash miss found");
                                    writeToFile = true;

                                }
                                if (writeToFile) {
                                    boolean success = pushUpRecord(possPlacemark);
                                    if(success) {
                                        //push hash in map
                                        recordsHashes.put(placemarkName, placemarkHash);
                                        //write hash to file
                                        writeFile(new File(recordsHashDir, placemarkName), placemarkHash);
                                    }

                                }
                            }
                        }
                    }
                }
            }
            //get workflows
            NodeList workflowsList = doc.getElementsByTagName("Folder");
            for(int i = 0;i<workflowsList.getLength();i++){
                Node workflow = workflowsList.item(i);
                NodeList children = workflow.getChildNodes();
                boolean isCorrectFolder =false;
                for(int j=0;j<children.getLength();j++){
                    Node possName = children.item(j);
                    if(possName.getNodeName().equals("name")){
                        if(possName.getChildNodes().item(0).getNodeValue().equals("Workflows")){
                            isCorrectFolder= true;
                        }
                    }
                }
                if(isCorrectFolder) {
                    for (int j = 0; j < children.getLength(); j++) {
                        Node possWorkflow = children.item(j);
                        if (possWorkflow.getNodeName().equals("Folder")) {
                            //turn to string and check hash map
                            String workflowName = null;

                            NodeList workflowChildren = possWorkflow.getChildNodes();
                            for (int k = 0; k < workflowChildren.getLength(); k++) {
                                Node workflowChild = workflowChildren.item(k);
                                if (workflowChild.getNodeName().equals("name")) {
                                    workflowName = workflowChild.getChildNodes().item(0).getNodeValue();

                                }
                            }
                            System.out.println("workflowname = " + workflowName);
                            //check hashmap
                            if (workflowName != null) {
                                workflowName = workflowName + ".idx";
                                //calculate hash of placemark
                                String workflowHash = hashString(nodeToString(possWorkflow));
                                boolean writeToFile = false;
                                if (workflowHashes.containsKey(workflowName)) {
                                    if (!workflowHash.equals(workflowHashes.get(workflowName))) {
                                        System.out.println("Hash difference found");
                                        writeToFile = true;

                                    }
                                } else {
                                    System.out.println("hash miss found");
                                    writeToFile = true;

                                }
                                if (writeToFile) {

                                    boolean success =pushUpWorkflow(possWorkflow);
                                    if(success) {
                                        //push hash in map
                                        workflowHashes.put(workflowName, workflowHash);
                                        //write hash to file
                                        writeFile(new File(workflowsHashDir, workflowName), workflowHash);
                                    }
                                }
                            }
                        }
                    }
                }
            }


        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
    private boolean pushUpRecord(Node record){
        RecordPusher pusher = new RecordPusher(props.getServerUrl());

        //parse out placemarks
        Placemark p = nodeToPlacemark(record);
        return pusher.push(p);
    }
    private boolean pushUpWorkflow(Node workflow){
        OperationPusher op = new OperationPusher(props.getServerUrl());
        String wfScript="";
        String opName="";
        List<Placemark> placemarks = new ArrayList<>();
        NodeList children = workflow.getChildNodes();
        for(int i=0;i<children.getLength();i++){
            Node child = children.item(i);
            //parse workflow syntax
            if(child.getNodeName().equals("description")){
                wfScript = child.getChildNodes().item(0).getNodeValue();
            }else if(child.getNodeName().equals("Placemark")){//parse out placemarks if exists
                placemarks.add(nodeToPlacemark(child));
            }else if(child.getNodeName().equals("name")){
                opName = child.getChildNodes().item(0).getNodeValue();
            }


        }

        return op.push(opName, wfScript, placemarks);
    }
    private Placemark nodeToPlacemark(Node n){
        Placemark p = new Placemark();
        //search geometry
        NodeList children = n.getChildNodes();
        for(int i =0;i<children.getLength();i++){
            Node child = children.item(i);
            //get geometry
            if(child.getNodeName().equals("Point")){
                List<double []> coords = extractCoords(child);
                if(coords!=null&& coords.size()>0){
                    double[] c= coords.get(0);
                    String wkt = "POINT("+c[0]+" "+c[1]+")";
                    p.setWkt(wkt);
                }
            }else if(child.getNodeName().equals("LineString")){
                List<double []> coords = extractCoords(child);
                if(coords!=null){
                    String wkt="LINESTRING(";
                    for(double[] c: coords){
                        wkt+=c[0]+" "+c[1]+",";
                    }
                    if(wkt.endsWith(",")){
                        wkt = wkt.substring(0, wkt.length()-1);
                    }
                    wkt+=")";
                    p.setWkt(wkt);
                }
            }else if(child.getNodeName().equals("Polygon")){
                //TODO polygon parse
            }
            //get contents
            if(child.getNodeName().equals("description")){
                p.setContents(child.getChildNodes().item(0).getNodeValue());
            }
            //get name
            if(child.getNodeName().equals("name")){
                p.setId(child.getChildNodes().item(0).getNodeValue());
            }
            //get times



        }

        return p;
    }
    private List<double[]> extractCoords(Node n){
        NodeList children = n.getChildNodes();
        List<double[]> coords = new ArrayList<>();
        for(int i =0;i<children.getLength();i++){
            Node child = children.item(i);
            if(child.getNodeName().equals("coordinates")){
                String rawCoords = child.getChildNodes().item(0).getNodeValue();
                String[] splitCoords = rawCoords.split("\\s+");
                for(int j =0; j<splitCoords.length;j++){
                    //System.out.println("Split coord="+splitCoords[j]);
                    if(!splitCoords[j].isEmpty()) {
                        String[] indCoords = splitCoords[j].split(",");
                        double[]c = new double[3];
                        c[0] = Double.parseDouble(indCoords[0]);
                        c[1] = Double.parseDouble(indCoords[1]);
                        c[2] = Double.parseDouble(indCoords[2]);
                        coords.add(c);
                    }
                }
                return coords;
            }
        }
        return null;
    }


    private static String convertByteArrayToHexString(byte[] arrayBytes) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < arrayBytes.length; i++) {
            stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16)
                    .substring(1));
            }
        return stringBuffer.toString();
    }
    private static String hashFile(File file){

        String contents = readFile(file);
        if(contents == null )return null;
        return hashString(contents);

    }
    private static String hashString(String message){
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hashedBytes = digest.digest(message.getBytes("UTF-8"));

            return convertByteArrayToHexString(hashedBytes);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            ex.printStackTrace();
            return null;
        }
    }
    private static String readFile(File file)

    {
        byte[] encoded = new byte[0];
        try {
            encoded = Files.readAllBytes(file.toPath());
        } catch (IOException e) {

            e.printStackTrace();
            return null;
        }
        return new String(encoded, StandardCharsets.UTF_8);
    }
    private static String nodeToString(Node node) {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException te) {
            System.out.println("nodeToString Transformer Exception");
        }
        return sw.toString();
    }
    public static void main(String [] args){
        FileScanner fs = new FileScanner();
        fs.start();
    }

}
