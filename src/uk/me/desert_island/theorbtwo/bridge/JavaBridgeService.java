package uk.me.desert_island.theorbtwo.bridge;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Binder;
import android.widget.Toast;
import java.net.*;
import uk.me.desert_island.theorbtwo.bridge.TcpIpConnection;

public class JavaBridgeService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
      InetAddress bind_address = InetAddress.getByName("0.0.0.0");
      ServerSocket server_socket = new ServerSocket(9849, 1, bind_address);
      
      /* FIXME: Where is the proper androidy place to put this mainloop? */
      while (true) {
        Socket connected_socket = server_socket.accept();
        
        new TcpIpConnection(connected_socket).start();
      }
      
      return START_STICKY;
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
