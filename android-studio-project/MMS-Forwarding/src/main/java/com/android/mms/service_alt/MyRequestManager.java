package com.android.mms.service_alt;

import android.net.Uri;

public class MyRequestManager implements MmsRequest.RequestManager {
    private byte[] pduData;

    public MyRequestManager() {
        this(null);
    }

    public MyRequestManager(byte[] pduData) {
        this.pduData = pduData;
    }

    @Override
    public void addSimRequest(MmsRequest request) {
    }

    @Override
    public boolean getAutoPersistingPref() {
        return false;
    }

    @Override
    public byte[] readPduFromContentUri(Uri contentUri, int maxSize) {
        return pduData;
    }

    @Override
    public boolean writePduToContentUri(Uri contentUri, byte[] response) {
        return false;
    }
}
