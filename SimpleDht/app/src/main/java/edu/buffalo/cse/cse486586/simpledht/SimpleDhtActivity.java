package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.os.StrictMode;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class SimpleDhtActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_dht_main);

        StrictMode.ThreadPolicy tp = StrictMode.ThreadPolicy.LAX;
        StrictMode.setThreadPolicy(tp);

        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.button3).setOnClickListener(
                new OnTestClickListener(tv, getContentResolver()));
        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                ContentResolver content_resolver = getContentResolver();
                Uri uri = Uri.parse("content://edu.buffalo.cse.cse486586.simpledht.provider");
                ContentValues keyValueToInsert = new ContentValues();

                keyValueToInsert.put("key"  , "raj");
                keyValueToInsert.put("value", "singh");

                content_resolver.insert(uri, keyValueToInsert);

                ContentValues keyValueToInsert2 = new ContentValues();

                keyValueToInsert2.put("key"  , "shakti");
                keyValueToInsert2.put("value", "kapoor");

                content_resolver.insert(uri, keyValueToInsert2);
            }
        });
        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                ContentResolver content_resolver = getContentResolver();
                Uri uri = Uri.parse("content://edu.buffalo.cse.cse486586.simpledht.provider");
                content_resolver.delete(uri, "@", null);
                //Cursor cursor = content_resolver.query(uri, null, "*", null, null);
                //Log.i("XXX", "ACTIVITY// "+cursor.getCount());
                
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_simple_dht_main, menu);
        return true;
    }

}
