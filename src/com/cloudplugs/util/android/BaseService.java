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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @brief Base service class to handle background execution with an automatic mechanism of Android CPU wake locks.
 * This class is for internal usage.
 */
public abstract class BaseService extends Service
{
	private static final String NAME       = BaseService.class.getSimpleName();
	private static final String NAME_ADD   = NAME + " add";
	private static final String NAME_START = NAME + " start";

	public static final String EXT_WAKE_ID       = NAME + ".wakeLockId";
	public static final int    ID_NULL           = 0;
	public static final int    WAKE_LOCK_TIMEOUT = 60*1000;

	private static final Map<Integer,WakeLock> _wakeLocks = new ConcurrentHashMap<Integer,WakeLock>();
	private static final AtomicInteger _wakeLockSeq = new AtomicInteger(ID_NULL);

	/**
	 * Adds an existing wake lock identified by its registry ID to the specified intent.
	 * @param intent
	 *         the android.content.Intent to add the wake lock  ID as extra to (never null)
	 * @param wakeLockId
	 *         the wake lock registry ID of an existing wake lock or null
	 * @param createIfNotExists
	 *         if <tt>wakeLockId</tt> is null and this parameter is <tt>true</tt>true a new wake
	 *         lock is created, registered, and added to <tt>intent</tt>
	 */
	protected static void addWakeLockId(Context ctx, Intent intent, Integer wakeLockId, boolean createIfNotExists) {
		if(wakeLockId != null)
			intent.putExtra(BaseReceiver.EXT_WAKE_ID, wakeLockId);
		else if(createIfNotExists)
			addWakeLock(ctx, intent);
	}

	/**
	 * Adds a new wake lock to the specified intent.
	 * This will add the wake lock to the central wake lock registry managed by this class.
	 * @param ctx the android.content.Context
	 * @param i the Intent to add the wake lock ID as extra to (never null)
	 */
	protected static void addWakeLock(Context ctx, Intent i) {
		WakeLock wakeLock = acquireWakeLock(ctx, NAME_ADD, WAKE_LOCK_TIMEOUT);
		Integer tmpWakeLockId = registerWakeLock(wakeLock);
		i.putExtra(EXT_WAKE_ID, tmpWakeLockId);
	}

	/**
	 * Registers a wake lock with the wake lock registry.
	 * @param wakeLock
	 *         the android.os.PowerManager.WakeLock instance that should be registered with the wake lock
	 *         registry (never null)
	 * @return the ID that identifies this wake lock in the registry
	 */
	protected static Integer registerWakeLock(WakeLock wakeLock) {
		Integer tmpWakeLockId = _wakeLockSeq.incrementAndGet();
		_wakeLocks.put(tmpWakeLockId, wakeLock);
		return tmpWakeLockId;
	}

	/**
	 * Acquires a wake lock.
	 * @oaram ctx the android.content.Context
	 * @param tag the tag to supply to android.os.PowerManager
	 * @param timeout the wake lock timeout
	 * @return a new android.os.PowerManager.WakeLock instance
	 */
	protected static WakeLock acquireWakeLock(Context ctx, String tag, long timeout) {
		PowerManager pm = (PowerManager)ctx.getSystemService(Context.POWER_SERVICE);
		WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag);
		wakeLock.setReferenceCounted(false);
		wakeLock.acquire(timeout);
		return wakeLock;
	}

	@Override
	public final int onStartCommand(Intent intent, int flags, int id) {
		// when a process is killed due to low memory, it's later restarted and services that were
		// started with START_STICKY are started with the intent being null.
		// for now we just ignore these restart events.
		if(intent == null) {
			stopSelf(id);
			return START_NOT_STICKY;
		}

		WakeLock wakeLock = acquireWakeLock(this, NAME_START, WAKE_LOCK_TIMEOUT);

		// if we were started by BaseReceiver, release the wake lock acquired there.
		int wakeLockId = intent.getIntExtra(BaseReceiver.EXT_WAKE_ID, ID_NULL);
		if(wakeLockId != ID_NULL) BaseReceiver.releaseWakeLock(this, wakeLockId);

		// if we were passed an ID from our own wake lock registry, retrieve that wake lock and release it.
		wakeLockId = intent.getIntExtra(EXT_WAKE_ID, ID_NULL);
		if(wakeLockId != ID_NULL) {
			WakeLock coreWakeLock = _wakeLocks.remove(wakeLockId);
			if(coreWakeLock != null)
				coreWakeLock.release();
		}

		// run the actual start-code of the service
		int res;
		try {
			res = startService(intent, id);
		} finally {
			try {		// release the wake lock acquired at the start of this method
				wakeLock.release();
			} catch(Exception e) { }
		}

		return res;
	}

	/**
	 * Subclasses need to implement this instead of overriding {@link #onStartCommand(Intent, int, int)}.
	 * This allows android.app.Service to manage the service lifecycle, including wake lock management.
	 * @param intent
	 *         the Intent supplied to android.content.Context#startService(Intent)
	 * @param startId
	 *         a unique integer representing this specific request to start
	 * @return the return value indicates what semantics the system should use for the service's
	 *         current started state. It may be one of the constants associated with the
	 *         android.app.Service#START_CONTINUATION_MASK bits.
	 */
	public abstract int startService(Intent intent, int startId);

	@Override
	public IBinder onBind(Intent i) {
		return null;
	}

	public static synchronized int getNumOfHeldWakeLocks() {
		int res = 0;
		for(WakeLock wl : _wakeLocks.values())
			if(wl!=null && wl.isHeld()) res++;
		return res;
	}
}
