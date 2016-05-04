package com.ksam.client;



import sun.misc.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.*;

/**
 * Created by jdownes on 4/10/2016.
 *
 * pushes records up to server
 *
 * HttpURLConnection connection = (HttpURLConnection)url.openConnection();
 connection.setRequestMethod("POST");
 connection.setRequestProperty("Content-Length", "" + sb.length());

 */

public class RecordPusher {
    public static final String WKT="wkt";
    public static final String SOURCE_ID="sourceId";
    public static final String RECORD_ID="recordId";
    public static final String CONTENTS="contents";
    String charset = "UTF-8";
    private String url;
    public RecordPusher(String url){
        this.url = url;
    }
    public  boolean push(Placemark p){


        System.out.println("Pushing record:");
        System.out.println("contents="+p.getContents());
        System.out.println("wkt="+p.getWkt());
        System.out.println("id="+p.getId());
        System.out.println("client id ="+SystemProperties.getInstance().getClientId());

        try {

            String query = String.format(WKT+"=%s&"+SOURCE_ID+"=%s&"+RECORD_ID+"=%s&"+CONTENTS+"=%s",
                    URLEncoder.encode(p.getWkt(), charset),
                    URLEncoder.encode(SystemProperties.getInstance().getClientId(), charset),
                    URLEncoder.encode(p.getId(), charset),
                    URLEncoder.encode(p.getContents(), charset));
            URL u = new URL(url+"/submit?"+query);
            System.out.println(url+"/submit?"+query);
            //System.out.println("Submitting to "+u.getHost());
            HttpURLConnection connection = (HttpURLConnection)u.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("charset", "utf-8");
            //connection.connect();

           /* InputStream is =connection.getInputStream();



            String theString = convertStreamToString(is);
            boolean success = Boolean.parseBoolean(theString);
            connection.disconnect();*/
            System.out.println("response from server "+connection.getResponseMessage());
            return true;


        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        } catch (ProtocolException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }
    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
