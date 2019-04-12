package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
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

                keyValueToInsert.put("key"  , "5562");
                keyValueToInsert.put("value", "singh");

                content_resolver.insert(uri, keyValueToInsert);
            }
        });
        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                ContentResolver content_resolver = getContentResolver();
                Uri uri = Uri.parse("content://edu.buffalo.cse.cse486586.simpledht.provider");
                Cursor cursor = content_resolver.query(uri, null, "5562", null, null);
                MatrixCursor matrix_cursor = (MatrixCursor) cursor;
                Log.i("XXX", "ACTIVITY// "+matrix_cursor.getCount());
                //if (cursor.getCount() > 0) {
                //    do {
                //        String returnKey = cursor.getString(keyIndex);
                //        String returnValue = cursor.getString(valueIndex);
                //        currentKeyValPairs = currentKeyValPairs + returnKey + ":" + returnValue + ":";
                //        //Log.i("getKeyValFromCursor", currentKeyValPairs);
                //    } while (matrixCursor.moveToNext());
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
