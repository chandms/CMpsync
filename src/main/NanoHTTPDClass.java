package main;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import 


public class NanoHTTPDClass {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/test", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "This is the response";
            PrintWriter printWriter = new PrintWriter(new File("C:/Users/Pupul/Desktop/out.txt"));
            t.sendResponseHeaders(200, response.length());

            DiskFileItemFactory d = new DiskFileItemFactory();

            try {
                ServletFileUpload up = new ServletFileUpload(d);
                List<FileItem> result = up.parseRequest(new RequestContext() {

                    @Override
                    public String getCharacterEncoding() {
                        return "UTF-8";
                    }

                    @Override
                    public int getContentLength() {
                        return 0; //tested to work with 0 as return
                    }

                    @Override
                    public String getContentType() {
                        return t.getRequestHeaders().getFirst("Content-type");
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        return t.getRequestBody();
                    }

                });
                t.getResponseHeaders().add("Content-type", "text/plain");
                t.sendResponseHeaders(200, 0);
                OutputStream os = t.getResponseBody();
                for(FileItem fi : result) {
                    os.write(fi.getName().getBytes());
                    os.write("\r\n".getBytes());
                    System.out.println("File-Item: " + fi.getFieldName() + " = " + fi.getName());
                }
                os.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

    }

}
