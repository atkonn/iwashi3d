/*
 * Copyright (C) 2011 QSDN,Inc.
 * Copyright (C) 2011 Atsushi Konno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.co.qsdn.android.jinbei3d.setting;

import android.app.Dialog;

import android.content.Context;
import android.content.res.Resources;

import android.preference.DialogPreference;
import android.preference.PreferenceManager;

import android.util.AttributeSet;
import android.util.Log;

import android.view.View;

import android.widget.Button;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SeekBar;
import android.widget.TextView;

import jp.co.qsdn.android.jinbei3d.R;


public class IwashiSpeedDialog 
  extends DialogPreference {
  private static final boolean _debug = false;
  private static final String TAG = IwashiSpeedDialog.class.getName();
  private static SeekBar seekBar = null;
  private static final int MIN = 20;

  public IwashiSpeedDialog(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public IwashiSpeedDialog(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  public View onCreateDialogView() {
    View view = super.onCreateDialogView();
    if (view != null) {
      seekBar = (SeekBar)view.findViewById(R.id.seek);
      seekBar.setProgress(Prefs.getInstance(getContext()).getIwashiSpeed() - MIN);
      final TextView nowSpeedView = (TextView)view.findViewById(R.id.dialog_now_speed);

      Resources res = view.getResources();
      final String label = res.getString(R.string.dialog_iwashi_speed_label);

      nowSpeedView.setText(label + (seekBar.getProgress() + MIN));
      seekBar.setOnSeekBarChangeListener(
        new OnSeekBarChangeListener() {
          @Override
          public void onStartTrackingTouch(SeekBar seekBar) {
            nowSpeedView.setText(label + (seekBar.getProgress() + MIN));
          }
          @Override
          public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
            nowSpeedView.setText(label + (seekBar.getProgress() + MIN));
          }
          @Override
          public void onStopTrackingTouch(SeekBar seekBar) {
            nowSpeedView.setText(label + (seekBar.getProgress() + MIN));
          }
      });
      final SeekBar __seekBar = seekBar;
      Button button = (Button)view.findViewById(R.id.to_default_button);
      button.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          __seekBar.setProgress(Prefs.DEFAULT_IWASHI_SPEED - MIN);
          nowSpeedView.setText(label + (__seekBar.getProgress() + MIN));
        }
      });
    }
    return view;
  }

  @Override
  protected void onDialogClosed (boolean positiveResult) {
    if (_debug) Log.d(TAG, "start onDialogClosed(" + positiveResult + ")");
    if (positiveResult) {
      if (seekBar != null) {
        if (_debug) Log.d(TAG, "スピード:[" + (seekBar.getProgress() + MIN) + "]");
        Prefs.getInstance(getContext()).setIwashiSpeed((seekBar.getProgress() + MIN));
        seekBar = null;
      }
    }
    super.onDialogClosed(positiveResult);
    if (_debug) Log.d(TAG, "end onDialogClosed(" + positiveResult + ")");
  }

}


