package jp.co.qsdn.android.atlantis;

import android.app.Dialog;

import android.content.Context;

import android.preference.DialogPreference;
import android.preference.PreferenceManager;

import android.util.AttributeSet;
import android.util.Log;

import android.view.View;

import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SeekBar;
import android.widget.TextView;


public class IwashiCountDialog 
  extends DialogPreference {
  private static final String TAG = IwashiCountDialog.class.getName();
  private static SeekBar seekBar = null;

  public IwashiCountDialog(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public IwashiCountDialog(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  public View onCreateDialogView() {
    View view = super.onCreateDialogView();
    if (view != null) {
      seekBar = (SeekBar)view.findViewById(R.id.seek);
      seekBar.setProgress(Prefs.getInstance(getContext()).getIwashiCount());
      final TextView nowCountView = (TextView)view.findViewById(R.id.dialog_now_count);
      nowCountView.setText("鰯の数:" + seekBar.getProgress());
      seekBar.setOnSeekBarChangeListener(
        new OnSeekBarChangeListener() {
          // トラッキング開始時に呼び出されます
          @Override
          public void onStartTrackingTouch(SeekBar seekBar) {
            Log.v("onStartTrackingTouch()", String.valueOf(seekBar.getProgress()));
            nowCountView.setText("鰯の数:" + seekBar.getProgress());
          }
          // トラッキング中に呼び出されます
          @Override
          public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
            Log.v("onProgressChanged()", String.valueOf(progress) + ", " + String.valueOf(fromTouch));
            nowCountView.setText("鰯の数:" + seekBar.getProgress());
          }
          // トラッキング終了時に呼び出されます
          @Override
          public void onStopTrackingTouch(SeekBar seekBar) {
            Log.v("onStopTrackingTouch()", String.valueOf(seekBar.getProgress()));
            nowCountView.setText("鰯の数:" + seekBar.getProgress());
          }
      });
    }
    return view;
  }

  @Override
  protected void onDialogClosed (boolean positiveResult) {
    Log.d(TAG, "start onDialogClosed(" + positiveResult + ")");
    if (positiveResult) {
      if (seekBar != null) {
        Log.d(TAG, "鰯数:[" + seekBar.getProgress() + "]");
        Prefs.getInstance(getContext()).setIwashiCount(seekBar.getProgress());
        seekBar = null;
      }
    }
    super.onDialogClosed(positiveResult);
    Log.d(TAG, "end onDialogClosed(" + positiveResult + ")");
  }

}

