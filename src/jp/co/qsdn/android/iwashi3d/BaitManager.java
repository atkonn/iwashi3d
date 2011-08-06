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
package jp.co.qsdn.android.iwashi3d;


import java.util.Stack;
import android.util.Log;

/**
 */
public class BaitManager {
  private static final String TAG = BaitManager.class.getName();
  private static final boolean _debug = false;
  private int max_count = 100;
  private int nowCount = 0;
  private Stack<Bait> stack = new Stack<Bait>();
  public void addBait(float x, float y, float z) {
    synchronized (this) {
      if (nowCount < max_count) {
        if (_debug) Log.d(TAG,"Add bait to ("+x+","+y+","+z+")");
        stack.push(new Bait(x,y,z));
        nowCount++;
      }
    }
  }

  public Bait getBait() {
    synchronized (this) {
      if (nowCount!=0) {
        return stack.get(0);
      }
      return null;
    }
  }

  public void eat(Bait bait) {
    synchronized (this) {
      if (stack.remove(bait)) {
        nowCount--;
        if (_debug) Log.d(TAG,"eaten bait ("+bait.getX()+","+bait.getY()+","+bait.getZ()+")");
      }
    }
  }
}
