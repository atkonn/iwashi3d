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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import jp.co.qsdn.android.jinbei3d.R;

public class ChangeCameraModeDialog 
  extends DialogPreference {
  private static final boolean _debug = false;
  private static final String TAG = ChangeCameraModeDialog.class.getName();
  private static RadioGroup radioGroup = null;

  public ChangeCameraModeDialog(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ChangeCameraModeDialog(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  public View onCreateDialogView() {
    View view = super.onCreateDialogView();
    if (view != null) {
      radioGroup = (RadioGroup)view.findViewById(R.id.radio_group);
Log.d(TAG, "onCreateDialogView id:[" + Prefs.getInstance(getContext()).getCameraMode() + "]");
      radioGroup.check(Prefs.getInstance(getContext()).getCameraMode());

      Resources res = view.getResources();
      final RadioGroup __radioGroup = radioGroup;
      Button button = (Button)view.findViewById(R.id.to_default_button);
      button.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          __radioGroup.check(Prefs.DEFAULT_CAMERA_MODE);
        }
      });
    }
    return view;
  }

  @Override
  protected void onDialogClosed (boolean positiveResult) {
    if (_debug) Log.d(TAG, "start onDialogClosed(" + positiveResult + ")");
    if (positiveResult) {
      if (radioGroup != null) {
        Prefs.getInstance(getContext()).setCameraMode(radioGroup.getCheckedRadioButtonId());
        radioGroup = null;
      }
    }
    super.onDialogClosed(positiveResult);
    if (_debug) Log.d(TAG, "end onDialogClosed(" + positiveResult + ")");
  }

}


