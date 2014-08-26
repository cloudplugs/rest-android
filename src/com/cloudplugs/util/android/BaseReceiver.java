package com.cloudplugs.util.android;

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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @brief Helper class of {@link BaseService} to allow asynchronous release of Android wake locks.
 * This class is for internal usage.
 */
public class BaseReceiver extends BroadcastReceiver
{
	public static final String NAME              = BaseReceiver.class.getSimpleName();
	public static final String ACT_WAKE_RELEASE  = NAME+".releaseWakeLock";
	public static final String EXT_WAKE_ID       = NAME+".wakeLockId";
	public static final int    ID_NULL           = BaseService.ID_NULL;
	public static final int    WAKE_LOCK_TIMEOUT = BaseService.WAKE_LOCK_TIMEOUT;

	private static final Map<Integer,WakeLock> _wakeLocks = new ConcurrentHashMap<Integer,WakeLock>();
	private static final AtomicInteger _wakeLockSeq = new AtomicInteger(ID_NULL);

	private static Integer _getWakeLock(Context ctx) {
		PowerManager pm = (PowerManager)ctx.getSystemService(Context.POWER_SERVICE);
		WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, NAME);
		wakeLock.setReferenceCounted(false);
		wakeLock.acquire(WAKE_LOCK_TIMEOUT);
		Integer tmpWakeLockId = _wakeLockSeq.incrementAndGet();
		_wakeLocks.put(tmpWakeLockId, wakeLock);
		return tmpWakeLockId;
	}

	private static void _releaseWakeLock(Integer wakeLockId) {
		WakeLock wl = _wakeLocks.remove(wakeLockId);
		if(wl != null) wl.release();
	}

	@Override
	public final void onReceive(Context ctx, Intent i) {
		Integer tmpWakeLockId = _getWakeLock(ctx);
		try {
			if(ACT_WAKE_RELEASE.equals(i.getAction())) {
				int wakeLockId = i.getIntExtra(EXT_WAKE_ID, ID_NULL);
				if(wakeLockId != ID_NULL)
					_releaseWakeLock(wakeLockId);
			} else {
				tmpWakeLockId = receive(ctx, i, tmpWakeLockId);
			}
		} finally {
			if(tmpWakeLockId != null)
				_releaseWakeLock(tmpWakeLockId);
		}
	}

	// subclasses should override this method
	public Integer receive(Context ctx, Intent i, Integer wakeLockId) {
		return wakeLockId;
	}

	public static void releaseWakeLock(Context ctx, int wakeLockId) {
		Intent i = new Intent();
		i.setClass(ctx, BaseReceiver.class);
		i.setAction(ACT_WAKE_RELEASE);
		i.putExtra(EXT_WAKE_ID, wakeLockId);
		ctx.sendBroadcast(i);
	}

	public static synchronized int getNumOfHeldWakeLocks() {
		int res = 0;
		for(WakeLock wl : _wakeLocks.values())
			if(wl!=null && wl.isHeld()) res++;
		return res;
	}
}
