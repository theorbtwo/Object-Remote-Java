package uk.me.desert_island.theorbtwo.bridge;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

public class JavaBridgeActivity extends Activity
{
    private TcpIpConnection my_connection;
    private final String LOGTAG = "JavaBridgeActivity";

    /* Callbacks we can set to run when events on the activity itself happen */
    private HashMap event_callbacks = new HashMap();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        final Activity this_activity = this;
        AndroidServiceStash.set_activity(this_activity);

        Toast.makeText(this_activity, "Starting server ... ", Toast.LENGTH_SHORT).show();
        if(my_connection != null) {
            Toast.makeText(this_activity, "TCP-IP server already running", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new Thread(new Runnable() {
                public void run() {
                    Looper.prepare();

                    // Would prefer that this not be 0.0.0.0 !
                    InetAddress bind_address;
                    try {
                        bind_address = Inet4Address.getByName("0.0.0.0");
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
                }
            }).start();
    }

    public void setEventCallbacks(HashMap new_callbacks) {
        event_callbacks = new_callbacks;
    }

    /* Called when the device's configuration changes, such as the
     * user rotating the device or docking/undocking from an external
     * keyboard. */
    @Override public void onConfigurationChanged(Configuration new_config) {
        // Somehow, this should get the perl side to execute some
        // function, passing it new_config.
        Log.w(LOGTAG, "onConfigurationChanged happened!");
        if (event_callbacks.get("on_configuration_changed") != null) {
            ((Runnable)event_callbacks.get("on_configuration_changed")).run();
        }
        super.onConfigurationChanged(new_config);
    }

    /* Called when an Intent/other activity we start returns
     */
    @Override  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.w(LOGTAG, "onActivityResult happened!");
        if(event_callbacks.get("on_activity_result") != null) {
            ((PassthroughRunnable)event_callbacks.get("on_activity_result")).run_extended(requestCode, resultCode, data);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void set_view_components(Runnable callback) {
        callback.run();
    }

    
    @Override
        public void onStart() {
        super.onStart();
    }
}
