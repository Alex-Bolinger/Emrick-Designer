package org.emrick.project;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GetHandler implements HttpHandler {
    private String pkt;
    private RequestCompleteListener requestCompleteListener;
    public GetHandler(String pkt, RequestCompleteListener requestCompleteListener) {
        this.pkt = pkt;
        this.requestCompleteListener = requestCompleteListener;
    }
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, Object> parameters = new HashMap<String, Object>();
        URI requestedUri = exchange.getRequestURI();
        String query = requestedUri.getRawQuery();
        if (query == null) {
            String running = "Server is running";
            exchange.sendResponseHeaders(200, running.length());
            OutputStream os = exchange.getResponseBody();
            os.write(running.getBytes());
            os.flush();
            os.close();
            return;
        }
        System.out.println(query);
        parseQuery(query, parameters);
        String response = "";


        File f = new File(pkt + parameters.get("id"));
        BufferedReader bfr = new BufferedReader(new FileReader(f));
        String line = bfr.readLine();
        while (line != null) {
            response += line + "\n";
            line = bfr.readLine();
        }
        bfr.close();

        exchange.sendResponseHeaders(200,response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.flush();
        os.close();
        requestCompleteListener.onRequestComplete(Integer.parseInt((String) parameters.get("id")));
    }

    public void parseQuery(String query, Map<String, Object> parameters) {
        String[] paramsAndValues = query.split("&");
        for (String p : paramsAndValues) {
            String key = p.split("=")[0];
            String value = p.split("=")[1];
            parameters.put(key,value);
        }
    }
}
