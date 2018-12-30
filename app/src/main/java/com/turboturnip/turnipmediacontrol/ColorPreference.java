package com.turboturnip.turnipmediacontrol;

import android.content.Context;
import android.graphics.Color;
import android.preference.Preference;
import android.support.annotation.Nullable;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class ColorPreference extends DialogPreference {

    public ColorPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        customSetup();
    }
    public ColorPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        customSetup();
    }
    public ColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        customSetup();
    }
    public ColorPreference(Context context) {
        super(context);
        customSetup();
    }
    private void customSetup(){
        setWidgetLayoutResource(R.layout.color_square);
    }

    @Override
    public int getDialogLayoutResource() {
        return R.layout.color_picker_dialog;
    }

    Integer value;
    public boolean getOptional(){
        return false;
    }
    public Integer getValue() {
        if (getOptional()) return null;
        return value;
    }
    public void setValue() {
        if (!getOptional()) {
            value = Color.argb(255, 0, 0, 0);
            //persist
        } //else
            //persistBoolean(true);
        persistInt(value);
    }
    public void setValue(int value) {
        this.value = value;

        //persistBoolean(false);
        persistInt(value);
    }
}
