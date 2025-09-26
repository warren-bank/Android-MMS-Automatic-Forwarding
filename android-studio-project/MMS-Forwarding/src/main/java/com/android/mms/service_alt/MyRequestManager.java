package com.android.mms.service_alt;

import android.net.Uri;

public class MyRequestManager implements ModifiedMmsRequest.RequestManager {
    private byte[] pduData;

    public MyRequestManager() {
        this(null);
    }

    public MyRequestManager(byte[] pduData) {
        this.pduData = pduData;
    }

    @Override
    public byte[] getPdu() {
        return pduData;
    }
}
