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
package jp.co.qsdn.android.iwashi3d.setting;

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

import jp.co.qsdn.android.iwashi3d.R;

public class ChangeCameraDialog 
  extends DialogPreference {
  private static final boolean _debug = false;
  private static final String TAG = ChangeCameraDialog.class.getName();
  private static SeekBar seekBar = null;

  public ChangeCameraDialog(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ChangeCameraDialog(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  public View onCreateDialogView() {
    View view = super.onCreateDialogView();
    if (view != null) {
      seekBar = (SeekBar)view.findViewById(R.id.distance);
      seekBar.setProgress(Prefs.getInstance(getContext()).getCameraDistance());
      final TextView nowCountView = (TextView)view.findViewById(R.id.dialog_now_count);

      Resources res = view.getResources();
      final String label = res.getString(R.string.dialog_camera_distance_label);

      nowCountView.setText(label + seekBar.getProgress());
      seekBar.setOnSeekBarChangeListener(
        new OnSeekBarChangeListener() {
          @Override
          public void onStartTrackingTouch(SeekBar seekBar) {
            nowCountView.setText(label + seekBar.getProgress());
          }
          @Override
          public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
            nowCountView.setText(label + seekBar.getProgress());
          }
          @Override
          public void onStopTrackingTouch(SeekBar seekBar) {
            nowCountView.setText(label + seekBar.getProgress());
          }
      });
      final SeekBar __seekBar = seekBar;
      Button button = (Button)view.findViewById(R.id.to_default_button);
      button.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          __seekBar.setProgress(Prefs.DEFAULT_CAMERA_DISTANCE);
          nowCountView.setText(label + (__seekBar.getProgress()));
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
        Prefs.getInstance(getContext()).setCameraDistance(seekBar.getProgress());
        seekBar = null;
      }
    }
    super.onDialogClosed(positiveResult);
    if (_debug) Log.d(TAG, "end onDialogClosed(" + positiveResult + ")");
  }

}


