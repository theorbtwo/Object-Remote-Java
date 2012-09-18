package uk.me.desert_island.theorbtwo.bridge;

import uk.me.desert_island.theorbtwo.bridge.Core;
import java.net.*;
import java.io.*;

public class NormalTCP {
    public static void main(String[] args)
        throws java.io.IOException, org.json.JSONException
    {
        ServerSocket listen_sock = new ServerSocket(4242);
        System.err.print("Listening\n");
        Socket connected_sock = listen_sock.accept();
        System.err.print("Connected\n");
        InputStream in_stream = connected_sock.getInputStream();
        PrintStream out_stream = new PrintStream(connected_sock.getOutputStream());
        
        out_stream.print("Shere\n");

        java.lang.StringBuilder in_line = new StringBuilder();
        while (true) {
            int c;
          
            System.err.print("Reading\n");
            c = in_stream.read();
            
            /* catch (java.io.IOException e) {
                System.err.println("IOException!");
                System.exit(1);
                // usless, but keeps javac from giving a might-be-used-uninit error
                c = -1;
                }*/

            if (c == 10) {
                // newline.
                //System.err.printf("Got a line: '%s'\n", in_line);
              
                Core.handle_line(in_line, out_stream, System.err);
              
                in_line = new StringBuilder();
            } else if (c == -1) {
                System.err.printf("Got -1 from read, shutting down.\n");
              
                System.exit(3);
            } else {
                in_line.appendCodePoint(c);
                System.err.printf("Read succeeded, buffer: <<%s>>\n", in_line);
            }
        }
    }
    
}
