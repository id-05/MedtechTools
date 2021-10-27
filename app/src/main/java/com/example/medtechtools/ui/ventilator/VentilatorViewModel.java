package com.example.medtechtools.ui.ventilator;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class VentilatorViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public VentilatorViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is ventilator fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}