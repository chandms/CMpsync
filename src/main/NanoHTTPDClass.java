package main;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class NanoHTTPDClass {
    static int port=8745;
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        System.out.println("Server starts at "+port+"!!");
        server.createContext("/test", new newHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "This is the response";

            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

    }

    static public void zipFile(String srcFile, String destZipFile)
            throws Exception {
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(destZipFile));
        File folder = new File(srcFile);
        byte[] buf = new byte[1024];
        int len;
        FileInputStream in = new FileInputStream(srcFile);
        zip.putNextEntry(new ZipEntry(folder.getName()));
        while ((len = in.read(buf)) > 0) {
            zip.write(buf, 0, len);
        }
        zip.close();
    }

    static public void zipFolder(String srcFolder, String destZipFile)
            throws Exception {
        ZipOutputStream zip = null;
        FileOutputStream fileWriter = null;
        fileWriter = new FileOutputStream(destZipFile);
        zip = new ZipOutputStream(fileWriter);
        addFolderToZip("", srcFolder, zip);
        zip.flush();
        zip.close();
    }

    static private void addFileToZip(String path, String srcFile,
                                     ZipOutputStream zip) throws Exception {
        File folder = new File(srcFile);
        if (folder.isDirectory()) {
            addFolderToZip(path, srcFile, zip);
        } else {
            byte[] buf = new byte[1024];
            int len;
            FileInputStream in = new FileInputStream(srcFile);
            zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
            while ((len = in.read(buf)) > 0) {
                zip.write(buf, 0, len);
            }
        }
    }

    static private void addFolderToZip(String path, String srcFolder,
                                       ZipOutputStream zip) throws Exception {
        File folder = new File(srcFolder);
        for (String fileName : folder.list()) {
            if (path.equals("")) {
                addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip);
            } else {
                addFileToZip(path + "/" + folder.getName(), srcFolder + "/"
                        + fileName, zip);
            }
        }
    }

    static class GetHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {


            Headers h = t.getResponseHeaders();
            h.add("Content-Type", "application/jpg");

            File file = new File ("C:/Users/Pupul/Desktop/CM picture.jpg");
            byte [] bytearray  = new byte [(int)file.length()];
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            bis.read(bytearray, 0, bytearray.length);

            t.sendResponseHeaders(200, file.length());

            OutputStream os = t.getResponseBody();
            os.write(bytearray,0,bytearray.length);

            os.close();
        }
    }

    static class newHandler implements HttpHandler{
        public void handle(HttpExchange t)throws IOException{
            String zipFile="C:/Users/Pupul/Desktop/folder";
            File f = new File(zipFile);
            String zipFileName = "C:/Users/Pupul/Desktop/"+f.getName() + ".zip";
            if (f.isDirectory()) {
                try {
                    zipFolder(zipFile, zipFileName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    zipFile(zipFile, zipFileName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println(zipFileName + " has been generated at "
                    + new File("").getAbsolutePath());
            URL url = new File(zipFileName).toURI().toURL();
            System.out.println("Chandrika Url= "+ url);
                    Headers h = t.getResponseHeaders();
                    h.add("Content-Type", "application/zip");

                    // a PDF (you provide your own!)
                    File file = new File(zipFileName);
                    byte [] bytearray  = new byte [(int)file.length()];
                    FileInputStream fis = new FileInputStream(file);
                    BufferedInputStream bis = new BufferedInputStream(fis);
                    bis.read(bytearray, 0, bytearray.length);

                    // ok, we are ready to send the response.



                    t.sendResponseHeaders(200, file.length());

                    OutputStream os = t.getResponseBody();
                    os.write(bytearray,0,bytearray.length);

                    os.close();

                }
            }
        }



