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
