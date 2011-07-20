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
package jp.co.qsdn.android.jinbei3d.model;

import javax.microedition.khronos.opengles.GL10;


public interface Model {
  float getX();
  float getY();
  float getZ();
  float getSize();

  float getDirectionX();
  float getDirectionY();
  float getDirectionZ();

  void calc();
  void draw(GL10 gl10);

  float[] getDistances();
  float getSpeed();

  float getX_angle();
  float getY_angle();
}
