package com.github.warren_bank.mms_automatic_forwarding.data_model;

import java.util.ArrayList;

public final class Message {
  public ArrayList<String> recipients;
  public String sender_phone_number;
  public String sender_contact_name;

  public Message(ArrayList<String> recipients, String sender_phone_number, String sender_contact_name) {
    this.recipients          = recipients;
    this.sender_phone_number = sender_phone_number;
    this.sender_contact_name = sender_contact_name;
  }
}
