package com.android.mms.service_alt;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;

public abstract class MyBaseRequest extends MmsRequest {
    private boolean didEditPrefs;

    public MyBaseRequest(RequestManager requestManager, int subId, String creator, Bundle configOverrides) {
        super(requestManager, subId, creator, configOverrides);

        didEditPrefs = false;
    }

    @Override
    public void execute(Context context, MmsNetworkManager networkManager) {
        editPrefs(context);

        super.execute(context, networkManager);
    }

    private void editPrefs(Context context) {
        if (didEditPrefs) return;

        // MmsRequest.useWifi(context) should always return TRUE
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("mms_over_wifi", true).commit();
        didEditPrefs = true;
    }
}
