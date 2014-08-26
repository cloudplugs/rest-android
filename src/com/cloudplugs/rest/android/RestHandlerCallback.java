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

import android.os.Handler;
import com.cloudplugs.rest.RestCallback;

/**
 * @brief Helper class for easy management of {@link com.cloudplugs.rest.RestCallback} with android.os.Handler.
 * It is a wrapper of a custom {@link com.cloudplugs.rest.RestCallback} such that it will be executed inside an android.os.Handler.
 */
public class RestHandlerCallback extends RestCallback.Meta
{
	/**
	 * Create a new callback automatically executes the given one inside the specified handler loop.
	 * @param handler the Android handler
	 * @param cb the callback to execute inside the <tt>handler</tt>
	 */
	public RestHandlerCallback(Handler handler, RestCallback cb) {
		super(cb);
		setHandler(handler);
	}

	/**
	 * @return the Android handler of this instance
	 */
	public Handler getHandler() {
		return handler;
	}

	/**
	 * Set an Android handler to use when executing the callback.
	 * @param handler the handler to use
	 */
	public void setHandler(Handler handler) {
		if(handler == null) throw new NullPointerException("null handler");
		this.handler = handler;
	}

	private Handler handler;

	@Override
	public void enqueue(Runnable runnable) {
		handler.post(runnable);
	}
}
