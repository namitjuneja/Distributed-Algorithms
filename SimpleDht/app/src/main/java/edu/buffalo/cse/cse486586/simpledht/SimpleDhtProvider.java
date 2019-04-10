package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;

public class SimpleDhtProvider extends ContentProvider {
    private String TAG = "XXX";
    static final int SERVER_PORT = 10000;
    static String my_port = "";
    static String port_str = "";
    static String my_hash = "";
    static HashMap<String, String> hash_to_port_str = new HashMap<String, String>();
    static HashMap<String, String> hash_to_actual_port = new HashMap<String, String>();
    static final String port_str_0 = "5554";
    static final String port_str_1 = "5556";
    static final String port_str_2 = "5558";
    static final String port_str_3 = "5560";
    static final String port_str_4 = "5562";
    static ArrayList<String> port_str_list = new ArrayList<String>();
    private String succ = "";
    private String pred = "";



    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values)
    {
        try
        {
            String file_name      = String.valueOf(values.get("key"));
            String file_name_hash = genHash(file_name);
            String file_content   = String.valueOf(values.get("value"));

            if ((pred.compareTo(my_hash) < 0 && (file_name_hash.compareTo(pred) > 0 && file_name_hash.compareTo(my_hash) < 0)) ||
                (pred.compareTo(my_hash) > 0 && (file_name_hash.compareTo(pred) > 0 || file_name_hash.compareTo(my_hash) < 0)))
            {
                Log.i(TAG, "INSERT// File inserted at: "+hash_to_port_str.get(my_hash));
                FileOutputStream output_stream = getContext().openFileOutput(file_name, Context.MODE_PRIVATE);
                output_stream.write(file_content.getBytes());
                output_stream.close();
            }
            else
            {
                // Send to successor
                Log.i(TAG, "INSERT// Sending file to SUCC");
                // message_format message_type : key : value
                String message = "insert_message" + ":" + file_name + ":" + file_content;
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message, my_port, hash_to_actual_port.get(succ));
            }


        }
        catch (NoSuchAlgorithmException e)
        {
            Log.e(TAG, "INSERT// NoSuchAlgorithmException");
            Log.e(TAG, e.getMessage(), e.getCause());
        }
        catch (Exception e)
        {
            Log.e(TAG, "Content insertion error." + e);
            Log.e(TAG, e.getMessage(), e.getCause());
        }

        Log.v(TAG, "INSERT// Finished");
        return uri;
    }


    @Override
    public boolean onCreate()
    {


        // Get port string and apply logic to add node to the chord
        try
        {
            TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
            port_str = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
            my_hash = genHash(port_str);
            my_port = String.valueOf((Integer.parseInt(port_str) * 2));
            Log.d(TAG, "onCREATE// " + my_port + "||" + port_str);

            port_str_list.add(port_str_0);
            port_str_list.add(port_str_1);
            port_str_list.add(port_str_2);
            port_str_list.add(port_str_3);
            port_str_list.add(port_str_4);

            hash_to_port_str.put(genHash(port_str_0), port_str_0);
            hash_to_port_str.put(genHash(port_str_1), port_str_1);
            hash_to_port_str.put(genHash(port_str_2), port_str_2);
            hash_to_port_str.put(genHash(port_str_3), port_str_3);
            hash_to_port_str.put(genHash(port_str_4), port_str_4);

            hash_to_actual_port.put(genHash(port_str_0), String.valueOf((Integer.parseInt(port_str_0)*2)));
            hash_to_actual_port.put(genHash(port_str_1), String.valueOf((Integer.parseInt(port_str_1)*2)));
            hash_to_actual_port.put(genHash(port_str_2), String.valueOf((Integer.parseInt(port_str_2)*2)));
            hash_to_actual_port.put(genHash(port_str_3), String.valueOf((Integer.parseInt(port_str_3)*2)));
            hash_to_actual_port.put(genHash(port_str_4), String.valueOf((Integer.parseInt(port_str_4)*2)));

            // ArrayList<String> sortedList = new ArrayList<String>(hash_to_port_str.keySet());
            // Collections.sort(sortedList);
            // Log.i("SORT0", hash_to_port_str.get(sortedList.get(0)));
            // Log.i("SORT1", hash_to_port_str.get(sortedList.get(1)));
            // Log.i("SORT2", hash_to_port_str.get(sortedList.get(2)));
            // Log.i("SORT3", hash_to_port_str.get(sortedList.get(3)));
            // Log.i("SORT4", hash_to_port_str.get(sortedList.get(4)));

            if (!port_str.equals("5554"))
            {
                String message = "node_join:"+port_str;
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message, my_port, "11108");
            }
            else
            {
                succ = genHash("5554");
                pred = genHash("5554");
            }

            // Create Socket Server and Start Listening
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
            }

            return false;
        }
        catch (NoSuchAlgorithmException e)
        {
            Log.e(TAG, "onCREATE// NoSuchAlgorithmException");
            Log.e(TAG, e.getMessage(), e.getCause());
            return false;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder)
    {
        try
        {
            String file_name            = selection; // also the key
            String file_name_hash       = genHash(file_name);
            String file_content         = "";
            String[] default_projection = {"key", "value"};

            if ((pred.compareTo(my_hash) < 0 && (file_name_hash.compareTo(pred) > 0 && file_name_hash.compareTo(my_hash) < 0)) ||
                (pred.compareTo(my_hash) > 0 && (file_name_hash.compareTo(pred) > 0 || file_name_hash.compareTo(my_hash) < 0)))
            {
                // Read value from file
                FileInputStream input_stream = getContext().openFileInput(file_name);
                file_content = new BufferedReader(new InputStreamReader(input_stream)).readLine();
                input_stream.close();
            }
            else
            {
                // Send to successor
                Log.i(TAG, "QUERY// Sending request to SUCC");
                // message format query : file name
                String message = "query" + ":" + file_name;
                send_message(message, hash_to_actual_port.get(succ));
            }

            // Create a cursor which will be returned
            MatrixCursor matrix_cursor = new MatrixCursor(default_projection);
            String[] row_data = {file_name, file_value};
            matrix_cursor.addRow(row_data);
            return matrix_cursor;
        }
        catch (NoSuchAlgorithmException e)
        {
            Log.e(TAG, "INSERT// NoSuchAlgorithmException");
            Log.e(TAG, e.getMessage(), e.getCause());
        }
        catch (Exception e)
        {
            Log.e(TAG, "Content query error.");
            Log.e(TAG, "File Name: "+selection);
            Log.e(TAG, e.getMessage(), e.getCause());
            return null;
        }
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    private String send_message(String... msgs)
    {
        String message = msgs[0];
        String port    = msgs[1];
        Log.i(TAG, "QUERY-CLIENT// Init - message: "+message+" port: "+port);
        try
        {
            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                    Integer.parseInt(port));

            // Create Output Stream
            PrintWriter client_output_stream = new PrintWriter(socket.getOutputStream(), true);

            // Create Input Stream
            InputStreamReader client_input_stream_reader = new InputStreamReader(socket.getInputStream());
            BufferedReader client_input_stream = new BufferedReader(client_input_stream_reader);

            // Send Message
            client_output_stream.println(message);
            Log.i(TAG, "QUERY-CLIENT// Message Sent: " + message);

            // Close both the streams and the socket once ACK is received.
            try
            {
                while (!socket.isClosed())
                {
                    String ack_msg = null;

                    if ((ack_msg=client_input_stream.readLine()) != null)
                    {
                        Log.i(TAG, "QUERY-CLIENT// Acknowledgment received from server: "+ack_msg);
                        Log.i(TAG, "QUERY-CLIENT// Closing client input and output streams...");
                        // Close Output Stream
                        client_output_stream.close();
                        // Close Input Stream
                        client_input_stream_reader.close();
                        client_input_stream.close();
                        // Close the Socket
                        socket.close();
                        Log.i(TAG, "QUERY-CLIENT// Client streams closed.");
                        return ack_msg;
                    }
                }
            }
            catch (Exception e)
            {
                Log.e(TAG, "QUERY-CLIENT// ClientTask socket error.");
                Log.e(TAG, "QUERY-CLIENT// EXCEPTION: \n"+e);
                return "XXX";
            }
        }
        catch (Exception e)
        {
            Log.i(TAG, "QUERY-CLIENT// Client task failed.");
            Log.e(TAG, e.getMessage(), e.getCause());
            return "XXX";
        }
        return "XXX";
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
//                        publishProgress(message);
                        String ack_message = "";
                        String message_type     = message.split(":")[0];
                        Log.i(TAG, "SERVER// Message Received");
                        Log.i(TAG, "SERVER// Message Type: "+message_type);
                        // Message can be 2 types
                        // - Node Join (only in 5554) (node_join)
                        // - Insert Message (insert_message)
                        // - Query Message (query_message)
                        if (message_type.equals("node_join"))
                        {
                            String foreign_port_str      = message.split(":")[1];
                            String foreign_port_str_hash = genHash(foreign_port_str);


                            Log.i(TAG, "SERVER// Message:      "+foreign_port_str);
                            Log.i(TAG, "SERVER// Current PRED | SUCC - "+hash_to_port_str.get(pred)
                                                                             +" | "
                                                                             +hash_to_port_str.get(succ));


                            Log.i(TAG, "SERVER// Node Join Logic begins.");

                            if (pred.equals(succ) && pred.equals(my_hash)) // Case when second node is getting added
                            {
                                // This is the second node being joined after 5554
                                // ack_message format-> pred : succ
                                Log.i(TAG, "SERVER// Second Node Logic");
                                ack_message = port_str + ":" + port_str;
                                pred = genHash(foreign_port_str);
                                succ = genHash(foreign_port_str);
                            }
                            else
                            {
                                // Check if foreign_port_str_hash is contained in 5554's limit
                                // If yes
                                //     Send pred = my pred;      succ = my_hash
                                //     Set  pred = foreign_port; succ = same
                                // If no
                                //     Send another message to pred or succ nad wait for a response
                                Log.i(TAG, "SERVER// More than second node logic");
                                if ((pred.compareTo(my_hash) < 0 && (foreign_port_str_hash.compareTo(pred) > 0 && foreign_port_str_hash.compareTo(my_hash) < 0)) ||
                                    (pred.compareTo(my_hash) > 0 && (foreign_port_str_hash.compareTo(pred) > 0 || foreign_port_str_hash.compareTo(my_hash) < 0)))
                                {
                                    Log.i(TAG, "SERVER// Foreign port Found in limits");
                                    if (port_str.equals("5554"))
                                    {
                                        Log.i(TAG, "SERVER// Port found in 5554");
                                        // keep ack length to 2
                                        // ack_message format-> pred : succ
                                        ack_message = hash_to_port_str.get(pred)
                                                    + ":"
                                                    + hash_to_port_str.get(my_hash);
                                        // send pred set message to predecessor
                                        // Send message to tell pred node to reset its SUCC
                                        String reset_message = "reset_succ:"+foreign_port_str;
                                        send_message(reset_message, hash_to_actual_port.get(pred));
                                        Log.i(TAG, "SERVER// Sending reset message to current pred");
                                        Log.i(TAG, "SERVER// Message: "+reset_message);
                                    }
                                    else
                                    {
                                        Log.i(TAG, hash_to_port_str.get(pred) + " (" +
                                                foreign_port_str + ") " +
                                                hash_to_port_str.get(succ));
                                        ack_message = hash_to_port_str.get(pred)
                                                + ":"
                                                + hash_to_port_str.get(my_hash)
                                                + ":"
                                                + foreign_port_str;

                                        pred = foreign_port_str_hash;
                                    }
                                }
                                else
                                {
                                    Log.i(TAG, "SERVER// Sending message to SUCC");
                                    String temp_ack_message = send_message(message, hash_to_actual_port.get(succ));
                                    Log.i(TAG, "SERVER// ACK Message received: "+temp_ack_message);
                                    if (temp_ack_message.split(":").length == 3)
                                    {
                                        String[] temp_ack_message_split = temp_ack_message.split(":");
                                        succ = genHash(temp_ack_message_split[2]);
                                        Log.i(TAG, "SERVER// Send Message LEN 3.");
                                        Log.i(TAG, "SERVER// Setting SUCC to: "+temp_ack_message_split[2]);
                                        ack_message = "";
                                        for (int i=0 ; i<temp_ack_message_split.length-1 ; i++)
                                        {
                                            ack_message = ack_message + temp_ack_message_split[i] + ":";
                                        }
                                        ack_message = ack_message.substring(0,ack_message.length()-1);
                                        Log.i(TAG, "SERVER// Reduced ACK Message is: "+ack_message);
                                    }
                                    else
                                    {
                                        Log.i(TAG, "SERVER// Send Message LEN 2.");
                                        ack_message = temp_ack_message;
                                    }
                                }
                            }
                        }
                        else if (message_type.equals("reset_succ"))
                        {
                            String foreign_port_str = message.split(":")[1];
                            String foreign_port_str_hash = genHash(foreign_port_str);
                            succ = foreign_port_str_hash;
                            Log.i(TAG, "SUCC RESET to: "+foreign_port_str_hash);
                            ack_message = "reset success";
                        }
                        else if (message_type.equals("insert_message"))
                        {
                            String key   = message.split(":")[1];
                            String value = message.split(":")[2];

                            Uri uri = Uri.parse("content://edu.buffalo.cse.cse486586.simpledht.provider");
                            ContentValues keyValueToInsert = new ContentValues();
                            keyValueToInsert.put("key"  , key);
                            keyValueToInsert.put("value", value);
                            insert(uri, keyValueToInsert);
                        }
                        else if (message_type.equals("query"))
                        {
                            Uri uri = Uri.parse("content://edu.buffalo.cse.cse486586.simpledht.provider");
                            String key_name = message.split(":")[1];
                            Cursor query_cursor = query(uri, null, key_name, null, null)
                        }

                        // Send Acknowledgement
                        server_output_stream.println(ack_message);
                        Log.i(TAG, "SERVER// Acknowledgement Sent: "+ack_message);

                        Log.i(TAG, "SERVER// Closing server input and output streams...");
                        // Close Input Stream
                        server_input_stream_reader.close();
                        server_input_stream.close();
                        // Close Output Stream
                        server_output_stream.close();
                        // Close the Socket
                        server_socket.close();
                        Log.i(TAG, "SERVER// Server streams closed.");
                        Log.i(TAG, "SERVER// PREDF// "+hash_to_port_str.get(pred));
                        Log.i(TAG, "SERVER// SUCCF// "+hash_to_port_str.get(succ));
                    }
                }
            }
            catch (Exception e)
            {
                Log.e(TAG, "SERVER// ServerTask Socket error.");
                Log.e(TAG, "SERVER// EXCEPTION: \n"+e);
            }

            return null;
        }

        protected void onProgressUpdate(String... strings)
        {
            String string_received = strings[0];

            Log.i(TAG, "SERVER// ServerTask progressed(received) this message: ");
            Log.i(TAG, strings[0]);
        }

        private String send_message(String... msgs)
        {
            String message = msgs[0];
            String port = msgs[1];
            Log.i(TAG, "SERVER-CLIENT// Init - message: "+message+" port: "+port);
            try
            {
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(port));

                // Create Output Stream
                PrintWriter client_output_stream = new PrintWriter(socket.getOutputStream(), true);

                // Create Input Stream
                InputStreamReader client_input_stream_reader = new InputStreamReader(socket.getInputStream());
                BufferedReader client_input_stream = new BufferedReader(client_input_stream_reader);

                // Send Message
                client_output_stream.println(message);
                Log.i(TAG, "SERVER-CLIENT// Message Sent: " + message);

                // Close both the streams and the socket once ACK is received.
                try
                {
                    while (!socket.isClosed())
                    {
                        String ack_msg = null;

                        if ((ack_msg=client_input_stream.readLine()) != null)
                        {
                            Log.i(TAG, "SERVER-CLIENT// Acknowledgment received from server: "+ack_msg);
                            Log.i(TAG, "SERVER-CLIENT// Closing client input and output streams...");
                            // Close Output Stream
                            client_output_stream.close();
                            // Close Input Stream
                            client_input_stream_reader.close();
                            client_input_stream.close();
                            // Close the Socket
                            socket.close();
                            Log.i(TAG, "SERVER-CLIENT// Client streams closed.");
                            return ack_msg;
                        }
                    }
                }
                catch (Exception e)
                {
                    Log.e(TAG, "SERVER-CLIENT// ClientTask socket error.");
                    Log.e(TAG, "SERVER-CLIENT// EXCEPTION: \n"+e);
                    return "XXX";
                }
            }
            catch (Exception e)
            {
                Log.i(TAG, "CLIENT// Client task failed.");
                Log.e(TAG, e.getMessage(), e.getCause());
                return "XXX";
            }
            return "XXX";
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
            Log.i(TAG, "CLIENT// Client Task begins.");
            try
            {
                String port         = msgs[2];
                String message      = msgs[0];
                String message_type = message.split(":")[0];
                Log.i(TAG, "CLIENT// message: "+message);
                Log.i(TAG, "CLIENT// message type: "+message_type);
                Log.i(TAG, "CLIENT// port: "+port);


                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(port));

                // Create Output Stream
                PrintWriter client_output_stream = new PrintWriter(socket.getOutputStream(), true);

                // Create Input Stream
                InputStreamReader client_input_stream_reader = new InputStreamReader(socket.getInputStream());
                BufferedReader client_input_stream = new BufferedReader(client_input_stream_reader);

                // Send Message
                client_output_stream.println(message);
                Log.i(TAG, "CLIENT// Message Sent: " + message);

                // Close both the streams and the socket once ACK is received.
                try
                {
                    while (!socket.isClosed())
                    {
                        String ack_msg = null;

                        if ((ack_msg=client_input_stream.readLine()) != null)
                        {
                            Log.i(TAG, "CLIENT// Acknowledgment received from server.");
                            Log.i(TAG, "CLIENT// Closing client input and output streams...");
                            // Close Output Stream
                            client_output_stream.close();
                            // Close Input Stream
                            client_input_stream_reader.close();
                            client_input_stream.close();
                            // Close the Socket
                            socket.close();
                            Log.i(TAG, "CLIENT// Client streams closed.");
                            if (message_type.equals("node_join"))
                            {
                                Log.i(TAG, "CLIENT// ACK// " + ack_msg);
                                pred = genHash(ack_msg.split(":")[0]);
                                succ = genHash(ack_msg.split(":")[1]);
                                Log.i(TAG, "CLIENT// PREDF// " + hash_to_port_str.get(pred));
                                Log.i(TAG, "CLIENT// SUCCF// " + hash_to_port_str.get(succ));
                            }
                        }
                    }
                }
                catch (Exception e)
                {
                    Log.e(TAG, "CLIENT// ClientTask socket error.");
                    Log.e(TAG, "CLIENT// EXCEPTION: \n"+e);
                }
            }
            catch (Exception e)
            {
                Log.i(TAG, "CLIENT// Client task failed.");
                Log.e(TAG, e.getMessage(), e.getCause());

            }

            return null;
        }
    }
}
