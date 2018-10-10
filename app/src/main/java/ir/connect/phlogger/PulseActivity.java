package ir.connect.phlogger;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v4.math.MathUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.StringTokenizer;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Random;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class PulseActivity extends AppCompatActivity {

    private static final String TAG = "MY_APP_DEBUG_TAG";

    Handler bluetoothIn;

    final int handlerState = 0;                        //used to identify handler message
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();
    private OutputStream outStream = null;
    private PulseActivity.ConnectedThread mConnectedThread;

    // EXTRA string to send on to mainactivity
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address
    private static String address;

    private static final Random RANDOM = new Random();
    private LineGraphSeries<DataPoint> series;
    private int lastX = 0;

    // Data handling
    byte[] buffer = new byte[256];
    int bytes;

    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

        // ... (Add other message types here as needed.)
    }

    private TextView bpmView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pulse);

        // we get graph view instance
        GraphView graph = (GraphView) findViewById(R.id.graph);

        // data
        series = new LineGraphSeries<DataPoint>();
        graph.addSeries(series);

        // customize a little bit viewport
        Viewport viewport = graph.getViewport();
        viewport.setYAxisBoundsManual(true);
        viewport.setMinY(0);
        viewport.setMaxY(1000);
        viewport.setMinX(0);
        viewport.setMaxX(250);
        viewport.setXAxisBoundsManual(true);
        viewport.setScrollable(true);
//        viewport.setBackgroundColor();

        bpmView = (TextView) findViewById(R.id.bpmText);

        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {
                    recDataString.append((String) msg.obj);                                     //keep appending to string until ~
                    int endOfLineIndex = recDataString.indexOf(">");                            // determine the end-of-line

//                    Log.d("MainActivityLogger", recDataString.toString());

                    if (endOfLineIndex > 0) {                                                   // make sure there data before ~
                        int startOfLineIndex = recDataString.indexOf("<");
                        if(startOfLineIndex == 0) {
//                            Log.d("MainActivityLogger", recDataString.toString());
                            StringBuilder temp = new StringBuilder();
                            for (int idx = 1; recDataString.charAt(idx) != '>'; idx++)
                                temp.append(recDataString.charAt(idx));

                            try {
                                StringTokenizer tokens = new StringTokenizer(temp.toString(), ",");
                                tokens.nextToken(); tokens.nextToken(); tokens.nextToken();
                                bpmView.setText(tokens.nextToken());
                                tokens.nextToken();
//                                int BPM = Integer.parseInt(tokens.nextToken());
//                                int pulseA = Integer.parseInt(tokens.nextToken());
                                int pulseB = Integer.parseInt(tokens.nextToken().trim());

                                series.appendData(new DataPoint(lastX++, pulseB), true, 250);

                                if((lastX % 10) == 0)
                                {
                                    mConnectedThread.write("x".getBytes());
                                }
                            } catch (Exception e){

                            }

                        }

                        recDataString.delete(0, recDataString.length());                        //clear all string data
                    }
                }
            }
        };


        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    @Override
    public void onResume() {
        super.onResume();


        //Get MAC address from DeviceListActivity via intent
        Intent intent = getIntent();

        //Get the MAC address from the DeviceListActivty via EXTRA
        address = intent.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

        //create device and set the MAC address
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
        }
        // Establish the Bluetooth socket connection.
        try
        {
            btSocket.connect();
        } catch (IOException e) {
            try
            {
                btSocket.close();
            } catch (IOException e2)
            {
                //insert code to deal with this
            }
        }
        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
        }
        mConnectedThread = new PulseActivity.ConnectedThread(btSocket);
        mConnectedThread.start();

        //I send a character when resuming.beginning transmission to check device is connected
        //If it is not an exception will be thrown in the write method and finish() will be called
        mConnectedThread.write("x".getBytes());

        // we're going to simulate real time with thread that append data to the graph
  /*      new Thread(new Runnable() {

            @Override
            public void run() {
                // sleep to slow down the add of entries
                // we add 100 new entries
                for (;;) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            addEntry();
                        }
                    });

                    // sleep to slow down the add of entries
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // manage error ...
                    }
                }
            }
        }).start();*/
    }

    // add random data to graph
    private void addEntry() {
        // here, we choose to display max 10 points on the viewport and we scroll to end
//        series.appendData(new DataPoint(lastX++, RANDOM.nextDouble() * 10d), true, 10000);
        mConnectedThread.write("x".getBytes());
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        {
            //Don't leave Bluetooth sockets open when leaving activity
            btSocket.close();
        } catch (IOException e2) {
            //insert code to deal with this
        }
    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {

        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep looping to listen for received messages
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);

//                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(mmBuffer, 0, numBytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, numBytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

                // Share the sent message with the UI activity.
                Message writtenMsg = bluetoothIn.obtainMessage(
                        MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
                writtenMsg.sendToTarget();
            } catch (IOException e) {

                // Send a failure message back to the activity.
                Message writeErrorMsg =
                        bluetoothIn.obtainMessage(MessageConstants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                bluetoothIn.sendMessage(writeErrorMsg);
            }
        }

//        //write method
//        public void write(String input) {
//            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
//            try {
//                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
//            } catch (IOException e) {
//                //if you cannot write, close the application
//                Toast.makeText(getBaseContext(), "Write :Connection Failure", Toast.LENGTH_LONG).show();
//                finish();
//
//            }
//        }
    }
}