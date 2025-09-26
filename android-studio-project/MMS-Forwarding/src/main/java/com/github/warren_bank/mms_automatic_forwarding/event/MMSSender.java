package com.github.warren_bank.mms_automatic_forwarding.event;

import com.github.warren_bank.mms_automatic_forwarding.R;
import com.github.warren_bank.mms_automatic_forwarding.data_model.Message;

import com.klinker.android.send_message.Utils;

import com.android.mms.dom.smil.parser.SmilXmlSerializer;
import com.android.mms.service_alt.MmsNetworkManager;
import com.android.mms.service_alt.MyRequestManager;
import com.android.mms.service_alt.MySendRequest;

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

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

public final class MMSSender {

  public static void forward(Context context, Message msg, RetrieveConf pdu) {
    try {
      String preface = context.getString(R.string.mms_preface_heading);
      if ((msg.sender_contact_name != null) && !msg.sender_contact_name.isEmpty()) {
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
      add_preface_TEXT_part(pduBody, preface);
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

  private static void send_MMS(Context context, byte[] pduBytes) {
    int subId = Utils.getDefaultSubscriptionId();

    MyRequestManager requestManager = new MyRequestManager(pduBytes);
    MmsNetworkManager manager = new MmsNetworkManager(context, subId);

    MySendRequest request = new MySendRequest(
      requestManager,
      subId,
      /* String creator */ null,
      /* Bundle configOverrides */ null,
      /* String locationUrl */ null
    );

    request.execute(context, manager);
  }
}
