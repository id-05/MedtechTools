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
import android.graphics.DashPathEffect;
import android.graphics.drawable.Drawable;
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
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IFillFormatter;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.UUID;

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
    LineChart chartPres;
    LineChart chartFlow;

    int i = 0;
    int j = 0;
    int pointsPressure = 12000;
    int pointsFlow = 12000;
    int countVisiblePoints = 2000;
    ArrayList<Byte> bufList = new ArrayList<>();
    ActionMenuItemView imageSearchBluetooth;
    UUID deviceUUID;
    SharedPreferences sPref;
    TextView statusString;
    String visibleDevName;
    LineDataSet set;//Pres;
    LineDataSet setFlow;
    ArrayList<Entry> valuesPres = new ArrayList<>();
    ArrayList<Entry> valuesFlow = new ArrayList<>();
    LineData dataPres, dataFlow;

    Timer timer = new Timer();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        //ventilatorViewModel = new ViewModelProvider(this).get(VentilatorViewModel.class);
        View root = inflater.inflate(R.layout.fragment_ventilator, container, false);

        statusString = root.findViewById(R.id.statusString);
        loadText();
        statusString.setText(visibleDevName);

        chartPres = root.findViewById(R.id.chartPres);
        prepareChat(chartPres, dataPres, valuesPres,0,pointsPressure,0,4);
        chartPres.notifyDataSetChanged();
        chartPres.invalidate();

        ///////////////////////////////////////////
        chartFlow = root.findViewById(R.id.chartFlow);
        prepareChat(chartFlow, dataFlow, valuesFlow,0,pointsFlow,-2,2);
        chartFlow.notifyDataSetChanged();
        chartFlow.invalidate();

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

    public void prepareChat(LineChart chart, LineData data, ArrayList<Entry> values, float xmin, float xmax, float ymin, float ymax){
        chart.setBackgroundColor(Color.WHITE);
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDrawGridBackground(false);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);

        Legend l = chart.getLegend();
        l.setForm(Legend.LegendForm.NONE);

        XAxis xAxisPres;
        xAxisPres = chart.getXAxis();
        xAxisPres.setAxisMaximum(xmax);
        xAxisPres.setAxisMinimum(xmin);
        xAxisPres.enableGridDashedLine(10f, 10f, 0f);

        YAxis yAxisPres;
        yAxisPres = chart.getAxisLeft();
        chart.getAxisRight().setEnabled(false);
        yAxisPres.enableGridDashedLine(10f, 10f, 0f);
        yAxisPres.setAxisMaximum(ymax);
        yAxisPres.setAxisMinimum(ymin);

        set = new LineDataSet(values,null);
        set.setDrawIcons(false);
        set.setColor(Color.BLACK);
        set.setCircleColor(Color.BLACK);
        set.setLineWidth(1f);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setFormLineWidth(1f);
        set.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
        set.setFormSize(15.f);
        set.setValueTextSize(9f);
        set.enableDashedHighlightLine(10f, 5f, 0f);
        set.setDrawFilled(true);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setFillFormatter(new IFillFormatter() {
            @Override
            public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
                return chart.getAxisLeft().getAxisMinimum();
            }
        });

        if (Utils.getSDKInt() >= 18) {
            Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.fade_color);
            set.setFillDrawable(drawable);
        } else {
            set.setFillColor(Color.BLACK);
        }

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(set);
        data = new LineData(dataSets);
        chart.setData(data);
        set.setValues(values);
        set.notifyDataSetChanged();
        chart.getData().notifyDataChanged();

        chart.setVisibleXRangeMaximum(countVisiblePoints);
        chart.moveViewToX(data.getEntryCount());
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
        //Log.d(TAG, "connected: Starting.");
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        public ConnectedThread(BluetoothSocket socket) {
            //Log.d(TAG, "ConnectedThread: Starting.");

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
                        double valuePres = (((( bufList.get(0) & 0xff ) << 8)) | ( bufList.get(1) & 0xff )) * 0.000805;
                        double valueFlow = (((( bufList.get(2) ) << 8)) | ( bufList.get(3) & 0xff )) * 0.000805;
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                if (valuesPres.size() > pointsPressure - 1 ) {
                                    valuesPres.clear();
                                    chartPres.moveViewToX(1);
                                }else {
                                    valuesPres.add(new Entry(i,(float)valuePres));
                                    chartPres.invalidate();
                                }

                                if(i>countVisiblePoints){
                                    chartPres.notifyDataSetChanged();
                                    chartPres.setVisibleXRangeMaximum(countVisiblePoints);
                                    chartPres.moveViewToX(i-countVisiblePoints);
                                    chartPres.invalidate();
                                }

                                if (valuesFlow.size() > pointsFlow - 1 ) {
                                    valuesFlow.clear();
                                    chartFlow.moveViewToX(1);
                                }else {
                                    valuesFlow.add(new Entry(i,(float)valueFlow));
                                    chartFlow.invalidate();
                                }

                                if(i>countVisiblePoints){
                                    chartFlow.notifyDataSetChanged();
                                    chartFlow.setVisibleXRangeMaximum(countVisiblePoints);
                                    chartFlow.moveViewToX(i-countVisiblePoints);
                                    chartFlow.invalidate();
                                }
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

        i = 0;
        j = 0;

        valuesPres.clear();
        chartPres.invalidate();

        valuesFlow.clear();
        chartFlow.invalidate();
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