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
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

/**
 * @brief A {@link com.cloudplugs.rest.RestSpooler} extension to handle Android wake locks for reliable execution
 * when the application is not in foreground.
 * This class is for internal usage.
 */
class RestSpooler extends com.cloudplugs.rest.RestSpooler
{
	private static final String TAG = RestSpooler.class.getSimpleName();

	public RestSpooler() {
		PowerManager pm = (PowerManager)RestClient.ctx().getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		wakeLock.setReferenceCounted(false);
		wakeLock.acquire();
	}

	@Override
	public boolean pause() {
		if(!super.pause()) return false;
		releaseWakeLock();
		return true;
	}

	@Override
	public boolean resume() {
		if(!super.resume()) return false;
		acquireWakeLock();
		return true;
	}

	@Override
	public int exec(Runnable runnable) {
		acquireWakeLock();
		return super.exec(runnable);
	}

	@Override
	public void onReady() {
		acquireWakeLock();
		super.onReady();
	}

	@Override
	public void onIdle() {
		super.onIdle();
		releaseWakeLock();
	}

	private void acquireWakeLock() {
		wakeLock.acquire();
	}

	private void releaseWakeLock() {
		if(wakeLock.isHeld())
			wakeLock.release();
	}

	private WakeLock wakeLock;
}
