package com.lumination.leadme.connections;

import android.util.Log;

import com.lumination.leadme.managers.NSDManager;
import com.lumination.leadme.managers.NetworkManager;
import com.lumination.leadme.services.NetworkService;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Class to handle the connections to individual students, runs three separate threads to send
 * messages, check connection and handle input.
 */
public class TcpClient extends Thread {
    private String TAG = "TcpClient: ";

    public InetAddress IpAddress;
    public int port;
    public int ID;
    private Socket clientSocket;
    private String Name;

    TcpClient tcp;
    boolean runAgain = true;
    boolean checkIn = true;
    ScheduledExecutorService scheduledExecutor = new ScheduledThreadPoolExecutor(3);

    public TcpClient(Socket socket, int clientID) {
        TAG += clientID; //ensures logs are identifiable
        IpAddress = socket.getInetAddress();
        port = socket.getPort();
        try {
            socket.setKeepAlive(true);
        } catch (SocketException e) {
            Log.d(TAG, "TcpClient: keep alive failed");
            e.printStackTrace();
        }
        ID = clientID;
        clientSocket = socket;
        tcp = this;

        scheduledExecutor.scheduleAtFixedRate(tcpRunner,0,1500, TimeUnit.MILLISECONDS);
        scheduledExecutor.scheduleAtFixedRate(ConnectionCheck,300,2000, TimeUnit.MILLISECONDS);
        scheduledExecutor.scheduleAtFixedRate(inputRun,0,1000, TimeUnit.MILLISECONDS);
    }

    /**
     * Responsible for sending messages to the learner from a leader, sends the messages in a
     * que so only one action is taken at a time.
     */
    Runnable tcpRunner = () -> {
        if (clientSocket.isConnected() && !clientSocket.isClosed()) {
            for (int i = 0; i < NetworkManager.currentClients.size(); i++) {
                Client client = NetworkManager.currentClients.get(i);
                if (ID == client.ID) {
                    while (client.messageQueue.size() > 0) {
                        Log.d(TAG, "sending: " + client.messageQueue.get(0).message + " " + client.messageQueue.get(0).type);
                        sendToClient(client.messageQueue.get(0).message, client.messageQueue.get(0).type);
                        NetworkManager.currentClients.get(i).messageQueue.remove(0);
                        try {
                            Thread.currentThread().sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    };

    /**
     * Responsible for checking the connection between the Leader and the Leaner associated with
     * this socket connection. Sends a ping every set time period if no other messages are being
     * sent.
     */
    Runnable ConnectionCheck = () -> {
        try {
            if(NetworkService.isRunning) {
                Log.d(TAG, "Sending Ping to: " + ID);
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                out.println("PING," + ID); //set to be ignored at the client end, tcp will tell us if they have disconnected
                if (out.checkError()) {
                    Log.e(TAG, "ERROR writing data to socket student has disconnected");
                    NSDManager.executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            NetworkManager.updateParent(Name, ID, "LOST");
                        }
                    });

                    scheduledExecutor.shutdown();
                    runAgain = false;
                }
            } else {
                scheduledExecutor.shutdown();
                runAgain = false;
            }
        } catch (IOException e) {
            Log.d(TAG, "checkConnection: failed");
            e.printStackTrace();
        }
    };

    /**
     * Responsible for handling incoming messages from the learner socket connection. Passes the
     * message of to the inputReceived function to determine an action.
     */
    Runnable inputRun = () -> {
        while(checkIn) {
            try {
                InputStreamReader inreader = new InputStreamReader(clientSocket.getInputStream());
                BufferedReader in = new BufferedReader(inreader);
                String Input = in.readLine();

                inputReceived(Input);
                if (Input == null) {
                    checkIn = false;
                }
            } catch (IOException e) {
                checkIn = false;
                e.printStackTrace();
            }
        }
    };

    private void inputReceived(String input) {
        if(input==null || !input.contains(",")){
            return;
        }
        Log.d(TAG, "inputHandler: "+ input);
        List<String> inputList = Arrays.asList(input.split(","));
        if(inputList.size()>1) {
            switch (inputList.get(0)) {
                case "NAME":
                    if (inputList.get(1).length() == 0) {
                        setLocalName("No_Name_Yet");
                    } else {
                        setLocalName(inputList.get(1));
                    }
                    break;
                case "PRINT":
                    Log.d(TAG, "inputHandler: " + inputList.get(1));
                    break;
                case "ACTION":
                    inputActionHandler(inputList.get(1));
                    break;
                case "PING":
                    Log.d(TAG, "inputReceived: ping messages are purposely ignored");
                    break;
                case "IMAGE":
                    NSDManager.executorService.submit(() -> NetworkManager.updateParent(inputList.get(1),ID,"IMAGE"));
                    break;
                default:
                    break;
            }
        }
    }

    private void inputActionHandler(String action) {
        //not sure why student would send action but implemented anyway
        Log.d(TAG, "inputActionHandler: Action received: "+action);
        NSDManager.executorService.submit(() -> NetworkManager.updateParent(action,ID,"ACTION"));

    }

    /**
     * Updates network adapter with name.
     * @param name A string representing the name of the student.
     */
    public void setLocalName(String name){
        Name=name;
        NSDManager.executorService.submit(() -> NetworkManager.updateParent(name+":"+IpAddress.toString(),ID,"NAME"));
    }

    void sendToClient(String message, String type){
        Log.d(TAG, "sendToClient: "+message+" Type: "+type);

       // simply sends message to the client attached to this process
        scheduledExecutor.schedule(() -> {
            try {
                PrintWriter out =   new PrintWriter(
                        new BufferedWriter(
                                new OutputStreamWriter(clientSocket.getOutputStream())), true);
                out.println(type + "," + message);
                out.flush();
            } catch (IOException e) {
                Log.d(TAG, "sendToClient: failed to write message to client, checking if there is a connection");
                //checkConnection();
                e.printStackTrace();
            }

            if(type.equals("DISCONNECT")){
                try {
                    clientSocket.close();
                    runAgain=false;
                    checkIn=false;
                    scheduledExecutor.shutdown();
                    Log.d(TAG, "sendToClient: client socket closed and student disconnected");
                    NSDManager.executorService.submit(() -> NetworkManager.updateParent("disconnect complete",ID,"DISCONNECT"));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        },0L,TimeUnit.MILLISECONDS);

        //executed after sending message so the student is notified that it is being forcefully disconnected.
    }
}
