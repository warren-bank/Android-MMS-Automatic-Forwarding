package com.android.mms.service_alt;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.android.mms.service_alt.exception.MmsHttpException;

/**
 * Request to send an MMS
 */
public class MySendRequest extends ModifiedMmsRequest {

    private final String mLocationUrl;
    private final byte[] mPduData;

    public MySendRequest(RequestManager requestManager, int subId, String creator, Bundle configOverrides, String locationUrl) {
        super(requestManager, subId, creator, configOverrides);

        this.mLocationUrl = locationUrl;
        this.mPduData = requestManager.getPdu();
    }

    @Override
    protected byte[] doHttp(Context context, MmsNetworkManager netMgr, ApnSettings apn) throws MmsHttpException {
        if (!prepareForHttpRequest()) return null;

        final MmsHttpClient mmsHttpClient = netMgr.getOrCreateHttpClient();
        if (mmsHttpClient == null) {
            throw new MmsHttpException(0/*statusCode*/, "MMS network is not ready");
        }
        return mmsHttpClient.execute(
                mLocationUrl != null ? mLocationUrl : apn.getMmscUrl(),
                mPduData,
                MmsHttpClient.METHOD_POST,
                apn.isProxySet(),
                apn.getProxyAddress(),
                apn.getProxyPort(),
                mMmsConfig);
    }

    @Override
    protected Uri persistIfRequired(Context context, int result, byte[] response) {
        return null;
    }

    @Override
    protected PendingIntent getPendingIntent() {
        return null;
    }

    @Override
    protected int getQueueType() {
        return 0;
    }

    @Override
    protected boolean prepareForHttpRequest() {
        return ((mPduData != null) && (mPduData.length > 0));
    }

    @Override
    protected boolean transferResponse(Intent fillIn, byte[] response) {
        return false;
    }

    @Override
    protected void revokeUriPermission(Context context) {
    }
}
