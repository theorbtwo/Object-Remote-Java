package uk.me.desert_island.theorbtwo.bridge;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.os.Binder;
import android.widget.Toast;
import android.util.Log;
import java.net.*;
import java.io.*;
import uk.me.desert_island.theorbtwo.bridge.TcpIpConnection;
import uk.me.desert_island.theorbtwo.bridge.PrintyThingAndroid;

// This should probably be an IntentService (see http://developer.android.com/guide/components/services.html)
// Then we would need to store our ids and suchlike outside of the
// memory (sqlitedb?) so that the individual handlers can continue from
// each other.

public class JavaBridgeService extends IntentService {
    private TcpIpConnection my_connection;
    private static final String LOGTAG = "JavaBridgeService";
    // It would probably be handy if Core were a proper object then we
    // can create one here and re-use it for new tcpconnections
    // private Core my_jb_core;

    public JavaBridgeService() {
        super("JavaBridgeService");
        AndroidServiceStash.set_service(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
      Toast.makeText(this, "Starting server ... ", Toast.LENGTH_SHORT).show();
      if(my_connection != null) {
          Toast.makeText(this, "TCP-IP server already running", Toast.LENGTH_SHORT).show();
          return;
      }

      // Would prefer that this not be 0.0.0.0 !
      InetAddress bind_address;
      try {
          //          bind_address = InetAddress.getLocalHost();
          bind_address = InetAddress.getByName("0.0.0.0");
      } catch (UnknownHostException e) {
          Log.e(LOGTAG, "Failed to find InetAddress 0.0.0.0");
          return;
      }

      ServerSocket server_socket;
      try {
          server_socket = new ServerSocket(9849, 1, bind_address);
      } catch(IOException e) {
          Log.e(LOGTAG, "Failed to accept create server socket on 9849");
          return;
      }
      
      /* FIXME: Where is the proper androidy place to put this mainloop? */
      while (true) {
        try {
            Socket connected_socket = server_socket.accept();
            my_connection = new TcpIpConnection(connected_socket, new PrintyThingAndroid("JavaBridgeService"));
            Log.d(LOGTAG, "Accepted new connection and started TcpIpConnection");
            my_connection.start();
        } catch (IOException e) {
            Log.e(LOGTAG, "Failed to accept socket connection!");
        }
      }
      
      //      return START_STICKY;
    }
    
    // I really don't understand this binder stuff.  This is taken directly from http://developer.android.com/reference/android/app/Service.html#LocalServiceSample
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        JavaBridgeService getService() {
            return JavaBridgeService.this;
        }
    }


    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind (Intent intent) {
        return mBinder;
    }
}
