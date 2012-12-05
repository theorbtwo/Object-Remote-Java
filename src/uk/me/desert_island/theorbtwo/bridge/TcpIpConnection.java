package uk.me.desert_island.theorbtwo.bridge;

import uk.me.desert_island.theorbtwo.bridge.Core;
import java.net.*;
import java.io.*;
import org.json.JSONException;

public class TcpIpConnection {

    private Socket incoming_socket;
    private PrintyThing err_log;
    private Core core;
    public TcpIpConnection(Socket incoming, PrintyThing pt) {
        incoming_socket = incoming;
        err_log = pt;
    }

    public void start() {
        try {
            InputStream in_stream = incoming_socket.getInputStream();
            PrintStream out_stream = new PrintStream(incoming_socket.getOutputStream());
            core = new Core(out_stream, err_log);

            out_stream.print("Shere\n");
            // handle until connection closed
            Boolean stop = false;
            while (incoming_socket.isConnected() && !stop) {
                if (this.handleInput(in_stream, out_stream)) {
                    err_log.print("Handled input line");
                } else {
                    stop = true;
                }
            }
            incoming_socket.close();
            err_log.print("Socket disconnected, now closed\n");
        } catch (IOException e) {
            err_log.print("TcpIpConnection: Can't get input/output streams from incoming socket");
        }
    }

    public boolean handleInput(InputStream in_stream, PrintStream out_stream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in_stream));
        String line = null;
        try {
            line = reader.readLine();
            if (line != null) {
                err_log.print("Read line from client:" + line); 
                try {
                    core.handle_line(new StringBuilder(line));
                } catch(Exception e) {
                    err_log.print("TcpIpConnection: Input was not parseable as JSON "+ line + "(or who knows what else) " + e);
                    
                    // Ugly, but should work.  FIXME: make err_log be a subclass of PrintStream in it's own right?
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos, true);
                    e.printStackTrace(ps);
                    err_log.print(baos.toString());
                }
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            err_log.print("TcpIpConnection: No lines to read from incoming socket");
        }
        return false;
    }

    /*
     * This should call start.. or something. Right now it complains: error: non-static variable core cannot be referenced from a static context
     * [javac]                     core.handle_line(in_line);


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
            
            // catch (java.io.IOException e) {
            //   System.err.println("IOException!");
            //   System.exit(1);
                // usless, but keeps javac from giving a might-be-used-uninit error
            //   c = -1;
            //    }

            if (c == 10) {
                // newline.
                //System.err.printf("Got a line: '%s'\n", in_line);
              
                try {
                    core.handle_line(in_line);
                } catch(Exception e) {
                    System.err.println("handle_line failed" + e);
                }
              
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
  */    
}
