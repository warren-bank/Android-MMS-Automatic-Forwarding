package com.google.android.mms.pdu_alt;

public class PduUtils {

  public static PduHeaders getPduHeaders(GenericPdu pdu) {
    return pdu.getPduHeaders();
  }

  public static SendReq getSendReq(PduHeaders headers, PduBody body) {
    return new SendReq(headers, body);
  }

}
