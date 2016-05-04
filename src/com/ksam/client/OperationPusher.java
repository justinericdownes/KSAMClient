package com.ksam.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.List;

/**
 * Created by jdownes on 4/10/2016.
 */
public class OperationPusher {
    public static final String CLIENT_ID="clientId";
    public static final String OPERATION_ID="opId";
    public static final String START_WKTS="startWKTs";
    public static final String QUERY_STRING="queryString";
    String charset = "UTF-8";

    private String url;
    public OperationPusher(String url){
        this.url = url;
    }
    public boolean push(String name, String op, List<Placemark> ps){
        System.out.println("Pushing operation: "+op);
        System.out.println("Placemark geometries:");
        String wkts="";

        for(Placemark p : ps){
            System.out.println(" wkt="+p.getWkt());
            wkts+=p.getWkt()+";";
        }
        String query = null;
        try {
            query = String.format(CLIENT_ID+"=%s&"+OPERATION_ID+"=%s&"+START_WKTS+"=%s&"+QUERY_STRING+"=%s",
                    URLEncoder.encode(SystemProperties.getInstance().getClientId(), charset),
                    URLEncoder.encode(name, charset),
                    URLEncoder.encode(wkts, charset),
                    URLEncoder.encode(op, charset));
            URL u = new URL(url+"/query?"+query);
            System.out.println(url+"/query?"+query);
            //System.out.println("Submitting to "+u.getHost());
            HttpURLConnection connection = (HttpURLConnection)u.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("charset", "utf-8");
            System.out.println("response from server "+connection.getResponseMessage());
        } catch (UnsupportedEncodingException e) {

            e.printStackTrace();
            return false;
        } catch (ProtocolException e) {
            e.printStackTrace();
            return false;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
