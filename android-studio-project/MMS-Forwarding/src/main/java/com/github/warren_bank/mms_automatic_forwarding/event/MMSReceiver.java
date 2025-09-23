package com.github.warren_bank.mms_automatic_forwarding.event;

import com.github.warren_bank.mms_automatic_forwarding.data_model.Contacts;
import com.github.warren_bank.mms_automatic_forwarding.data_model.Preferences;
import com.github.warren_bank.mms_automatic_forwarding.data_model.RecipientListItem;

import com.google.android.mms.pdu_alt.GenericPdu;
import com.google.android.mms.pdu_alt.MultimediaMessagePdu;
import com.google.android.mms.pdu_alt.PduHeaders;
import com.google.android.mms.pdu_alt.PduParser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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
      if ((pdu == null) || (PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF != pdu.getMessageType()))
        return;

      ArrayList<RecipientListItem> listItems = Preferences.getRecipientListItems(context);
      if (listItems.isEmpty())
        return;

      String sender                = pdu.getFrom().getString();
      ArrayList<String> recipients = RecipientListItem.match(listItems, sender);
      if ((recipients == null) || recipients.isEmpty())
        return;

      Log.i(TAG, "MMS received.\nfrom: " + sender);

      String sender_contact_name = Contacts.getContactName(context, sender);

      MMSSender.forward(context, recipients, sender, sender_contact_name, (MultimediaMessagePdu) pdu);
    }
    catch (Exception e) {}
  }
}
