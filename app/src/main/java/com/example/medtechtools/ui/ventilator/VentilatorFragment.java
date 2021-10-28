package com.example.medtechtools.ui.ventilator;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.medtechtools.R;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

import static android.content.Context.MODE_PRIVATE;

public class VentilatorFragment extends Fragment {

    private VentilatorViewModel ventilatorViewModel;
    Animation animationRotate = null;
    Animation animationStop = null;
    BluetoothDevice curDevice;
    private BluetoothDevice mmDevice;
    ConnectedThread mConnectedThread;
    static String TAG = "medtechtools";
    boolean bluetoothStatus = true;
    private Menu mOptionsMenu;
    LineChartView chart;
    List<PointValue> values = new ArrayList<>();
    List<PointValue> valuesCursor = new ArrayList<>();
    LineChartData data;
    int i = 0;
    int j = 0;
    int amountOfPoints = 120;
    ArrayList<Byte> bufList = new ArrayList<>();
    ActionMenuItemView imageSearchBluetooth;
    UUID deviceUUID;
    SharedPreferences sPref;
    TextView statusString;
    String visibleDevName;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ventilatorViewModel =
                new ViewModelProvider(this).get(VentilatorViewModel.class);
        View root = inflater.inflate(R.layout.fragment_ventilator, container, false);

        statusString = root.findViewById(R.id.statusString);
        loadText();
        statusString.setText(visibleDevName);

        chart = root.findViewById(R.id.chart);
        chart.setInteractive(true);
        chart.setZoomType(ZoomType.HORIZONTAL_AND_VERTICAL);
        chart.setFocusable(false);

        final Viewport v = new Viewport(chart.getMaximumViewport());
        v.bottom = 0;
        v.top = 4;
        v.left = 0;
        v.right = amountOfPoints;
        chart.setMaximumViewport(v);
        chart.setCurrentViewport(v);
        chart.setViewportCalculationEnabled(false);
        Line line = new Line(values).setColor(Color.BLUE).setCubic(true);
        Line lineCursor = new Line(valuesCursor).setColor(Color.RED);
        List<Line> lines = new ArrayList<>();

        line.setHasPoints(false);
        line.setFilled(true);
        line.setCubic(true);
        lines.add(line);

        lineCursor.setHasPoints(false);
        lineCursor.setPointRadius(2);
        lineCursor.setFilled(true);
        lineCursor.setCubic(true);
        lines.add(lineCursor);

        data = new LineChartData();
        data.setLines(lines);

