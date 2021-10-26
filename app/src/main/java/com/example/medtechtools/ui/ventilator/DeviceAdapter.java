package com.example.medtechtools.ui.ventilator;

import android.content.Context;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class DeviceAdapter extends ArrayAdapter {
    public DeviceAdapter(@NonNull Context context, ArrayList<String> list) {
        super(context, 0);
        //super(context, list);
    }
}
