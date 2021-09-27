package com.rudyii.hsw.client.broadcasting.receivers;

import static com.rudyii.hsw.client.HomeSystemClientApplication.getAppContext;
import static com.rudyii.hsw.client.helpers.Utils.getPrimaryAccountEmail;
import static com.rudyii.hsw.client.helpers.Utils.writeJson;
import static com.rudyii.hsw.client.providers.DatabaseProvider.getAllServers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.rudyii.hs.common.objects.IamBack;
import com.rudyii.hsw.client.helpers.ToastDrawer;
import com.rudyii.hsw.client.objects.internal.ServerData;

import org.json.JSONObject;

import java.net.InetAddress;
import java.util.Map;

public class NetworkChangedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
            WifiManager wifiManager = (WifiManager) getAppContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo.getSupplicantState().equals(SupplicantState.COMPLETED)) {
                disarmSystem();
            }
        }
    }

    private void disarmSystem() {
        RequestQueue requestQueue = Volley.newRequestQueue(getAppContext());
        Map<String, ServerData> allServers = getAllServers();
        allServers.forEach((serverKey, serverData) -> {
            AsyncTask.execute(() -> {
                try {
                    if (InetAddress.getByName(serverData.getServerIp()).isReachable(1000)) {
                        String url = "http://" + serverData.getServerIp() + ":" + serverData.getServerPort() + "/control/iam_back";
                        IamBack iamBack = IamBack.builder()
                                .serverKey(serverKey)
                                .email(getPrimaryAccountEmail())
                                .build();
                        JsonObjectRequest jsonObjectRequest =
                                null;
                        jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(writeJson(iamBack)), response -> {
                            System.out.println(response.toString());
                        }, error -> {
                            System.out.println(error.toString());
                        });
                        new ToastDrawer().showToast(serverData.getServerAlias() + " :)");

                        requestQueue.add(jsonObjectRequest);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }
}
