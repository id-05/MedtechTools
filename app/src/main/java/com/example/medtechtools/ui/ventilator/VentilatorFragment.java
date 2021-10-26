package com.example.medtechtools.ui.ventilator;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import com.example.medtechtools.R;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

public class VentilatorFragment extends Fragment {

    private VentilatorViewModel ventilatorViewModel;
    private ImageView blueStatus;
    BluetoothDevice curDevice;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    ConnectedThread mConnectedThread;
    UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    static String TAG = "medtechtools";
    boolean bluetoothStatus = true;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ventilatorViewModel =
                new ViewModelProvider(this).get(VentilatorViewModel.class);
        View root = inflater.inflate(R.layout.fragment_ventilator, container, false);

        blueStatus = root.findViewById(R.id.blueStatus);
        blueStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("medtechtools","click");
                BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
                if((bluetooth!=null)&&(bluetoothStatus))
                {
                    if (bluetooth.isEnabled()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setTitle("Choose an device:");
                        ArrayList<String> listOfDevices = new ArrayList<>();
                        Set<BluetoothDevice> pairedDevices = bluetooth.getBondedDevices();
                        if(pairedDevices.size()>0){
                            for(BluetoothDevice device: pairedDevices)
                            {
                                listOfDevices.add(device.getName());
                            }
                        }

                        builder.setItems(listOfDevices.toArray(new String[0]), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                for(BluetoothDevice device: pairedDevices)
                                {
                                    if(device.getName().equals(listOfDevices.get(which))){
                                        curDevice = device;
                                        ConnectThread connect = new ConnectThread(device, MY_UUID_INSECURE);
                                        connect.start();

                                        //byte[] bytik = new byte[]{(byte) 0x01};
                                        //mConnectedThread.write(bytik);
                                    }
                                }
                            }
                        });

                        AlertDialog dialog = builder.create();
                        dialog.show();
                        bluetoothStatus = false;
                    }
                    else
                    {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivity(enableBtIntent);
                    }
                }else{
                    mConnectedThread.cancel();
                    blueStatus.setImageResource(R.drawable.ic_baseline_bluetooth_disabled_24);
                    bluetoothStatus = true;
                }
            }
        });

        final TextView textView = root.findViewById(R.id.text_gallery);
        ventilatorViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        return root;
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "ConnectThread: started.");
            mmDevice = device;
            deviceUUID = uuid;
        }

        public void run(){
            BluetoothSocket tmp = null;
            Log.i(TAG, "RUN mConnectThread ");
            try {
                Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID: "
                        + MY_UUID_INSECURE );
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID_INSECURE);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
            }

            mmSocket = tmp;

            try {
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                    Log.d(TAG, "run: Closed Socket.");
                } catch (IOException e1) {
                    Log.e(TAG, "mConnectThread: run: Unable to close connection in socket " + e1.getMessage());
                }
                Log.d(TAG, "run: ConnectThread: Could not connect to UUID: " + MY_UUID_INSECURE );
            }
            connected(mmSocket);
        }

        public void cancel() {
            try {
                Log.d(TAG, "cancel: Closing Client Socket.");
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: close() of mmSocket in Connectthread failed. " + e.getMessage());
            }
        }
    }

    private void connected(BluetoothSocket mmSocket) {
        Log.d(TAG, "connected: Starting.");
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                blueStatus.setImageResource(R.drawable.ic_baseline_bluetooth_connected_24);
            }
        });

    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Starting.");

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    final String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + Arrays.toString(incomingMessage.getBytes()));
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //view_data.setText(incomingMessage);
                        }
                    });
                } catch (IOException e) {
                    Log.e(TAG, "write: Error reading Input Stream. " + e.getMessage() );
                    break;
                }
            }
        }


        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to outputstream: " + text);
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "write: Error writing to output stream. " + e.getMessage() );
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }



}