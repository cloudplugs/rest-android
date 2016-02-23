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
import android.util.Log;
import com.cloudplugs.util.ErrHandler;


/**
 * @brief A {@link com.cloudplugs.rest.RestClient} extension for Android.
 * It implements an automatic and transparent mechanism to create an Android service for reliable background execution.
 * The underlying spooler (if any) will automatically be paused and resumed respectively when the Internet connectivity became
 * unavailable and available.<br/>
 * See {@link com.cloudplugs.rest.RestClient} for further details about the RestClient usage.
 */
public class RestClient extends com.cloudplugs.rest.RestClient
{
	/** Event String emitted every time the Internet connectivity becomes available. */
	public static final String EVT_CONN = RestService.EVT_CONN;

	protected RestClient(Listener listener) {
		super(RestSpooler.class);
		if(listener == null) throw new NullPointerException("null listener");
		this.listener = listener;
		RestService.addListener(this);
	}

	/**
	 * Create asynchronously a new instance of this class so that it will be passed to the given
	 * {@link com.cloudplugs.rest.android.RestClient.Listener} when ready.
	 * This method will automatically create a dedicated Android service (if not already running) for handling
	 * the background execution.
	 * @param context the Android context in which create the Android service
	 * @param listener the {@link com.cloudplugs.rest.android.RestClient.Listener} will receive the instance of this class
	 * @throws NullPointerException if an argument is null
	 */
	public static void create(Context context, Listener listener) {
		if(context != null) ctx = context.getApplicationContext();
		else if(ctx == null) throw new NullPointerException("null context");
		RestClient rc = new RestClient(listener);
		if(RestService.shouldStart()) {
			RestService.start(ctx);
		} else {
			rc.onCreateClient();
		}
	}

	@Override
	public void onReady() {
		super.onReady();
		if(listener != null) onCreateClient();
	}

	@Override
	public void onStop() {
		if(RestService.shouldStop()) {
			RestService.stop(ctx);
		} else {
			stop();
		}
		super.onStop();
	}

	@Override
	public void onEvt(Object evt, Object value) {
		if(EVT_CONN.equals(evt) && value instanceof Boolean) {
			if((Boolean)value) resume();
			else               pause();
		}
		super.onEvt(evt, value);
	}

	/**
	 * @return the instance of android.content.Context associated to this instance
	 */
	public static Context ctx() {
		return ctx;
	}

	private void onCreateClient() {
		try {
			listener.onReady(this);
		} catch(Throwable t) {
			ErrHandler handler = ErrHandler.active;
			if(handler != null) handler.handleErr(t);
		}
		listener = null;
	}

	/**
	 * @brief A Listener instance will receive the newly created instance of {@link RestClient}
	 * when passed to {@link #create(Context, Listener)}
	 * method.
	 */
	public static interface Listener {
		/**
		 * This method is automatically and asynchronously invoked when the REST client become available and ready to use.
		 * @param restClient the newly created client
		 */
		public void onReady(RestClient restClient);
	}

	static {
		// check if the default error handler has already been changed
		if(ErrHandler.active == ErrHandler.DEFAULT) {
			// if not, replace the default error handler using Android log
			ErrHandler.active = new ErrHandler() {
				@Override
				public void handleErr(Throwable err) {
					if(err == null) Log.d(TAG, "null error");
					else Log.e(TAG, "", err);
				}
			};
		}
	}

	private Listener listener;
	private static Context ctx;

	private static final String TAG = ErrHandler.class.getName();
}
