package uk.me.desert_island.theorbtwo.bridge;

import android.app.Activity;
import android.widget.Toast;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;
import android.os.Handler;
import android.os.Messenger;
import android.os.Message;
import android.os.RemoteException;
import android.os.Bundle;

public class JavaBridgeActivity extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    // Listen to messages from the service
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage (Message msg) {
            System.err.println("(Activity)Message time: " + msg.getWhen() + "\n(Activity)Message content: " + msg.obj.toString());
        }
    }
    Messenger activityMessenger = new Messenger(new IncomingHandler());
    Messenger serviceMessenger = null;
    boolean isBound;
    
    private ServiceConnection sConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                // This is called when the connection with the service has been
                // established, giving us the object we can use to
                // interact with the service.  We are communicating with the
                // service using a Messenger, so here we get a client-side
                // representation of that from the raw IBinder object.
                serviceMessenger = new Messenger(service);
                Message message = Message.obtain(null, JavaBridgeService.MSG_SETUP);
                message.replyTo = activityMessenger;
                try {
                    serviceMessenger.send(message);
                }
                catch (RemoteException e) {
                    e.printStackTrace();
                }

                isBound = true;
            }

            public void onServiceDisconnected(ComponentName className) {
                // This is called when the connection with the service has been
                // unexpectedly disconnected -- that is, its process crashed.
                serviceMessenger = null;
                isBound = false;
            }
        };

    @Override
    public void onStart() {
        super.onStart();
        Intent service_intent = new Intent(this, JavaBridgeService.class);
        bindService(service_intent, sConnection, Context.BIND_AUTO_CREATE);
        
        //        AndroidServiceStash.set_activity(this);

        //        startService(service_intent);
        Toast.makeText(this, "Started JavaBridgeService", Toast.LENGTH_SHORT).show();
    }

}
