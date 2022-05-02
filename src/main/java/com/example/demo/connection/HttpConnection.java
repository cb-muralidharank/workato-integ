package com.example.demo.connection;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpConnection {
    public static final String HTTP_STATUS_CODE = "httpStatusCode";
    public static final String HTTP_RESPONSE = "httpResponse";
    public static final String SUCCESS = "success";
    private final HttpURLConnection connection;

    public HttpConnection(String endpointUrl, String method,String params) throws IOException {
        connection = (HttpURLConnection) new URL(endpointUrl).openConnection();
        connection.setRequestMethod(method);
        connection.setUseCaches(false);
        setAdminAuthCred();
        if(params!=null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type","application/json");
            try (OutputStream os = connection.getOutputStream()) {
                os.write(params.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    public JSONObject getResponse() throws IOException, JSONException {
        String content = "Error on processing the response";
        JSONObject response = new JSONObject();
        response.put(SUCCESS,false);
        response.put(HTTP_STATUS_CODE, connection.getResponseCode());
        try (InputStream resp = (connection.getResponseCode()>=200 && connection.getResponseCode()<400)?connection.getInputStream():connection.getErrorStream()) {
            if(resp!=null){
                response.put(SUCCESS,true);
                InputStreamReader inp = new InputStreamReader(resp);
                StringBuilder buf = new StringBuilder();
                char[] buffer = new char[1024];
                int bytesRead;
                while ((bytesRead = inp.read(buffer, 0, buffer.length)) >= 0) {
                    buf.append(buffer, 0, bytesRead);
                }
                content = buf.toString();
                inp.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        response.put(HTTP_RESPONSE, content);
        connection.disconnect();
        return response;
    }

    private void setAdminAuthCred() {
        connection.setRequestProperty("x-user-email", "workato@chargebee.com");
        connection.setRequestProperty("x-user-token", "410e91ce-9906-4408-b577-e332b0524190");
    }
}
