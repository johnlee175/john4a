package com.johnsoft.library.util.net;

import java.io.InputStream;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.IntDef;

/**
 * wifi auto connector
 *
 * @author John Kenrinus Lee
 * @version 2017-03-07
 */

public final class WifiAutoConnector {
    private static void log(String message) {
        System.err.println("WifiAutoConnector -- " + message);
    }

    public static boolean simpleConnectWifi(Context context, String targetSSID, String targetPassword) {
        Context appContext = context.getApplicationContext();
        // 1. 注意热点和密码均包含引号, 此处需要需要转义引号
        String ssid = "\"" + targetSSID + "\"";
        String password = "\"" + targetPassword + "\"";

        // 2. 配置wifi信息
        WifiConfiguration wc = new WifiConfiguration();
        wc.allowedAuthAlgorithms.clear();
        wc.allowedGroupCiphers.clear();
        wc.allowedKeyManagement.clear();
        wc.allowedPairwiseCiphers.clear();
        wc.allowedProtocols.clear();

        wc.SSID = ssid;
        wc.hiddenSSID = true;
        wc.status = WifiConfiguration.Status.ENABLED;

        wc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wc.preSharedKey = password;

        // 3. 链接wifi

        WifiManager wifiManager = (WifiManager) appContext.getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled()) {
            if (!wifiManager.setWifiEnabled(true)) {
                log("wifiManager.setWifiEnabled(true) -> failed");
                return false;
            }
        }

        int times = 500;
        while (wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED && times >= 0) {
            --times;
            try {
                Thread.sleep(10L);
            } catch (InterruptedException ignored) {
                // do nothing
            }
            if (times == 0) {
                log("wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED stilling...");
            }
        }

        List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        if (configurations != null && !configurations.isEmpty()) {
            for (WifiConfiguration conf : configurations) {
                if (!wifiManager.disableNetwork(conf.networkId)) {
                    log("wifiManager.disableNetwork(int) -> failed");
                }
                if (conf.SSID != null && conf.SSID.equals(ssid)) {
                    // remove could be failed when api level >= 23
                    if (wifiManager.removeNetwork(conf.networkId)) {
                        if (!wifiManager.saveConfiguration()) {
                            log("wifiManager.saveConfiguration() after removeNetwork(int) -> failed");
                        }
                    } else {
                        log("wifiManager.removeNetwork(int) -> failed");
                    }
                }
            }
        } else {
            log("wifiManager.getConfiguredNetworks() return null or empty list");
        }

