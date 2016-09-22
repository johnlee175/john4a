/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package com.johnsoft.library.util.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;

/**
 * @author John Kenrinus Lee
 * @version 2016-09-22
 */
public final class DialogUtilities {
    private DialogUtilities() {
    }

    public static void showNumberPickerDialog(Context context, int minValue, int maxValue, final int currentValue,
                                              final NumberPicker.OnValueChangeListener onValueChangeListener) {
        final NumberPicker numberPicker = new NumberPicker(context);
        numberPicker.setMinValue(minValue);
        numberPicker.setMaxValue(maxValue);
        numberPicker.setValue(currentValue);

        final int wrapContent = RelativeLayout.LayoutParams.WRAP_CONTENT;
        RelativeLayout.LayoutParams numberPickerParams = new RelativeLayout.LayoutParams(wrapContent, wrapContent);
        numberPickerParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

        final RelativeLayout relativeLayout = new RelativeLayout(context);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(50, 50);
        relativeLayout.setLayoutParams(params);
        relativeLayout.addView(numberPicker,numberPickerParams);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setTitle("Pick A Number");
        alertDialogBuilder.setView(relativeLayout);
        alertDialogBuilder
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (onValueChangeListener != null) {
                            onValueChangeListener.onValueChange(numberPicker, currentValue, numberPicker.getValue());
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
