package com.cloudplugs.rest.android;

/*<license>
Copyright 2014 CloudPlugs Inc.

Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
</license>*/

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build.VERSION;
import com.cloudplugs.util.Listener;
import com.cloudplugs.util.MultiListener;
import com.cloudplugs.util.android.BaseService;
import com.cloudplugs.util.android.BaseReceiver;

/**
 * @brief An Android service for reliable sending of HTTP requests.
 * This class is for internal usage.
 * Do not create nor destroy this service directly, it is automatically managed by {@link RestClient}.
 * <br/><br/>
 * You need to declare this exact service in your <tt>AndroidManifest.xml</tt> to have it works, so
 * add the following line inside the <tt>&lt;application&gt;</tt> tag:
 * {@code <service android:name="com.cloudplugs.rest.android.RestService" />}
 * Later on you need to declare also the following permissions in your <tt>AndroidManifest.xml</tt>:
 * {@code <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />}
 * {@code <uses-permission android:name="android.permission.INTERNET" />}
 * {@code <uses-permission android:name="android.permission.WAKE_LOCK" />}
 */
public class RestService extends BaseService
{
	public  static final String EVT_CONN    = "connState";
	private static final String ACTION_CONN = ConnectivityManager.CONNECTIVITY_ACTION;

	public static boolean isRunning() {
		return me != null;
	}

	public static boolean isStarting() {
		return starting;
	}

	public static boolean isStopping() {
		return stopping;
	}

	public static boolean isConnected() {
		return connected;
	}

	public static boolean shouldStart() {
		return !starting && (me==null || stopping);
	}

	public static boolean shouldStop() {
		return !stopping && (me!=null || starting);
	}

	public static void start(Context ctx) {
		start(ctx, null);
	}

	public static void start(Context ctx, Integer wakeLockId) {
		//if(me != null) return;
		starting = true;
		stopping = false;
		Intent intent = new Intent(ctx, RestService.class);
		addWakeLockId(ctx, intent, wakeLockId, true);
		ctx.startService(intent);
	}

	public static void stop(Context ctx) {
		starting = false;
		stopping = true;
		ctx.stopService(new Intent(ctx, RestService.class));
	}

	public static boolean hasListener(Listener l) {
		return listeners.hasListener(l);
	}

	public static void addListener(Listener l) {
		listeners.addListener(l);
	}

	public static void removeListener(Listener l) {
		listeners.removeListener(l);
	}

	public static void removeAllListeners() {
		listeners.clearListeners();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		if(receiver == null) {
			if(connMan == null)
				connMan = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
			receiver = new Receiver();
			Intent intent = registerReceiver(receiver, new IntentFilter(ACTION_CONN));
			if(intent != null) receiver.onReceive(this, intent);
		}
	}

	@Override
	public void onDestroy() {
		me = null;
		stopping = false;
		super.onDestroy();
		if(receiver != null) {
			unregisterReceiver(receiver);
			receiver = null;
			connMan  = null;
		}
		listeners.onStop();
	}

	@Override
	public int startService(Intent intent, int startId) {
		starting = false;
		if(stopping) {
			stopSelf();
			return START_NOT_STICKY;
		}
		if(me == null) {
			me = this;
			listeners.onReady();
		}
		return START_STICKY;
	}

	private final static class Receiver extends BaseReceiver {
		@Override @SuppressWarnings("deprecation")
		public Integer receive(Context context, Intent intent, Integer wakeLockId) {
			if(ACTION_CONN.equals(intent.getAction())) {
				NetworkInfo ni = null;
				if(VERSION.SDK_INT < 17) {
					ni = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
				} else {
					int type = intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, -666);
					if(type != -666)
						ni = connMan.getNetworkInfo(type);
				}
				if(ni != null) {
					boolean conn = ni.isAvailable();
					if(conn != connected) {
						connected = conn;
						listeners.onEvt(EVT_CONN, conn);
					}
				}
			}
			return wakeLockId;
		}
	}

	private static RestService me;
	private static ConnectivityManager connMan;
	private static BaseReceiver receiver;
	private static MultiListener listeners = new MultiListener();
	private static boolean starting = false;
	private static boolean stopping = false;
	private static boolean connected = false;
}
