package uk.me.desert_island.theorbtwo.bridge;

import android.app.Activity;
import android.widget.Toast;
import android.content.Intent;
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

    @Override
    public void onStart() {
        super.onStart();
        Intent service_intent = new Intent(this, JavaBridgeService.class);
        startService(service_intent);
        Toast.makeText(this, "Started JavaBridgeService", Toast.LENGTH_SHORT).show();
    }
}
