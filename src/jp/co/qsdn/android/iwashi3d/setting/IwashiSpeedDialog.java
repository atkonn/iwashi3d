package jp.co.qsdn.android.iwashi3d.setting;

import android.app.Dialog;

import android.content.Context;
import android.content.res.Resources;

import android.preference.DialogPreference;
import android.preference.PreferenceManager;

import android.util.AttributeSet;
import android.util.Log;

import android.view.View;

import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SeekBar;
import android.widget.TextView;

import jp.co.qsdn.android.iwashi3d.R;


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
      seekBar.setProgress(Prefs.getInstance(getContext()).getIwashiSpeed());
      final TextView nowSpeedView = (TextView)view.findViewById(R.id.dialog_now_speed);

      Resources res = view.getResources();
      final String label = res.getString(R.string.dialog_iwashi_speed_label);

      nowSpeedView.setText(label + seekBar.getProgress());
      seekBar.setOnSeekBarChangeListener(
        new OnSeekBarChangeListener() {
          // トラッキング開始時に呼び出されます
          @Override
          public void onStartTrackingTouch(SeekBar seekBar) {
            nowSpeedView.setText(label + (seekBar.getProgress() + MIN));
          }
          // トラッキング中に呼び出されます
          @Override
          public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
            nowSpeedView.setText(label + (seekBar.getProgress() + MIN));
          }
          // トラッキング終了時に呼び出されます
          @Override
          public void onStopTrackingTouch(SeekBar seekBar) {
            nowSpeedView.setText(label + (seekBar.getProgress() + MIN));
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


