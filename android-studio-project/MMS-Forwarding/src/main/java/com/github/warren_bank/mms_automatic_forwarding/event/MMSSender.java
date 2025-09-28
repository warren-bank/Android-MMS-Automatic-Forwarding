package com.github.warren_bank.mms_automatic_forwarding.event;

import com.github.warren_bank.mms_automatic_forwarding.R;
import com.github.warren_bank.mms_automatic_forwarding.data_model.Message;

import com.klinker.android.send_message.Utils;

import com.android.mms.MmsConfig;
import com.android.mms.dom.smil.parser.SmilXmlSerializer;

import com.google.android.mms.ContentType;
import com.google.android.mms.pdu_alt.CharacterSets;
import com.google.android.mms.pdu_alt.EncodedStringValue;
import com.google.android.mms.pdu_alt.PduBody;
import com.google.android.mms.pdu_alt.PduComposer;
import com.google.android.mms.pdu_alt.PduHeaders;
import com.google.android.mms.pdu_alt.PduPart;
import com.google.android.mms.pdu_alt.PduUtils;
import com.google.android.mms.pdu_alt.RetrieveConf;
import com.google.android.mms.pdu_alt.SendReq;
import com.google.android.mms.smil.SmilHelper;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

public final class MMSSender {
  private static final String TAG = "MMSSender";

  public static void forward(Context context, Message msg, RetrieveConf pdu) {
    try {
      String preface = context.getString(R.string.mms_preface_heading);
      if (!TextUtils.isEmpty(msg.sender_contact_name)) {
        preface += "\n  " + msg.sender_contact_name;
      }
      preface += "\n  " + msg.sender;

      EncodedStringValue[] encTo = EncodedStringValue.encodeStrings(
        msg.recipients.toArray(new String[0])
      );

      EncodedStringValue encFrom = new EncodedStringValue(
        Utils.getMyPhoneNumber(context)
      );

      PduHeaders pduHeaders = PduUtils.getPduHeaders(pdu);
      PduBody    pduBody    = pdu.getBody();

      remove_old_SMIL_part(pduBody);
      add_preface_TEXT_part(pduBody, preface + "\n");
      add_new_SMIL_part(pduBody);

      byte[] pduBytes = compose_new_send_request(context, pduHeaders, pduBody, encTo, encFrom);
      if ((pduBytes == null) || (pduBytes.length == 0))
        return;

      send_MMS(context, pduBytes);
    }
    catch(Exception e) {}
  }

  private static void remove_old_SMIL_part(PduBody pduBody) {
    int partsNum = pduBody.getPartsNum();
    byte[] smil_CT = ContentType.APP_SMIL.getBytes();
    byte[] part_CT;
    PduPart pduPart;
    for (int i=0; i < partsNum; i++) {
      pduPart = pduBody.getPart(i);
      part_CT = pduPart.getContentType();

      if (Arrays.equals(smil_CT, part_CT)) {
        pduBody.removePart(i);
        break;
      }
    }
  }

  private static void add_preface_TEXT_part(PduBody pduBody, String preface) {
    byte[] name = ("fwd").getBytes();
    byte[] mime = ("text/plain").getBytes();
    byte[] data = preface.getBytes();

    PduPart pduPart = new PduPart();
    pduPart.setName(name);
    pduPart.setContentId(name);
    pduPart.setContentLocation(name);
    pduPart.setContentType(mime);
    pduPart.setCharset(CharacterSets.UTF_8);
    pduPart.setData(data);

    pduBody.addPart(0, pduPart);
  }

  private static void add_new_SMIL_part(PduBody pduBody) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SmilXmlSerializer.serialize(
      SmilHelper.createSmilDocument(pduBody),
      out
    );

    PduPart pduPart = new PduPart();
    pduPart.setContentId("smil".getBytes());
    pduPart.setContentLocation("smil.xml".getBytes());
    pduPart.setContentType(ContentType.APP_SMIL.getBytes());
    pduPart.setData(out.toByteArray());

