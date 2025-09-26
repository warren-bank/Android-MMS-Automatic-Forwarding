package com.github.warren_bank.mms_automatic_forwarding.event;

import com.github.warren_bank.mms_automatic_forwarding.data_model.Contacts;
import com.github.warren_bank.mms_automatic_forwarding.data_model.Message;
import com.github.warren_bank.mms_automatic_forwarding.data_model.Preferences;
import com.github.warren_bank.mms_automatic_forwarding.data_model.RecipientListItem;

import com.klinker.android.send_message.Utils;

import com.android.mms.service_alt.MmsNetworkManager;
import com.android.mms.service_alt.MyDownloadRequest;
import com.android.mms.service_alt.MyRequestManager;

import com.google.android.mms.pdu_alt.GenericPdu;
import com.google.android.mms.pdu_alt.NotificationInd;
import com.google.android.mms.pdu_alt.PduHeaders;
import com.google.android.mms.pdu_alt.PduParser;
import com.google.android.mms.pdu_alt.RetrieveConf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;

public class MMSReceiver extends BroadcastReceiver implements MyDownloadRequest.ResponseListener {
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

  @Override
  public void onResponse(Context context, RetrieveConf pdu) {
    onReceiveRetrieveConf(context, pdu);
  }

  private void onReceiveNotificationInd(final Context context, final NotificationInd pdu) {
    // prevent: android.os.NetworkOnMainThreadException
    new Thread(new Runnable() {
      @Override
      public void run() {
        Message msg = getMessage(context, pdu, false);
        if (msg == null) return;

        String locationUrl = new String(pdu.getContentLocation());
        if ((locationUrl == null) || locationUrl.isEmpty()) return;

        // begin download from MMS Proxy with callback

        int subId = Utils.getDefaultSubscriptionId();

        MyRequestManager requestManager = new MyRequestManager();
        MmsNetworkManager manager = new MmsNetworkManager(context, subId);

        MyDownloadRequest request = new MyDownloadRequest(
          requestManager,
          subId,
          /* String creator */ null,
          /* Bundle configOverrides */ null,
          locationUrl,
          /* ResponseListener responseListener */ MMSReceiver.this
        );

        request.execute(context, manager);
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