        int networkId = wifiManager.addNetwork(wc);
        if (networkId != -1) {
            if (!wifiManager.saveConfiguration()) {
                log("wifiManager.saveConfiguration() after addNetwork(WifiConfiguration) -> failed");
            }
            wifiManager.disconnect();
            if (wifiManager.enableNetwork(networkId, true)) {
                wifiManager.reconnect();
                return true;
            } else {
                log("wifiManager.enableNetwork(int, true) -> failed");
            }
        } else {
            log("wifiManager.addNetwork(WifiConfiguration) return -1");
        }
        return false;
    }

    public static final int TYPE_OPEN = 0;
    public static final int TYPE_WEP = 1;
    public static final int TYPE_WAP = 2;
    public static final int TYPE_WAP2 = 3;

    @IntDef({TYPE_OPEN, TYPE_WEP, TYPE_WAP, TYPE_WAP2})
    @Documented
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.SOURCE)
    public @interface EncType {
    }

    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    private int networkId;
    private boolean isConnected;

    private String identity;
    private String anonymousIdentity;
    private int eapMethod = -1;
    private int phase2Method = -1;
    private InputStream caCertificate;

    public void applyEap(String identity, String anonymousIdentity, int eapMethod, int phase2Method,
                         InputStream caCertificate) {
        if (Build.VERSION.SDK_INT >= 18) {
            this.identity = identity;
            this.anonymousIdentity = anonymousIdentity;
            this.eapMethod = eapMethod;
            this.phase2Method = phase2Method;
            this.caCertificate = caCertificate;
        }
    }

    @TargetApi(18)
    private void reconfigIfApplyEap(WifiConfiguration wc, String password) {
        if (identity != null && !identity.trim().isEmpty() && eapMethod >= 0) {
            wc.preSharedKey = null;
            wc.allowedKeyManagement.clear();
            wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
            wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
            wc.enterpriseConfig.setPassword(password);
            wc.enterpriseConfig.setIdentity(identity);
            wc.enterpriseConfig.setEapMethod(eapMethod);
            if (phase2Method < 0) {
                phase2Method = WifiEnterpriseConfig.Phase2.NONE;
            }
            wc.enterpriseConfig.setPhase2Method(phase2Method);
            if (anonymousIdentity != null && anonymousIdentity.trim().isEmpty()) {
                anonymousIdentity = null;
            }
            wc.enterpriseConfig.setAnonymousIdentity(anonymousIdentity);
            if (caCertificate != null) {
            try {
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                X509Certificate cert = (X509Certificate) certFactory.generateCertificate(caCertificate);
                wc.enterpriseConfig.setCaCertificate(cert);
            } catch (CertificateException e) {
                e.printStackTrace();
            }
            } else {
                wc.enterpriseConfig.setCaCertificate(null);
            }
        }
    }

    public boolean connectWifiMayWait(Context context, String targetSSID, String targetPassword, @EncType int encType,
                               boolean strictCheck, long timeoutMillis) throws InterruptedException {
        isConnected = false;
        WifiConnectionReceiver wifiConnectionReceiver = new WifiConnectionReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION); // 连上与否
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION); // 是不是正在获得IP地址
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION); // 信号强度变化
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION); // 网络状态变化
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION); // wifi状态, 是否连上, 密码
        context.registerReceiver(wifiConnectionReceiver, filter);
        lock.lockInterruptibly();
        try {
            if (connectWifi(context, targetSSID, targetPassword, encType, strictCheck)) {
                log("waiting wifi connect successfully ...");
                condition.await(timeoutMillis, TimeUnit.MILLISECONDS);
            }
            return isConnected;
        } finally {
            lock.unlock();
            context.unregisterReceiver(wifiConnectionReceiver);
        }
    }

    /* <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
       <uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>
       <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
       <uses-permission android:name="android.permission.CHANGE_NETWORK_STATE"/> */
    /* Thread safe. Target version <= 22 */
    public boolean connectWifi(Context context, String targetSSID, String targetPassword, @EncType int encType,
                               boolean strictCheck) {
        Context appContext = context.getApplicationContext();
        // 1. 注意热点和密码均包含引号, 此处需要需要转义引号
        String ssid = "\"" + targetSSID + "\"";
        String password = "\"" + targetPassword + "\"";

        // 2. 配置wifi信息
        WifiConfiguration wc = new WifiConfiguration();
        wc.allowedAuthAlgorithms.clear();
        wc.allowedGroupCiphers.clear();
        wc.allowedKeyManagement.clear();
        wc.allowedPairwiseCiphers.clear();
        wc.allowedProtocols.clear();

        wc.SSID = ssid;
        wc.hiddenSSID = true;
        wc.status = WifiConfiguration.Status.ENABLED;
        wc.priority = 1000000;

        switch (encType) {
            case TYPE_OPEN: // 开放网络
                wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                wc.wepKeys[0] = "";
                wc.wepTxKeyIndex = 0;
                wc.preSharedKey = "";
                break;
            case TYPE_WEP: // Wired Equivalent Privacy
                wc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                wc.wepKeys[0] = password;
                wc.wepTxKeyIndex = 0;
                break;
            case TYPE_WAP: // Wi-Fi Protected Access
                wc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wc.preSharedKey = password;
                if (Build.VERSION.SDK_INT >= 18) {
                    reconfigIfApplyEap(wc, password);
                }
                break;
            case TYPE_WAP2: // RSN
                wc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wc.preSharedKey = password;
                if (Build.VERSION.SDK_INT >= 18) {
                    reconfigIfApplyEap(wc, password);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown enc");
        }
        // 3. 链接wifi

        WifiManager wifiManager = (WifiManager) appContext.getSystemService(Context.WIFI_SERVICE);

        if (strictCheck) {
            if (!wifiManager.isWifiEnabled()) {
                if (!wifiManager.setWifiEnabled(true)) {
                    log("wifiManager.setWifiEnabled(true) -> failed");
                    return false;
                }
            }

            int times = 500;
            while (wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED && times >= 0) {
                --times;
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException ignored) {
                    // do nothing
                }
                if (times == 0) {
                    log("wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED stilling...");
                }
            }

            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null && wifiInfo.getSSID().equals(ssid)) {
                SupplicantState supplicantState = wifiInfo.getSupplicantState();
                if (supplicantState != null && supplicantState.equals(SupplicantState.COMPLETED)) {
                    NetworkInfo.DetailedState detailedState = WifiInfo.getDetailedStateOf(supplicantState);
                    if (detailedState != null
                            && (detailedState.equals(NetworkInfo.DetailedState.CONNECTED)
                                        || detailedState.equals(NetworkInfo.DetailedState.OBTAINING_IPADDR))) {
                        return true;
                    }
                }
            }

            if (wifiManager.startScan()) {
                List<ScanResult> scanResults = wifiManager.getScanResults();
                if (scanResults != null && !scanResults.isEmpty()) {
                    boolean hasSSID = false;
                    for (ScanResult scanResult : scanResults) {
                        if (scanResult.SSID.equals(targetSSID)) {
                            hasSSID = true;
                            break;
                        }
                    }
                    if (!hasSSID) {
                        log("no SSID matched name is valid nearby");
                        return false;
                    }
                } else {
                    log("wifiManager.getScanResults() return null or empty list");
                }
            } else {
                log("wifiManager.startScan() -> failed");
            }

            List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
            if (configurations != null && !configurations.isEmpty()) {
                int priority = -1;
                for (WifiConfiguration conf : configurations) {
                    if (conf.priority > priority) {
                        priority = conf.priority;
                    }
                    if (!wifiManager.disableNetwork(conf.networkId)) {
                        log("wifiManager.disableNetwork(int) -> failed");
                    }
                    if (conf.SSID != null && conf.SSID.equals(ssid)) {
                        // remove could be failed when api level >= 23
                        if (wifiManager.removeNetwork(conf.networkId)) {
                            if (!wifiManager.saveConfiguration()) {
                                log("wifiManager.saveConfiguration() after removeNetwork(int) -> failed");
                            }
                        } else {
                            log("wifiManager.removeNetwork(int) -> failed");
                        }
                    }
                }
                log("wifiManager.getConfiguredNetworks() max priority: " + priority);
                if (priority > wc.priority) {
                    wc.priority = priority + 1;
                }
            } else {
                log("wifiManager.getConfiguredNetworks() return null or empty list");
            }
        }

        int networkId = wifiManager.addNetwork(wc);
        if (networkId != -1) {
            boolean saveSuccessful = wifiManager.saveConfiguration();
            if (!saveSuccessful) {
                log("wifiManager.saveConfiguration() after addNetwork(WifiConfiguration) -> failed");
            }
            if (!wifiManager.disconnect()) {
                log("wifiManager.disconnect() -> failed");
            }
            if (wifiManager.enableNetwork(networkId, true)) {
                this.networkId = networkId;
                boolean reconnectSuccessful = wifiManager.reconnect();
                if (!reconnectSuccessful) {
                    log("wifiManager.reconnect() -> failed");
                }
                boolean reassociateSuccessful = wifiManager.reassociate();
                if (!reassociateSuccessful) {
                    log("wifiManager.reassociate() -> failed");
                }
                return saveSuccessful && reconnectSuccessful && reassociateSuccessful;
            } else {
                log("wifiManager.enableNetwork(int, true) -> failed");
            }
        } else {
            log("wifiManager.addNetwork(WifiConfiguration) return -1");
        }
        return false;
    }

    private final class WifiConnectionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                log("ignore action by WifiConnectionReceiver: " + intent.getAction());
                return;
            }
            lock.lock();
            try {
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = wifiManager.getConnectionInfo();
                if (info.getNetworkId() == networkId && info.getSupplicantState() == SupplicantState.COMPLETED) {
                    isConnected = true;
                    condition.signalAll();
                } else {
                    log("info.getNetworkId() == networkId -> " + (info.getNetworkId() == networkId)
                            + ", info.getSupplicantState() -> " + info.getSupplicantState());
                }
            } finally {
                lock.unlock();
            }
        }
    }
}
