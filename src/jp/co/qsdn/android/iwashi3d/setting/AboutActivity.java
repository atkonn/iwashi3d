package jp.co.qsdn.android.iwashi3d.setting;

import android.app.Activity;

import android.os.Bundle;

import android.view.View;

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import jp.co.qsdn.android.iwashi3d.R;

public class AboutActivity extends Activity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.about);

//    TextView textView = (TextView) findViewById(R.id.about_text1);
//    textView.setText("テスト");
//    String text = textView.getText().toString();
//    Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    {
      final AboutActivity __this = this;
      Button button = (Button)findViewById(R.id.about_button_ok);
      button.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Button button = (Button) v;
          __this.finish();
        }
      });
    }
  }
}