    pduBody.addPart(0, pduPart);
  }

  private static byte[] compose_new_send_request(Context context, PduHeaders pduHeaders, PduBody pduBody, EncodedStringValue[] encTo, EncodedStringValue encFrom) throws Exception {
    SendReq sendRequest = PduUtils.getSendReq(pduHeaders, pduBody);

    sendRequest.setMessageType(PduHeaders.MESSAGE_TYPE_SEND_REQ);
    sendRequest.setTransactionId(
        ("T" + Long.toHexString(System.currentTimeMillis())).getBytes()
    );
    sendRequest.setTo(encTo);
    sendRequest.setFrom(encFrom);

    final PduComposer pduComposer = new PduComposer(context, sendRequest);
    return pduComposer.make();
  }

  // based on: com.klinker.android.send_message.Transaction.sendMmsThroughSystem()
  //     https://github.com/klinker41/android-smsmms/blob/master/library/src/main/java/com/klinker/android/send_message/Transaction.java#L649-L731
  private static void send_MMS(Context context, byte[] pduBytes) {
    try {
      Intent intent = SmsManagerReceiver.getIntent(context);

      int flags = PendingIntent.FLAG_CANCEL_CURRENT;
      if (Build.VERSION.SDK_INT >= 23)
        flags |= PendingIntent.FLAG_IMMUTABLE;
      PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags);

      String fileName = SmsManagerReceiver.getFileName(intent);
      File sendFile = new File(context.getCacheDir(), fileName);
      Uri writerUri = (new Uri.Builder())
        .authority(context.getPackageName() + ".MmsFileProvider")
        .path(fileName)
        .scheme(ContentResolver.SCHEME_CONTENT)
        .build();

      FileOutputStream writer = null;
      Uri contentUri = null;
      try {
        writer = new FileOutputStream(sendFile);
        writer.write(pduBytes);
        contentUri = writerUri;
      }
      catch (final IOException e) {
        Log.e(TAG, "Error writing send file", e);
      }
      finally {
        if (writer != null) {
          try {
            writer.close();
          }
          catch (IOException e) {
          }
        }
      }

      if (contentUri != null) {
        Bundle configOverrides = new Bundle();
        configOverrides.putBoolean(SmsManager.MMS_CONFIG_GROUP_MMS_ENABLED, true);
        String httpParams = MmsConfig.getHttpParams();
        if (!TextUtils.isEmpty(httpParams)) {
          configOverrides.putString(SmsManager.MMS_CONFIG_HTTP_PARAMS, httpParams);
        }
        configOverrides.putInt(SmsManager.MMS_CONFIG_MAX_MESSAGE_SIZE, MmsConfig.getMaxMessageSize());

        SmsManager.getDefault().sendMultimediaMessage(context, contentUri, null, configOverrides, pendingIntent);
      }
      else {
        Log.e(TAG, "Error writing sending Mms");
        try {
          pendingIntent.send(SmsManager.MMS_ERROR_IO_ERROR);
        }
        catch (PendingIntent.CanceledException ex) {
          Log.e(TAG, "Mms pending intent cancelled?", ex);
        }
      }
    }
    catch (Exception e) {
      Log.e(TAG, "error using system sending method", e);
    }
  }

  public static class SmsManagerReceiver extends BroadcastReceiver {
    private static final String ACTION_SMS_SENT = ".SMS_SENT";
    private static final String EXTRA_FILE_NAME = "FILE_NAME";

    private static String getAction(Context context) {
      return context.getPackageName() + ACTION_SMS_SENT;
    }

    private static Intent getIntent(Context context) {
      Intent intent = new Intent(getAction(context));
      intent.setClass(context, SmsManagerReceiver.class);

      String fileName = "send." + String.valueOf(Math.abs(new Random().nextLong())) + ".dat";
      intent.putExtra(EXTRA_FILE_NAME, fileName);

      return intent;
    }

    private static String getFileName(Intent intent) {
      return intent.getStringExtra(EXTRA_FILE_NAME);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
      if (!intent.getAction().equals(getAction(context)))
        return;

      try {
        String fileName = getFileName(intent);
        File sentFile = new File(context.getCacheDir(), fileName);
        sentFile.delete();
      }
      catch(Exception e) {
      }
    }
  }
}
