package com.github.warren_bank.mms_automatic_forwarding.event;

import com.github.warren_bank.mms_automatic_forwarding.data_model.Contacts;
import com.github.warren_bank.mms_automatic_forwarding.data_model.Message;
import com.github.warren_bank.mms_automatic_forwarding.data_model.Preferences;
import com.github.warren_bank.mms_automatic_forwarding.data_model.RecipientListItem;

import com.google.android.mms.pdu_alt.GenericPdu;
import com.google.android.mms.pdu_alt.NotificationInd;
import com.google.android.mms.pdu_alt.PduHeaders;
import com.google.android.mms.pdu_alt.PduParser;
import com.google.android.mms.pdu_alt.PduPersister;
import com.google.android.mms.pdu_alt.RetrieveConf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.util.Log;

import java.util.ArrayList;

public class MMSReceiver extends BroadcastReceiver {
  private static final String TAG          = "MMSReceiver";
  private static final String MMS_RECEIVED = "android.provider.Telephony.WAP_PUSH_RECEIVED";

  public void onReceive(Context context, Intent intent) {
    try {
      if (!Preferences.isEnabled(context))
        return;

      if (!intent.getAction().equals(MMS_RECEIVED))
        return;

      byte[] pushData = intent.getByteArrayExtra("data");
      if ((pushData == null) || (pushData.length == 0))
        return;

      PduParser  parser = new PduParser(pushData);
      GenericPdu pdu    = parser.parse();

      if (pdu == null)
        return;

      switch(pdu.getMessageType()) {
        case PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND:
          if (pdu instanceof NotificationInd) {
            onReceiveNotificationInd(context, (NotificationInd) pdu);
          }
          break;
        case PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF:
          // my understanding: this PDU message type will never be broadcast
          if (pdu instanceof RetrieveConf) {
            onReceiveRetrieveConf(context, (RetrieveConf) pdu);
          }
          break;
      }
    }
    catch (Exception e) {}
  }

  private void onReceiveNotificationInd(final Context context, final NotificationInd pdu) {
    // prevent: android.os.NetworkOnMainThreadException
    new Thread(new Runnable() {
      private static final int MAX_TRIES = 60;
      private static final int SLEEP_MS  = 1000;

      @Override
      public void run() {
        Message msg = getMessage(context, pdu, false);
        if (msg == null) return;

        // begin database lookup from Telephony ContentProvider

        final String tr_id = new String(pdu.getTransactionId());
        final String ct_l  = new String(pdu.getContentLocation());

        final String timestamp = Integer.toString(
          (int)(System.currentTimeMillis() / 1000) - 2
        );

        for (int i = 0; i < MAX_TRIES; i++) {
          Cursor cursor = null;

          if ((cursor == null) || (cursor.getCount() == 0) || !cursor.moveToFirst()) {
            if (!tr_id.isEmpty() && !ct_l.isEmpty()) {
              cursor = context.getContentResolver().query(
                Mms.Inbox.CONTENT_URI,
                new String[]{Mms._ID},
                (Mms.TRANSACTION_ID + "=? AND " + Mms.CONTENT_LOCATION + "=? AND " + Mms._ID + " IS NOT NULL"),
                new String[]{tr_id, ct_l},
                null
              );
            }
          }

          if ((cursor == null) || (cursor.getCount() == 0) || !cursor.moveToFirst()) {
            if (!timestamp.isEmpty()) {
              cursor = context.getContentResolver().query(
                Mms.Inbox.CONTENT_URI,
                new String[]{Mms._ID},
                (Mms.DATE + ">=? AND " + Mms._ID + " IS NOT NULL"),
                new String[]{timestamp},
                Mms.DEFAULT_SORT_ORDER
              );
            }
          }

          if ((cursor == null) || (cursor.getCount() == 0) || !cursor.moveToFirst()) {
            try {
              Thread.sleep(SLEEP_MS);
              continue;
            }
            catch(InterruptedException e) {
              break;
            }
          }
          else {
            try {
              String msgId = cursor.getString(cursor.getColumnIndex(Mms._ID));
              cursor.close();

              if ((msgId != null) && !msgId.isEmpty()) {
                Uri messageUri  = Uri.parse(Mms.Inbox.CONTENT_URI + "/" + msgId);
                GenericPdu pdu2 = PduPersister.getPduPersister(context).load(messageUri);

                if (pdu2 instanceof RetrieveConf) {
                  onReceiveRetrieveConf(context, (RetrieveConf) pdu2);
                }
              }
              else {
                // should never happen
                Thread.sleep(SLEEP_MS);
                continue;
              }
            }
            catch(Exception e) {
            }
            break;
          }
        }
      }
    }).start();
  }

  private void onReceiveRetrieveConf(final Context context, final RetrieveConf pdu) {
    // prevent: android.os.NetworkOnMainThreadException
    new Thread(new Runnable() {
      @Override
      public void run() {
        Message msg = getMessage(context, pdu);
        if (msg == null) return;

        MMSSender.forward(context, msg, pdu);
      }
    }).start();
  }

  private Message getMessage(Context context, GenericPdu pdu) {
    return getMessage(context, pdu, true);
  }

  private Message getMessage(Context context, GenericPdu pdu, boolean getContactName) {
    String sender = pdu.getFrom().getString();
    Log.i(TAG, "MMS received.\nfrom: " + sender);

    ArrayList<RecipientListItem> listItems = Preferences.getRecipientListItems(context);
    if (listItems.isEmpty())
      return null;

    ArrayList<String> recipients = RecipientListItem.match(listItems, sender);
    if ((recipients == null) || recipients.isEmpty())
      return null;

    String sender_contact_name = getContactName
      ? Contacts.getContactName(context, sender)
      : null;

    return new Message(recipients, sender, sender_contact_name);
  }
}
