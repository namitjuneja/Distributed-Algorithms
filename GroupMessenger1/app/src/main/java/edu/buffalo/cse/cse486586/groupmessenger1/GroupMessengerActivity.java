package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.util.Log;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import android.net.Uri;

public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;
    static int seq = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        Log.i(TAG, "onCreate called");
        try
        {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
            Log.i(TAG, "Server Socket created successfully");
        }
        catch (Exception e)
        {
            Log.i(TAG, "Server Socket creation error.");
            Log.e(TAG, e.getMessage(), e.getCause());
            return;
        }

        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        final EditText editText = (EditText) findViewById(R.id.editText1);

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create 5 sockets and send message to them
                Log.i(TAG, "OnClick begin.");
                Log.i(TAG, "myPort: "+myPort);

                String msg = editText.getText().toString() + "\n";
                editText.setText("");

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void>
    {
        @Override
        protected Void doInBackground(ServerSocket... sockets)
        {
            ServerSocket serverSocket = sockets[0];

            try
            {
                while (true)
                {
                    // Create Socket Connection
                    Socket server_socket = serverSocket.accept();

                    // Create Input Stream
                    InputStreamReader server_input_stream_reader = new InputStreamReader(server_socket.getInputStream());
                    BufferedReader server_input_stream = new BufferedReader(server_input_stream_reader);

                    // Create Output Stream
                    PrintWriter server_output_stream = new PrintWriter(server_socket.getOutputStream(), true);

                    //Read message from input stream
                    String message = null;
                    if ((message=server_input_stream.readLine()) != null)
                    {
                        // Take message and call onProgressUpdate to fill up the text view
                        publishProgress(message);
                        Log.i(TAG, "Message Received: " + message);

                        // Send Acknowledgement
                        server_output_stream.println("ACK");
                        Log.i(TAG, "Acknowledgement Sent.");

                        Log.i(TAG, "Closing server input and output streams...");
                        // Close Input Stream
                        server_input_stream_reader.close();
                        server_input_stream.close();
                        // Close Output Stream
                        server_output_stream.close();
                        // Close the Socket
                        server_socket.close();
                        Log.i(TAG, "Server streams closed.");
                    }
                }
            }
            catch (Exception e)
            {
                Log.e(TAG, "ServerTask Socket error.");
                Log.e(TAG, "EXCEPTION: \n"+e);
            }

            return null;
        }

        protected void onProgressUpdate(String... strings)
        {
            String string_received = strings[0];
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(string_received + "\t\n");

            ContentResolver content_resolver = getContentResolver();
            Uri uri = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger1.provider");
            ContentValues keyValueToInsert = new ContentValues();
            keyValueToInsert.put("key", Integer.toString(seq));
            keyValueToInsert.put("value", string_received);

            content_resolver.insert(uri, keyValueToInsert);
            seq += 1;


            Log.i(TAG, "ServerTask progressed(received) this message: ");
            Log.i(TAG, strings[0]);
        }

        private Uri buildUri(String scheme, String authority) {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority(authority);
            uriBuilder.scheme(scheme);
            return uriBuilder.build();
        }

    }

    private class ClientTask extends AsyncTask<String, Void, Void>
    {
        @Override
        protected Void doInBackground(String... msgs)
        {
            Log.i(TAG, msgs[0]);
            Log.i(TAG, msgs[1]);
            try
            {
                String port_list[] = {REMOTE_PORT0,
                                      REMOTE_PORT1,
                                      REMOTE_PORT2,
                                      REMOTE_PORT3,
                                      REMOTE_PORT4};

                for (String port: port_list)
                {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(port));

                    String message = msgs[0];

                    // Create Output Stream
                    PrintWriter client_output_stream = new PrintWriter(socket.getOutputStream(), true);

                    // Create Input Stream
                    InputStreamReader client_input_stream_reader = new InputStreamReader(socket.getInputStream());
                    BufferedReader client_input_stream = new BufferedReader(client_input_stream_reader);

                    // Send Message
                    client_output_stream.println(message);
                    Log.i(TAG, "Message Sent: " + message);

                    // Close both the streams and the socket once ACK is received.
                    try
                    {
                        while (!socket.isClosed())
                        {
                            String ack_msg = null;

                            if ((ack_msg=client_input_stream.readLine()) != null)
                            {
                                Log.i(TAG, "Acknowledgment received from server.");
                                Log.i(TAG, "Closing client input and output streams...");
                                // Close Output Stream
                                client_output_stream.close();
                                // Close Input Stream
                                client_input_stream_reader.close();
                                client_input_stream.close();
                                // Close the Socket
                                socket.close();
                                Log.i(TAG, "Client streams closed.");
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        Log.e(TAG, "ClientTask socket error.");
                        Log.e(TAG, "EXCEPTION: \n"+e);
                    }
                }
            }
            catch (Exception e)
            {
                Log.i(TAG, "Client task failed.");
                Log.e(TAG, e.getMessage(), e.getCause());

            }

            return null;
        }
    }
}
