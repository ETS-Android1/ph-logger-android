package ir.connect.phlogger;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.math.MathUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Random;

import com.mikhaellopez.circularprogressbar.CircularProgressBar;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class PhActivity extends AppCompatActivity {

    private TextView roomText, sensorText, phText, phTextView, voltageText, pulseText;
    private CircularProgressBar roomCircle, sensorCircle, phCircle;
    private ImageView heartView;

    int animationDuration = 1500; // 2500ms = 2,5s
    int bpmHistory = 0, tempHistory = 0, humidityHistory = 0;

    Handler bluetoothIn;

    final int handlerState = 0;                        //used to identify handler message
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();
    private OutputStream outStream = null;
    private ConnectedThread mConnectedThread;

    // EXTRA string to send on to mainactivity
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address
    private static String address;

    private int counter = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ph);

        //Link the buttons and textViews to respective views
        phText = (TextView) findViewById(R.id.phText);
        phTextView = (TextView) findViewById(R.id.phTextView);
        voltageText = (TextView) findViewById(R.id.voltageText);
        roomText = (TextView) findViewById(R.id.roomText);
        sensorText = (TextView) findViewById(R.id.sensorText);

        phCircle = (CircularProgressBar) findViewById(R.id.phCircle);
        roomCircle = (CircularProgressBar)findViewById(R.id.roomCircle);
        sensorCircle = (CircularProgressBar)findViewById(R.id.sensorCircle);


        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {
                    recDataString.append((String) msg.obj);                                     //keep appending to string until ~
                    int endOfLineIndex = recDataString.indexOf(">");                            // determine the end-of-line

//                    Log.d("MainActivityLogger", recDataString.toString());

                    if (endOfLineIndex > 0) {                                                   // make sure there data before ~
                        int startOfLineIndex = recDataString.indexOf("<");
                        if(startOfLineIndex == 0)
                        {
                            counter = 0;
                            mConnectedThread.write("x");

                            Log.d("MainActivityLogger", recDataString.toString());
                            StringBuilder temp = new StringBuilder();
                            for (int idx = 1; recDataString.charAt(idx) != '>'; idx++)
                                temp.append(recDataString.charAt(idx));

                            try {
                                StringTokenizer tokens = new StringTokenizer(temp.toString(), ",");
                                float voltage = (float) (Float.parseFloat(tokens.nextToken()) * 5.0 / 1024 / 10);
                                float PH = (float) (-3.506 * voltage + 14.89);                     //get sensor value from string between indices 1-5
                                float humidity = Float.parseFloat(tokens.nextToken());
                                float tempreture = Float.parseFloat(tokens.nextToken());

                                roomCircle.setProgressWithAnimation(humidity, animationDuration);
                                roomText.setText(Float.toString(humidity));

                                sensorCircle.setProgressWithAnimation(tempreture, animationDuration);
                                sensorText.setText(Float.toString(tempreture));

                                if (PH >= 0 && PH <= 15)                                           //if it starts with # we know it is what we are looking for
                                {
//                            Log.d("MainActivityLogger", temp.toString());

                                    phCircle.setProgressWithAnimation(PH * 100 / 14, animationDuration);
                                    phText.setText(Float.toString(PH));
                                    voltageText.setText(Float.toString(voltage) + " volt");

                                    if (PH > 13) {
                                        phText.setTextColor(getResources().getColor(R.color.colorpurple));
                                        phTextView.setTextColor(getResources().getColor(R.color.colorpurple));

                                        phCircle.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorpurple));
                                    } else if (PH > 12) {
                                        phText.setTextColor(getResources().getColor(R.color.colorViolet));
                                        phTextView.setTextColor(getResources().getColor(R.color.colorViolet));

                                        phCircle.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorViolet));
                                    } else if (PH > 11) {
                                        phText.setTextColor(getResources().getColor(R.color.colorDarkBlue));
                                        phTextView.setTextColor(getResources().getColor(R.color.colorDarkBlue));

                                        phCircle.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorDarkBlue));
                                    } else if (PH > 10) {
                                        phText.setTextColor(getResources().getColor(R.color.colorBlue));
                                        phTextView.setTextColor(getResources().getColor(R.color.colorBlue));

                                        phCircle.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBlue));
                                    } else if (PH > 9) {
                                        phText.setTextColor(getResources().getColor(R.color.colorPaleBlue));
                                        phTextView.setTextColor(getResources().getColor(R.color.colorPaleBlue));

                                        phCircle.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPaleBlue));
                                    } else if (PH > 8) {
                                        phText.setTextColor(getResources().getColor(R.color.colorTorquise));
                                        phTextView.setTextColor(getResources().getColor(R.color.colorTorquise));

                                        phCircle.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorTorquise));
                                    } else if (PH > 7) {
                                        phText.setTextColor(getResources().getColor(R.color.colorDarkGreen));
                                        phTextView.setTextColor(getResources().getColor(R.color.colorDarkGreen));

                                        phCircle.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorDarkGreen));
                                    } else if (PH > 6) {
                                        phText.setTextColor(getResources().getColor(R.color.colorGreen));
                                        phTextView.setTextColor(getResources().getColor(R.color.colorGreen));

                                        phCircle.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorGreen));
                                    } else if (PH > 5) {
                                        phText.setTextColor(getResources().getColor(R.color.colorLimeGreen));
                                        phTextView.setTextColor(getResources().getColor(R.color.colorLimeGreen));

                                        phCircle.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorLimeGreen));
                                    } else if (PH > 4) {
                                        phText.setTextColor(getResources().getColor(R.color.colorYellow));
                                        phTextView.setTextColor(getResources().getColor(R.color.colorYellow));

                                        phCircle.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorYellow));
                                    } else if (PH > 3) {
                                        phText.setTextColor(getResources().getColor(R.color.colorBeige));
                                        phTextView.setTextColor(getResources().getColor(R.color.colorBeige));

                                        phCircle.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBeige));
                                    } else if (PH > 2) {
                                        phText.setTextColor(getResources().getColor(R.color.colorOrange));
                                        phTextView.setTextColor(getResources().getColor(R.color.colorOrange));

                                        phCircle.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorOrange));
                                    } else if (PH > 1) {
                                        phText.setTextColor(getResources().getColor(R.color.colorPink));
                                        phTextView.setTextColor(getResources().getColor(R.color.colorPink));

                                        phCircle.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPink));
                                    } else {
                                        phText.setTextColor(getResources().getColor(R.color.colorRed));
                                        phTextView.setTextColor(getResources().getColor(R.color.colorRed));

                                        phCircle.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRed));
                                    }

                                }

                            } catch (Exception e) {

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
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();

        //I send a character when resuming.beginning transmission to check device is connected
        //If it is not an exception will be thrown in the write method and finish() will be called
        mConnectedThread.write("x");

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

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();

            }
        }
    }

    private void sendData(String message) {
        byte[] msgBuffer = message.getBytes();

        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
            if (address.equals("00:00:00:00:00:00"))
                msg = msg + ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 35 in the java code";
            msg = msg +  ".\n\nCheck that the SPP UUID: " + BTMODULEUUID.toString() + " exists on server.\n\n";

        }
    }
}