        List<AxisValue> axisValuesForY = new ArrayList<>();
        AxisValue tempAxisValue;
        for (float i = 0; i <= 4; i +=1){
            tempAxisValue = new AxisValue(i);
            tempAxisValue.setLabel(i+"");
            axisValuesForY.add(tempAxisValue);
        }
        Axis axisX = new Axis ();
        axisX.setName("t");
        Axis axisY = new Axis (axisValuesForY);
        axisY.setName("U");
        axisY.setAutoGenerated(false);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);

        chart.setLineChartData(data);
        setHasOptionsMenu(true);

        animationStop = AnimationUtils.loadAnimation(getContext(), R.anim.stop);
        animationRotate = AnimationUtils.loadAnimation(getContext(), R.anim.rotate);
        imageSearchBluetooth = getActivity().findViewById(R.id.ventiletorBluetooth);
        animationRotate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                imageSearchBluetooth.startAnimation(animationRotate);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        return root;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.ventilator_menu, menu);
        mOptionsMenu = menu;
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.ventiletorBluetooth){
            imageSearchBluetooth = getActivity().findViewById(R.id.ventiletorBluetooth);
            tryConnect();
        }

        return super.onOptionsItemSelected(item);
    }

    public void tryConnect(){
        Log.d("medtechtools","click");
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        if((bluetooth!=null)&&(bluetoothStatus))
        {
            if (bluetooth.isEnabled()) {
                if(visibleDevName.equals("no remember device")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Choose an device:");
                    ArrayList<String> listOfDevices = new ArrayList<>();
                    Set<BluetoothDevice> pairedDevices = bluetooth.getBondedDevices();
                    if (pairedDevices.size() > 0) {
                        for (BluetoothDevice device : pairedDevices) {
                            listOfDevices.add(device.getName());
                        }
                    }
                    builder.setItems(listOfDevices.toArray(new String[0]), new DialogInterface.OnClickListener() {
                        @SuppressLint("RestrictedApi")
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            for (BluetoothDevice device : pairedDevices) {
                                if (device.getName().equals(listOfDevices.get(which))) {
                                    uiSearchBT();
                                    curDevice = device;
                                    visibleDevName = device.getName();
                                    statusString.setText(visibleDevName);
                                    saveText();
                                    ParcelUuid[] uuids = device.getUuids();
                                    ConnectThread connect = new ConnectThread(device, uuids[0].getUuid());
                                    connect.start();
                                    bluetoothStatus = false;
                                }
                            }
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }else{
                    Set<BluetoothDevice> pairedDevices = bluetooth.getBondedDevices();
                    for (BluetoothDevice device : pairedDevices) {
                        if (device.getName().equals(visibleDevName)) {
                            uiSearchBT();
                            curDevice = device;
                            ParcelUuid[] uuids = device.getUuids();
                            ConnectThread connect = new ConnectThread(device, uuids[0].getUuid());
                            connect.start();
                            bluetoothStatus = false;
                        }
                    }
                }
            }
            else
            {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(enableBtIntent);
            }
        }else{
            byte[] bytes = {0x00, 0x01};
            mConnectedThread.write(bytes);
            mConnectedThread.cancel();
            MenuItem menuItem = mOptionsMenu.findItem(R.id.ventiletorBluetooth);
            menuItem.setIcon(R.drawable.ic_baseline_bluetooth_disabled_24);
            bluetoothStatus = true;
        }
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
          //  Log.d(TAG, "ConnectThread: started.");
            mmDevice = device;
            deviceUUID = uuid;
        }

        public void run(){
            BluetoothSocket tmp = null;
           // Log.i(TAG, "RUN mConnectThread ");
            try {
              //  Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID: "
                     //   + deviceUUID);
                tmp = mmDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
              //  Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
            }

            mmSocket = tmp;

            try {
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                   // Log.d(TAG, "run: Closed Socket.");
                } catch (IOException e1) {
                   // Log.e(TAG, "mConnectThread: run: Unable to close connection in socket " + e1.getMessage());
                }
                //og.d(TAG, "run: ConnectThread: Could not connect to UUID: " + deviceUUID);
            }
            connected(mmSocket);
        }

        public void cancel() {
            try {
                //Log.d(TAG, "cancel: Closing Client Socket.");
                mmSocket.close();
            } catch (IOException e) {
                //Log.e(TAG, "cancel: close() of mmSocket in Connectthread failed. " + e.getMessage());
            }
        }
    }

    private void connected(BluetoothSocket mmSocket) {
        Log.d(TAG, "connected: Starting.");
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
        chart.setLineChartData(data);
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
            byte[] buffer = new byte[1];
            int bytes;
            getActivity().runOnUiThread(new Runnable() {
                @SuppressLint({"RestrictedApi", "UseCompatLoadingForDrawables"})
                @Override
                public void run() {
                    uiStartBT();
                    byte[] bytes = {0x01, 0x01};
                    write(bytes);
                }
            });
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    bufList.add(buffer[0]);
                    j++;
                    if(j==6)
                    {
                        if (i == amountOfPoints) {
                            i = 0;
                        }
                        double buf = (((( bufList.get(0) & 0xff ) << 8)) | ( bufList.get(1) & 0xff )) * 0.000805;
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                if ((values != null) && (values.size() > (amountOfPoints - 1))) {
                                    values.get(i-1).set(i, (float) buf);
                                    valuesCursor.get(0).set(i, (float) buf);
                                } else {
                                    if(valuesCursor.size()==0){
                                        valuesCursor.add(new PointValue(i, (float) buf));
                                    }else{
                                        valuesCursor.get(0).set(i, (float) buf);
                                    }
                                    values.add(new PointValue(i, (float) buf));
                                }
                                chart.setLineChartData(data);
                            }
                        });
                        i++;
                        j = 0;
                        bufList.clear();
                    }
                } catch (IOException e) {
                    //Log.e(TAG, "write: Error reading Input Stream. " + e.getMessage() );
                    getActivity().runOnUiThread(new Runnable() {
                        @SuppressLint({"RestrictedApi", "UseCompatLoadingForDrawables"})
                        @Override
                        public void run() {
                            uiStopBT();
                        }
                    });
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            //Log.d(TAG, "write: Writing to outputstream: " + text);
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                //Log.e(TAG, "write: Error writing to output stream. " + e.getMessage() );
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException ignored) { }
        }
    }

    @SuppressLint("RestrictedApi")
    public void uiSearchBT(){
        imageSearchBluetooth.setIcon(ContextCompat.getDrawable(requireContext(),R.drawable.ic_baseline_autorenew_24));
        imageSearchBluetooth.startAnimation(animationRotate);
    }

    @SuppressLint({"RestrictedApi"})
    public void uiStopBT(){
        imageSearchBluetooth.setIcon(ContextCompat.getDrawable(requireContext(),R.drawable.ic_baseline_bluetooth_disabled_24));
        imageSearchBluetooth.startAnimation(animationStop);
        values.clear();
        valuesCursor.clear();
        i = 0;
        j = 0;
        chart.setLineChartData(data);
    }

    @SuppressLint({"RestrictedApi"})
    public void uiStartBT(){
        imageSearchBluetooth.setIcon(ContextCompat.getDrawable(requireContext(),R.drawable.ic_baseline_bluetooth_connected_24));
        imageSearchBluetooth.startAnimation(animationStop);
    }

    void saveText() {
        sPref = getActivity().getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putString("BLUENAME", visibleDevName);
        ed.commit();
    }

    void loadText() {
        sPref = getActivity().getPreferences(MODE_PRIVATE);
        String savedText = sPref.getString("BLUENAME", "no remember device");
        visibleDevName = savedText;
    }
}