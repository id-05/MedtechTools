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

import com.example.medtechtools.R;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    //private VentilatorViewModel ventilatorViewModel;
    Animation animationRotate = null;
    Animation animationStop = null;
    BluetoothDevice curDevice;
    private BluetoothDevice mmDevice;
    ConnectedThread mConnectedThread;
    static String TAG = "medtechtools";
    boolean bluetoothStatus = true;
    private Menu mOptionsMenu;

    LineChartView chartPressure;
    List<PointValue> valuesPressure = new ArrayList<>();
    List<PointValue> valuesPressureCursor = new ArrayList<>();
    LineChartData dataPressure;

    LineChartView chartFlow;
    List<PointValue> valuesFlow = new ArrayList<>();
    List<PointValue> valuesFlowCursor = new ArrayList<>();
    LineChartData dataFlow;

    int i = 0;
    int j = 0;
    int pointsPressure = 2000;
    int pointsFlow = 2000;
    ArrayList<Byte> bufList = new ArrayList<>();
    ActionMenuItemView imageSearchBluetooth;
    UUID deviceUUID;
    SharedPreferences sPref;
    TextView statusString;
    String visibleDevName;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        //ventilatorViewModel = new ViewModelProvider(this).get(VentilatorViewModel.class);
        View root = inflater.inflate(R.layout.fragment_ventilator, container, false);

        statusString = root.findViewById(R.id.statusString);
        loadText();
        statusString.setText(visibleDevName);

        chartPressure = root.findViewById(R.id.chartPressure);
        chartPressure.setInteractive(true);
        chartPressure.setZoomType(ZoomType.HORIZONTAL_AND_VERTICAL);
        chartPressure.setFocusable(false);

        chartFlow = root.findViewById(R.id.chartFlow);
        chartFlow.setInteractive(true);
        chartFlow.setZoomType(ZoomType.HORIZONTAL_AND_VERTICAL);
        chartFlow.setFocusable(false);

        final Viewport vP = new Viewport(chartPressure.getMaximumViewport());
        vP.bottom = 0;
        vP.top = 4;
        vP.left = 0;
        vP.right = pointsPressure;
        chartPressure.setMaximumViewport(vP);
        chartPressure.setCurrentViewport(vP);
        chartPressure.setViewportCalculationEnabled(false);

        final Viewport vF = new Viewport(chartFlow.getMaximumViewport());
        vF.bottom = 0;
        vF.top = 4;
        vF.left = 0;
        vF.right = pointsFlow;
        chartFlow.setMaximumViewport(vF);
        chartFlow.setCurrentViewport(vF);
        chartFlow.setViewportCalculationEnabled(false);

        Line linePressure = new Line(valuesPressure).setColor(Color.GRAY);//.setCubic(true);
        Line linePressureCursor = new Line(valuesPressureCursor).setColor(Color.YELLOW);
        List<Line> linesPressure = new ArrayList<>();
        linePressure.setHasPoints(false);
        linePressure.setFilled(true);
        linesPressure.add(linePressure);
        linePressureCursor.setHasPoints(false);
        linePressureCursor.setPointRadius(2);
        linePressureCursor.setFilled(true);
        linesPressure.add(linePressureCursor);
        dataPressure = new LineChartData();
        dataPressure.setLines(linesPressure);

        Line lineFlow = new Line(valuesFlow).setColor(Color.GRAY);//.setCubic(true);
        Line lineFlowCursor = new Line(valuesFlowCursor).setColor(Color.YELLOW);
        List<Line> linesFlow = new ArrayList<>();
        lineFlow.setHasPoints(false);
        lineFlow.setFilled(true);
        linesFlow.add(lineFlow);
        lineFlowCursor.setHasPoints(false);
        lineFlowCursor.setPointRadius(2);
        lineFlowCursor.setFilled(true);
        linesFlow.add(lineFlowCursor);
        dataFlow = new LineChartData();
        dataFlow.setLines(linesFlow);

        List<AxisValue> axisValuesForYP = new ArrayList<>();
        AxisValue tempAxisValueP;
        for (float i = 0; i <= 4; i +=1){
            tempAxisValueP = new AxisValue(i);
            tempAxisValueP.setLabel(i+"");
            axisValuesForYP.add(tempAxisValueP);
        }
        Axis axisXP = new Axis ();
        axisXP.setName("t");
        Axis axisYP = new Axis (axisValuesForYP);
        axisYP.setName("Pressure");
        axisYP.setAutoGenerated(false);
        dataPressure.setAxisXBottom(axisXP);
        dataPressure.setAxisYLeft(axisYP);

        List<AxisValue> axisValuesForYF = new ArrayList<>();
        AxisValue tempAxisValueF;
        for (float i = 0; i <= 4; i +=1){
            tempAxisValueF = new AxisValue(i);
            tempAxisValueF.setLabel(i+"");
            axisValuesForYF.add(tempAxisValueF);
        }
        Axis axisXF = new Axis ();
        axisXF.setName("t");
        Axis axisYF = new Axis (axisValuesForYF);
        axisYF.setName("Flow");
        axisYF.setAutoGenerated(false);
        dataFlow.setAxisXBottom(axisXF);
        dataFlow.setAxisYLeft(axisYF);

        chartPressure.setLineChartData(dataPressure);
        chartFlow.setLineChartData(dataFlow);

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
            byte[] bytes = {0x00, 0x00};
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
        chartPressure.setLineChartData(dataPressure);
        chartFlow.setLineChartData(dataPressure);
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
                        if (i == pointsPressure) {
                            i = 0;
                        }
                        double valuePress = (((( bufList.get(0) & 0xff ) << 8)) | ( bufList.get(1) & 0xff )) * 0.000805;
                        double valueFlow = (((( bufList.get(2) & 0xff ) << 8)) | ( bufList.get(3) & 0xff )) * 0.000805;
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                if ((valuesPressure != null) && (valuesPressure.size() > (pointsPressure - 1))) {
                                    valuesPressure.get(i-1).set(i, (float) valuePress);
                                   // Log.d(TAG,"i = "+i);
                                    valuesPressureCursor.get(0).set(i, (float) valuePress);
                                } else {
                                    if(valuesPressureCursor.size()==0){
                                        valuesPressureCursor.add(new PointValue(i, (float) valuePress));
                                    }else{
                                        valuesPressureCursor.get(0).set(i, (float) valuePress);
                                    }
                                    valuesPressure.add(new PointValue(i, (float) valuePress));
                                }
                                chartPressure.setLineChartData(dataPressure);

                                if ((valuesFlow != null) && (valuesFlow.size() > (pointsFlow - 1))) {
                                    valuesFlow.get(i-1).set(i, (float) valueFlow);
                                  //  Log.d(TAG,"i = "+i);
                                    valuesFlowCursor.get(0).set(i, (float) valueFlow);
                                } else {
                                    if(valuesFlowCursor.size()==0){
                                        valuesFlowCursor.add(new PointValue(i, (float) valueFlow));
                                    }else{
                                        valuesFlowCursor.get(0).set(i, (float) valueFlow);
                                    }
                                    valuesFlow.add(new PointValue(i, (float) valueFlow));
                                }
                                chartFlow.setLineChartData(dataFlow);
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
            //String text = new String(bytes, Charset.defaultCharset());
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
        valuesPressure.clear();
        valuesPressureCursor.clear();
        i = 0;
        j = 0;
        chartPressure.setLineChartData(dataPressure);

        valuesFlow.clear();
        valuesFlowCursor.clear();
        chartFlow.setLineChartData(dataFlow);
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