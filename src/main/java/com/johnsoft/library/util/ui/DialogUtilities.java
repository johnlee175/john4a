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

import com.johnsoft.library.R;
import com.johnsoft.library.template.BaseApplication;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;
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

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context, R.style.AppAlertDialogTheme);
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

    public static void showSelectDialog(String title, String message, String[] items,
                                        final OnItemSelectedListener listener) {
        final Context appContext = BaseApplication.getApplication();
        final Handler mainHandler = BaseApplication.getApplication().getMainHandler();
        final Resources resources = appContext.getResources();
        final DialogInterface.OnClickListener l = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    try {
                        if (listener != null) {
                            final ListView listView = ((AlertDialog) dialog).getListView();
                            int position = listView.getCheckedItemPosition();
                            String item = (String) listView.getAdapter().getItem(position);
                            listener.onItemSelected(item, position);
                        }
                    } finally {
                        dialog.dismiss();
                    }
                } else {
                    dialog.cancel();
                }
            }
        };
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(appContext,
                R.style.AppAlertDialogTheme);
        alertDialogBuilder.setTitle(title + " - " + message)
                .setSingleChoiceItems(items, 0, null)
                .setPositiveButton(resources.getString(R.string.ok), l)
                .setNegativeButton(resources.getString(R.string.cancel), l);
        final AlertDialog alertDialog = alertDialogBuilder.create();
        final Window window = alertDialog.getWindow();
        if (window != null) {
            window.setType(WindowManager.LayoutParams.TYPE_TOAST);
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                alertDialog.show();
            }
        });
    }

    public interface OnItemSelectedListener {
        void onItemSelected(String item, int position);
    }
}
