package com.github.warren_bank.mms_automatic_forwarding.security_model;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import java.util.ArrayList;

public final class RuntimePermissions {
  private static final int REQUEST_CODE = 0;

  protected static String[] getMissingPermissions(Activity activity, String[] permissions_all) {
    ArrayList<String> permissions_req = new ArrayList<String>();

    for (String permission_name : permissions_all) {
      if (activity.checkSelfPermission(permission_name) != PackageManager.PERMISSION_GRANTED) {
        permissions_req.add(permission_name);
      }
    }

    return permissions_req.isEmpty()
      ? null
      : permissions_req.toArray(new String[0]);
  }

  public static boolean isEnabled(Activity activity) {
    if (Build.VERSION.SDK_INT < 23)
      return true;

    final String[] permissions_all = new String[]{ "android.permission.SEND_SMS", "android.permission.READ_SMS", "android.permission.WRITE_SMS", "android.permission.RECEIVE_SMS", "android.permission.RECEIVE_MMS" };
    final String[] permissions_req = RuntimePermissions.getMissingPermissions(activity, permissions_all);

    if (permissions_req == null)
      return true;

    activity.requestPermissions(permissions_req, REQUEST_CODE);
    return false;
  }

  public static void onRequestPermissionsResult (Activity activity, int requestCode, String[] permissions, int[] grantResults) {
    if (requestCode != REQUEST_CODE)
      return;

    if (grantResults.length == 0)
      return;

    for (int result : grantResults) {
      if (result != PackageManager.PERMISSION_GRANTED) return;
    }

    if (canWrite(activity))
      activity.recreate();
  }

  private static boolean canWrite(Activity activity) {
    if (Settings.System.canWrite(activity))
      return true;

    try {
      Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
      intent.setData(
        Uri.parse("package:" + activity.getPackageName())
      );
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

      activity.startActivityForResult(intent, REQUEST_CODE);
      return false;
    }
    catch (Exception e) {
      return true;
    }
  }

  public static void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
    if (requestCode != REQUEST_CODE)
      return;

    if (canWrite(activity))
      activity.recreate();
  }

}
