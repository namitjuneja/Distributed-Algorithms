package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;
    static int seq = 0;
    static int message_id = 0;
    static String my_port = "";
    static HashMap<String, ArrayList<Float>> proposed_ids = new HashMap<String, ArrayList<Float>>();
    static HashMap<Integer, String> message_id_reference = new HashMap<Integer, String>();
    static HashMap<Float, Integer> float_to_message_id   = new HashMap<Float, Integer>();
    static ArrayList<Float> float_sorter = new ArrayList<Float>();
    static ArrayList<Float> final_message_id_list = new ArrayList<Float>();
    static HashMap<Float, String> final_id_to_string = new HashMap<Float, String>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        Log.i(TAG, "onCreate called");

        // Create Socket Server
        try
        {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
            Log.i(TAG, "Server Socket created successfully");
        }
        catch (Exception e)
        {
            Log.i(TAG, "Server Socket creation error.");
            Log.d(TAG, "Server Socket Port: "+SERVER_PORT);
            Log.e(TAG, e.getMessage(), e.getCause());
            return;
        }
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * Send the message from the input box (EditText) and send it to other AVDs
         * when the "Send" button is pressed.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        my_port = String.valueOf((Integer.parseInt(portStr) * 2));

        final EditText editText = (EditText) findViewById(R.id.editText1);

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create 5 sockets and send message to them
                Log.i(TAG, "Send button clicked.");

                // Get message from the text area
                String msg = editText.getText().toString();

                // reset the text area
                editText.setText("");

                // Send message to all AVDs
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
                        msg, // message content
                        my_port,      // AVD's port number
                        "False",      // is_deliverable
                        Integer.toString(message_id));

                // create an empty array for the message for storing proposed sequence numbers
                // from other AVDs
                proposed_ids.put(Integer.toString(message_id), new ArrayList<Float>());

                // Store a reference for the local message ID and its content.
                // Used when finally sending data to the content provider.
                message_id_reference.put(message_id, msg);

                // Increment local message ID
                message_id += 1;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
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

                    server_socket.setSoTimeout(500);

                    // Create Input Stream
                    InputStreamReader server_input_stream_reader = new InputStreamReader(server_socket.getInputStream());
                    BufferedReader server_input_stream = new BufferedReader(server_input_stream_reader);

                    // Create Output Stream
                    PrintWriter server_output_stream = new PrintWriter(server_socket.getOutputStream(), true);

                    // Read message from input stream
                    String server_string = null;
                    if ((server_string=server_input_stream.readLine()) != null)
                    {
                        // Disintegrate the message string into its components
                        String server_is_deliverable = server_string.split(":")[0];
                        String server_message        = server_string.split(":")[1];
                        String server_message_id     = server_string.split(":")[2];

                        Log.i(TAG, "SERVER// Received the following message.");
                        Log.i(TAG, "SERVER// Deliverable: "+server_is_deliverable);
                        Log.i(TAG, "SERVER// Message: "+server_message);
                        Log.i(TAG, "SERVER// Message ID: "+server_message_id);

                        // Take action based on the value of the deliverable
                        // If deliverable == True
                        //     - It is a broadcast telling the final value of the message
                        //     - Insert and Publish the message_id<->message as key<->value
                        //     - Send a customary acknowledgement with is_deliverable = True
                        // Else if deliverable = False
                        //     - It is a message asking for a proposed sequence value
                        //     - Increment the current sequence value
                        //     - Send this sequence value as proposed value in acknowledgement
                        //     (append .<port_number> to help settle ties)

                        String server_ack_message = null;

                        if (server_is_deliverable.equals("True"))
                        {
                            publishProgress(server_message, server_message_id);
                            // Message String Format - Deliverable : xxx : xxx : xxx
                            // x's are just space holders because that is how the client expects the
                            // data to be sent
                            server_ack_message = server_is_deliverable + ":xxx:xxx:xxx";
                            Log.i(TAG, "SERVER//ACK// Acknowledgement being sent is");
                            Log.i(TAG, "SERVER//ACK// Deliverable: "+server_is_deliverable);
                            Log.i(TAG, "SERVER//ACK// Message: ");
                            Log.i(TAG, "SERVER//ACK// Message ID: ");
                            Log.i(TAG, "SERVER//ACK// Proposed ID: ");
                        }
                        else if (server_is_deliverable.equals("False"))
                        {

                            String server_proposed_id = Integer.toString(seq);
                            // Message String Format - Deliverable : Message : Message ID : Proposed ID
                            server_ack_message = server_is_deliverable + ":" + server_message + ":" + server_message_id + ":" + server_proposed_id + "." + my_port;
                            seq += 1;
                            Log.i(TAG, "SERVER//ACK// Acknowledgement being sent is");
                            Log.i(TAG, "SERVER//ACK// Deliverable: "+server_is_deliverable);
                            Log.i(TAG, "SERVER//ACK// Message: "+server_message);
                            Log.i(TAG, "SERVER//ACK// Message ID: "+server_message_id);
                            Log.i(TAG, "SERVER//ACK// Proposed ID: "+server_proposed_id);
                        }

                        // Send Acknowledgement
                        server_output_stream.println(server_ack_message);
                        Log.i(TAG, "SERVER// ==SENT== - " + server_ack_message);

                        // Close all the streams
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
            catch (NullPointerException e)
            {
                Log.e(TAG, "FAIL FAIL SERVER FAIL");
                Log.e(TAG, e.getMessage(), e.getCause());
                Log.getStackTraceString(e);
            }
            catch (Exception e)
            {
                Log.e(TAG, "ServerTask Socket error.");
                Log.e(TAG, e.getMessage(), e.getCause());
                Log.getStackTraceString(e);
            }

            return null;
        }

        protected void onProgressUpdate(String... strings)
        {
            /*
             * Append the given message to the display.
             * Insert the message into the sorted list of messages, assign the messages keys 0 to n,
             * then insert them into the file based storage using the content provider.
             */
            String string_received = strings[0];
            Float message_final_id = Float.parseFloat(strings[1]);

            // Logic used to delete messages sent with <5 proposed sequences
            // when a new proposed sequence arrives for that message.
            // This is useful when one one or more nodes fail.
            float delete_key = Float.parseFloat("0.0");
            boolean to_delete = false;

            for (Map.Entry<Float, String> item : final_id_to_string.entrySet()) {
                float key = item.getKey();
                String value = item.getValue();
                Log.i(TAG, "CONTENT//MODERN// " + value + " | " +  string_received + " | " +  value.equals(string_received));
                if (value.equals(string_received))
                {
                    Log.i(TAG, "CONTENT//MODERN// Duplicate String");
                    // This is a repeat case
                    // Delete this key value pair
                    // Delete this key, and the new key from final_message_id_list
                    delete_key = key;
                    to_delete = true;

                    final_message_id_list.remove(key);
                    if (final_message_id_list.contains(message_final_id))
                    {
                        final_message_id_list.remove(message_final_id);
                    }
                }
            }

            if (to_delete)
            {
                final_id_to_string.remove(delete_key);
            }

            // Store a reference for the final proposed ID and its content.
            // Used when finally adding data to the content provider.
            final_id_to_string.put(message_final_id, string_received);
            final_message_id_list.add(message_final_id);

            Log.i(TAG, "ADDING: " + message_final_id);

            // Sort final proposed message IDs
            Collections.sort(final_message_id_list);

            // Assign the sorted messages a simple integer key starting from 0
            // and insert them using the content provider and also display them in the TextView.
            // This logic can be optimized.
            for (int i=0; i < final_message_id_list.size(); i++)
            {
                TextView remoteTextView = (TextView) findViewById(R.id.textView1);
                remoteTextView.append(string_received + "\t\n");

                ContentResolver content_resolver = getContentResolver();
                Uri uri = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger2.provider");
                ContentValues keyValueToInsert = new ContentValues();

                keyValueToInsert.put("key"  , Integer.toString(i));
                keyValueToInsert.put("value", final_id_to_string.get((final_message_id_list.get(i))));

                Log.i(TAG, "CONTENT// final_id_to_string size: "+final_id_to_string.keySet().size());
                Log.i(TAG, "CONTENT// "+"Message Key: " + Integer.toString(i));

                content_resolver.insert(uri, keyValueToInsert);
            }
        }

        private Uri buildUri(String scheme, String authority)
        {
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
            /*
             * Send the message to all 5 AVDs including it own self.
             */
            // Disintegrate the string into its respective components
            String client_message        = msgs[0];
            String client_port           = msgs[1];
            String client_is_deliverable = msgs[2];
            String client_message_id     = msgs[3];

            Log.i(TAG, "CLIENT// Sending the following message.");
            Log.i(TAG, "CLIENT// Message: "+client_message);
            Log.i(TAG, "CLIENT// Port: "+client_port);
            Log.i(TAG, "CLIENT// Deliverable: "+client_is_deliverable);
            Log.i(TAG, "CLIENT// Message ID: "+client_message_id);

            // Message String Format - Deliverable : Message : Message ID
            String client_string = client_is_deliverable + ":" + client_message + ":" + client_message_id;

            ArrayList<String> port_list = new ArrayList<String>();

            port_list.add(REMOTE_PORT0);
            port_list.add(REMOTE_PORT1);
            port_list.add(REMOTE_PORT2);
            port_list.add(REMOTE_PORT3);
            port_list.add(REMOTE_PORT4);

            // Send the message
            for (String port: port_list)
            {
                try
                {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(port));

                    socket.setSoTimeout(500);

                    // Create Output Stream
                    PrintWriter client_output_stream = new PrintWriter(socket.getOutputStream(), true);

                    // Create Input Stream
                    InputStreamReader client_input_stream_reader = new InputStreamReader(socket.getInputStream());
                    BufferedReader client_input_stream = new BufferedReader(client_input_stream_reader);

                    // Send Message
                    client_output_stream.println(client_string);
                    Log.i(TAG, "CLIENT// ==SENT== - " + client_string);

                    String ack_msg = null;

                    // Get Acknowledgment and act on it based on is_deliverable
                    long time_before= System.currentTimeMillis();
                    while (!socket.isClosed())
                    {
                        try
                        {
                            long time_diff =System.currentTimeMillis() - time_before;

                            if (time_diff > 500)
                            {
                                //remove port
                                Log.i("P2", port + " NO MORE.");
                                throw new Exception();
                            }


                            Log.i(TAG, port + ack_msg + "-><-");
                            ack_msg = client_input_stream.readLine();

                            if (ack_msg != null) {
                                Log.i(TAG, "ERROR|" + ack_msg);

                                // Separate the elements of the acknowledgement
                                String ack_is_deliverable = ack_msg.split(":")[0];
                                String ack_message = ack_msg.split(":")[1];
                                String ack_message_id = ack_msg.split(":")[2];
                                String ack_proposed_id = ack_msg.split(":")[3];

                                Log.i(TAG, "CLIENT//ACK// Acknowledgement Received: ");
                                Log.i(TAG, "CLIENT//ACK// Deliverable: " + ack_is_deliverable);
                                Log.i(TAG, "CLIENT//ACK// Message: " + ack_message);
                                Log.i(TAG, "CLIENT//ACK// Message ID: " + ack_message_id);
                                Log.i(TAG, "CLIENT//ACK// Proposed ID: " + ack_proposed_id);

                                // Take action based on value of Deliverable
                                // If Deliverable == True:
                                //     Insert and Publish the message with the Final ID
                                // Else If Deliverable == False:
                                //     Append the proposed ID to the list of proposed IDs of that msg
                                //     If the list of proposed IDs has 5 proposals then Broadcast
                                //     the message with the final ID

                                if (ack_is_deliverable.equals("True")) {
                                    // Do nothing
                                    Log.i(TAG, "CLIENT//ACK Deliverable is True. Continuing...");
                                } else if (ack_is_deliverable.equals("False")) {
                                    Log.i(TAG, "LOGIC//SORTER// ======");
                                    // Get the proposed ids list for the message, append the proposed ID and put it back
                                    ArrayList<Float> ack_message_proposed_ids = proposed_ids.get(ack_message_id);
                                    if (!ack_message_proposed_ids.contains(Float.parseFloat(ack_proposed_id))) {
                                        ack_message_proposed_ids.add(Float.parseFloat(ack_proposed_id));
                                        proposed_ids.put(ack_message_id, ack_message_proposed_ids);
                                    }
                                    // Check if the size of the list just became 5
                                    int proposed_ids_size = ack_message_proposed_ids.size();

                                    Log.i(TAG, "PL//: " + proposed_ids.toString());

                                    // If proposed IDs size is 5 (No. of AVDs)
                                    // Broadcast to all AVDs the message and its final ID as the
                                    // max of proposed ids of the acknowledged message.
                                    if (proposed_ids_size >= 4) {
                                        float max_proposed_value = Collections.max(ack_message_proposed_ids);

                                        Log.i(TAG, "FIVE SIZE FOUND FOR -> " + ack_message_id + " | " + ack_message + " | " + Float.toString(max_proposed_value));

                                        String client_final_is_deliverable = "True";
                                        String client_final_message = ack_message;
                                        String client_final_message_id = Float.toString(max_proposed_value);

                                        Log.i(TAG, "CLIENT//FINAL// Sending Final message.");
                                        new ClientTask().doInBackground(
                                                client_final_message,
                                                client_port,
                                                client_final_is_deliverable,
                                                client_final_message_id);
                                        Log.i(TAG, "CLIENT//FINAL// Finished Sending Final message.");
                                    }
                                }

                                // Close all the Client Streams
                                Log.i(TAG, "CLIENT// Closing client input and output streams...");
                                // Close Output Stream
                                client_output_stream.close();
                                // Close Input Stream
                                client_input_stream_reader.close();
                                client_input_stream.close();
                                // Close the Socket
                                socket.close();
                                Log.i(TAG, "CLIENT// Client streams closed.");
                            }
                        }

                        catch (SocketTimeoutException e)
                        {
                            Log.e(TAG, "TimeOut");
                            // Close all the Client Streams
                            Log.i(TAG, "CLIENT// Closing client input and output streams...");
                            // Close Output Stream
                            client_output_stream.close();
                            // Close Input Stream
                            client_input_stream_reader.close();
                            client_input_stream.close();
                            // Close the Socket
                            socket.close();
                            Log.i(TAG, "CLIENT// Client streams closed.");
                        }
                        catch (IOException e)
                        {
                            Log.e("P3", "IOExc");
                            // Close all the Client Streams
                            Log.i(TAG, "CLIENT// Closing client input and output streams...");
                            // Close Output Stream
                            client_output_stream.close();
                            // Close Input Stream
                            client_input_stream_reader.close();
                            client_input_stream.close();
                            // Close the Socket
                            socket.close();
                            Log.i(TAG, "CLIENT// Client streams closed.");
                        }
                        catch (Exception e)
                        {
                            Log.e("P3", "Exc");
                            // Close all the Client Streams
                            Log.i(TAG, "CLIENT// Closing client input and output streams...");
                            // Close Output Stream
                            client_output_stream.close();
                            // Close Input Stream
                            client_input_stream_reader.close();
                            client_input_stream.close();
                            // Close the Socket
                            socket.close();
                            Log.i(TAG, "CLIENT// Client streams closed.");
                        }
                    }

                }
                catch (NullPointerException e)
                {
                    Log.e(TAG, "A node FAILED.");
                    port_list.remove(port);
                }
                catch (Exception e)
                {
                    Log.e(TAG, "Client task failed.");
                    Log.e(TAG, e.getMessage(), e.getCause());
                }
            }
            return null;
        }
    }
}
