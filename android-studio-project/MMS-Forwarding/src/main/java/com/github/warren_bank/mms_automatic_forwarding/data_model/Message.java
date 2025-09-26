package com.github.warren_bank.mms_automatic_forwarding.data_model;

import java.util.ArrayList;

public final class Message {
  public ArrayList<String> recipients;
  public String sender;
  public String sender_contact_name;

  public Message(ArrayList<String> recipients, String sender, String sender_contact_name) {
    this.recipients          = recipients;
    this.sender              = sender;
    this.sender_contact_name = sender_contact_name;
  }
}
