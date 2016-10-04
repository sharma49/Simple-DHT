package edu.buffalo.cse.cse486586.simpledht;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {
    private static final String TAG = SimpleDhtProvider.class.getSimpleName();
    // Ports

    public int myPort;
//    public String myPortHash=null;
    public int myPred;
    public int mySucc;
    private static final String key = "key";
    private static final String value = "value";
    public volatile ConcurrentHashMap<String,String> h=new ConcurrentHashMap<String,String> ();
    public ArrayList<Integer> arr;
    public static volatile ConcurrentHashMap<String,String> res=new ConcurrentHashMap<String,String> ();
    public static volatile MatrixCursor qcursor = new MatrixCursor(new String[]{key,value});

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        delete(selection);
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub

        insert(values.getAsString(key), values.getAsString(value));

        return uri;
    }
    private void insert(String key, String value) {
        try {
            String hash = genHash(key);
            String mhash=genHash(String.valueOf(myPort));
            if(myPort==myPred){
                h.put(key, value);
                return;
            }
            if ((hash).compareTo(genHash(String.valueOf(myPred)))>0 && (hash).compareTo(mhash)<=0) {
                h.put(key, value);
                return;
            }
            if ((mhash).compareTo(genHash(String.valueOf(myPred)))<0 && (mhash).compareTo(genHash(String.valueOf(mySucc)))<0){
                if(hash.compareTo(mhash)<=0 || (hash).compareTo(genHash(String.valueOf(myPred)))>0)
                h.put(key, value);
                else{
                    Message msg=new Message();
                    msg.key=key;
                    msg.value=value;
                    msg.type="insert";
                    client(msg,mySucc*2);
                }
            }
//

            else{
                Message msg=new Message();
                msg.key=key;
                msg.value=value;
                msg.type="insert";
                client(msg,mySucc*2);
            }

        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG,  e.getMessage());
        }
    }

    @Override
    public boolean onCreate() {
        TelephonyManager tel = (TelephonyManager)this.getContext().getSystemService(
                Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = Integer.parseInt(portStr);
        arr=new ArrayList<Integer>();
        arr.add(myPort);

//        try {
//            myPortHash = genHash(String.valueOf(myPort));
//        }
//        catch(NoSuchAlgorithmException e){
//            Log.d(TAG, "NoSuchAlgo");
//        }
        try {
            ServerSocket serverSocket = new ServerSocket(10000);
            server(serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "ServerSocket:\n" + e.getMessage());
            return false;
        }
        myPred=myPort;
        mySucc=myPort;
        Message msg =new Message();
        msg.myPort=myPort;
        msg.pred=myPred;
        msg.succ=mySucc;
        msg.h=h;
        msg.type="join";

        if(myPort!=5554)
        client(msg, 11108);
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        if (selection.equals("*") || selection.equals("@")) {
            return Allquery(selection, myPort);
        } else {
            Log.d(TAG,"here1");

            return singlequery(selection, myPort);
        }
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
    public void client(final Message msg,final int port){
        new Thread(new Runnable() {


            public void run() {


                try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), port);
                    ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                    output.writeObject(msg);
                    output.close();
                    socket.close();
                } catch (UnknownHostException e) {
                    Log.e(TAG, "UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "IOException"+port+msg.type);
                }

            }
        }).start();
    }

    private void server(final ServerSocket s) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    ServerSocket serverSocket = s;
                    while (true) {
                        Socket socket = serverSocket.accept();
                        ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                        Message msg = (Message) input.readObject();
                        try {
                            if (msg.type.equals("join")) {
                                Log.i(TAG, String.valueOf(myPred) + " " + String.valueOf(myPort) + " " + String.valueOf(mySucc));

                                joinNode(msg);

                            }
                            else if (msg.type.equals("update")) {
                                int i = 0;

                                arr=msg.arr;
                                i=msg.arr.indexOf(myPort);
                                if (i == 0) {
                                    myPred = msg.arr.get(msg.arr.size() - 1);
                                    if (i + 1 <= msg.arr.size() - 1)
                                        mySucc = msg.arr.get(i + 1);
                                } else if (i == msg.arr.size() - 1) {
                                    if (i - 1 >= 0)
                                        myPred = msg.arr.get(i - 1);
                                    mySucc = (msg.arr.get(0));
                                } else {
                                    myPred = msg.arr.get(i - 1);
                                    mySucc = msg.arr.get(i + 1);
                                }

                                Log.i(TAG, String.valueOf(myPred) + " " + String.valueOf(myPort) + " " + String.valueOf(mySucc));
                                for(int p:arr){
                                    Log.d(TAG,p+" ");
                                }
                            }

                            else if (msg.type.equals("insert")) {

                                insert(msg.key, msg.value);
                            }

                            else if(msg.type.equals("send_client")){
                                if(msg.key.equals("AllQuery")) {
                                    Message m = new Message();
                                    m.key="AllQuery";
                                    m.h = h;
                                    m.type = "receive";
                                    client(m, msg.origin * 2);
                                }
                                else if(h.containsKey(msg.key)){
                                    Message m = new Message();
                                    m.key=msg.key;
                                    m.value = h.get(msg.key);
                                    m.type = "receive";
                                    client(m, msg.origin * 2);
                                }
                            }
                            else if(msg.type.equals("receive")){

                                if(msg.key.equals("AllQuery"))
                                    res.putAll(msg.h);
                                else {
                                    MatrixCursor cursor = new MatrixCursor(new String[]{key, value});
                                    cursor.addRow(new String[]{msg.key, msg.value});

                                    qcursor=cursor;
                                }

                            }
                            else if(msg.type.equals("delete")){
                                if(msg.key.equals("notcheck")){
                                    h.clear();
                                }
                                else if(h.containsKey(msg.key)) h.remove(msg.key);
                            }


                        }catch(Exception e){
                            Log.e(TAG,e.toString()+msg.type);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "IOException1");
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        }).start();
    }
    public void joinNode(Message msg){
        String hash1=null;
        String hash2=null;
        try {
            hash1 = genHash(String.valueOf(arr.get(0)));
            hash2 = genHash(String.valueOf(msg.myPort));
        }catch(NoSuchAlgorithmException e){
            Log.d(TAG,e.toString());
        }
        int i=0;
        while(i<arr.size() && hash2.compareTo(hash1)>0){
            try {
                if(i+1<arr.size())
                hash1 = genHash(String.valueOf(arr.get(i+1)));
            }catch(NoSuchAlgorithmException e){
                Log.d(TAG,e.toString());
            }
            i++;
        }
        arr.add(i,msg.myPort);
        msg.arr=arr;
        msg.type="update";
        for(int j :arr){
            Log.i(TAG, String.valueOf(j));
            client(msg, j*2);
        }

    }

    private Cursor Allquery(String key1,int origin) {

        MatrixCursor cursor = new MatrixCursor(new String[]{key, value});

        if (myPort == myPred || key1.equals("@") ) {
            for(Map.Entry<String, String> entry : h.entrySet()) {
                Log.d(TAG,"h@"+entry.getKey());
                cursor.addRow(new String[]{entry.getKey(), entry.getValue()});
            }
            return cursor;
        } else {
            res.putAll(h);
            Message m = new Message();

            m.type = "send_client";
            m.origin = origin;
            m.key="AllQuery";
            for(int j:arr) {
                if(j!=myPort)
                client(m, j*2);

            }

            try {
                Thread.sleep(7000);
            }
            catch(Exception e){
                Log.d(TAG,e.toString());
            }


            for (Map.Entry<String, String> entry : res.entrySet()) {
                cursor.addRow(new String[]{entry.getKey(), entry.getValue()});
            }
        }

        return cursor;
    }


    private Cursor singlequery(String key1, int origin) {
        MatrixCursor cursor = new MatrixCursor(new String[]{key, value});
        Log.d(TAG,"here");

        if(h.containsKey(key1)){
            Log.d(TAG, "normal"+key1);

            cursor.addRow(new String[]{key1,h.get(key1)});
            return cursor;
        }
        else{
            Message m=new Message();
            m.key=key1;
            m.type="send_client";
            m.origin=origin;
            for(int j:arr){
                if(j!=myPort)
                    client(m,j*2);
            }
            try {
                Thread.sleep(4000);
            }
            catch(Exception e){
                Log.d(TAG,e.toString());
            }
        }

        return qcursor;

    }
    private void delete(String key) {
        if (key.equals("@") ) {
            h.clear();
        }
         else  if (key.equals("*")) {
                h.clear();
                Message m=new Message();
                m.type="delete";
                m.key="notcheck";
                for(int j:arr) {
                    if (j != myPort)
                        client(m, j * 2);
                }


        } else {
                if (h.containsKey(key)) {
                    h.remove(key);
                } else {
                    Message m=new Message();
                    m.type="delete";
                    m.key=key;
                    for(int j:arr) {
                        if (j != myPort)
                            client(m, j * 2);
                    }

                }

        }
    }

}
