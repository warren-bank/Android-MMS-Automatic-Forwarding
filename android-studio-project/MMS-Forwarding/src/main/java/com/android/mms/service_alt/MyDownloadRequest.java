package com.android.mms.service_alt;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.android.mms.service_alt.exception.MmsHttpException;
import com.google.android.mms.pdu_alt.GenericPdu;
import com.google.android.mms.pdu_alt.PduParser;
import com.google.android.mms.pdu_alt.RetrieveConf;

/**
 * Request to download an MMS
 */
public class MyDownloadRequest extends ModifiedMmsRequest {

    public static interface ResponseListener {
        public void onResponse(Context context, RetrieveConf pdu);
    }

    private final String mLocationUrl;
    private final ResponseListener mResponseListener;

    public MyDownloadRequest(RequestManager requestManager, int subId, String creator, Bundle configOverrides, String locationUrl, ResponseListener responseListener) {
        super(requestManager, subId, creator, configOverrides);

        mLocationUrl = locationUrl;
        mResponseListener = responseListener;
    }

    @Override
    protected byte[] doHttp(Context context, MmsNetworkManager netMgr, ApnSettings apn) throws MmsHttpException {
        if (!prepareForHttpRequest()) return null;

        final MmsHttpClient mmsHttpClient = netMgr.getOrCreateHttpClient();
        if (mmsHttpClient == null) {
            throw new MmsHttpException(0/*statusCode*/, "MMS network is not ready");
        }
        return mmsHttpClient.execute(
                mLocationUrl,
                null/*pud*/,
                MmsHttpClient.METHOD_GET,
                apn.isProxySet(),
                apn.getProxyAddress(),
                apn.getProxyPort(),
                mMmsConfig);
    }

    @Override
    protected Uri persistIfRequired(Context context, int result, byte[] response) {
        if (!prepareForHttpRequest()) return null;

        if ((response != null) && (response.length > 0)) {
            final GenericPdu pdu = (new PduParser(response, mMmsConfig.getSupportMmsContentDisposition())).parse();
            if (pdu != null && (pdu instanceof RetrieveConf)) {
                final RetrieveConf retrieveConf = (RetrieveConf) pdu;

                mResponseListener.onResponse(context, retrieveConf);
            }
        }

        return null;
    }

    @Override
    protected PendingIntent getPendingIntent() {
        return null;
    }

    @Override
    protected int getQueueType() {
        return 1;
    }

    @Override
    protected boolean prepareForHttpRequest() {
        return ((mLocationUrl != null) && (mResponseListener != null));
    }

    @Override
    protected boolean transferResponse(Intent fillIn, final byte[] response) {
        return false;
    }

    @Override
    protected void revokeUriPermission(Context context) {
    }
}
