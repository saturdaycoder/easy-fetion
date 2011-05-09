package com.saturdaycoder.easyfetion;

import android.util.Log;

public class Debugger {
  private static final String TAG="EasyFetion";
  public static void v(String s) {
    Log.v(TAG, s);
  }

  public static void d(String s) {
    Log.d(TAG, s);
  }
  public static void i(String s) {
    Log.i(TAG, s);
  }
  public static void e(String s) {
    Log.e(TAG, s);
  }
}
