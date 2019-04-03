package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class GroupMessengerProvider extends ContentProvider {
    private String TAG = "GroupMessengerActivity";

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String file_name    = (String) values.get("key");
        String file_content = (String) values.get("value");

        FileOutputStream output_stream;

        try
        {
            output_stream = getContext().openFileOutput(file_name, Context.MODE_PRIVATE);
            output_stream.write(file_content.getBytes());
            output_stream.close();
        }
        catch (Exception e)
        {
            Log.e(TAG, "Content insertion error." + e);
            Log.e(TAG, e.getMessage(), e.getCause());
        }

        Log.v(TAG, "INSERTED: " + values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        FileInputStream input_stream;
        String file_name = selection; // also the key
        String[] default_projection = {"key", "value"};

        try
        {
            // Read value from file
            input_stream = getContext().openFileInput(file_name);
            String file_value = new BufferedReader(new InputStreamReader(input_stream)).readLine();
            input_stream.close();

            // Create a cursor which will be returned
            MatrixCursor matrix_cursor = new MatrixCursor(default_projection);
            String[] row_data = {file_name, file_value};
            matrix_cursor.addRow(row_data);
            return matrix_cursor;
        }
        catch (Exception e)
        {
            Log.e(TAG, "Content query error.");
            Log.e(TAG, "File Name: "+file_name);
            Log.e(TAG, e.getMessage(), e.getCause());
            return null;
        }
    }
}
