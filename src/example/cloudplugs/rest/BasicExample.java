package example.cloudplugs.rest;

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

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.cloudplugs.rest.*;
import com.cloudplugs.rest.android.RestClient;
import example.cloudplugs.R;

/**
 * @brief Basic Activity example.
 * Publish a random number to a predefined channel each time the button is pressed.
 */
public class BasicExample extends Activity implements RestCallback
{
	// TODO: put here your prototype PlugID
	private final static String AUTH_PLUGID = "dev-XXXXXXXXXXXXXXXXXXXXXXXX";
	// TODO: put here your password
	private final static String AUTH_PASS = "password";
	// leave true for using your account password (or set it to false for using the specific prototype password)
	private final static boolean AUTH_MASTER = true;

	// the channel name used for publishing data
	private final static String CHANNEL = "temperature";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.basic_example);
		handler = new Handler();

		// this text view will contain logged messages
		textView = (TextView)findViewById(R.id.textView);
		textView.setMovementMethod(new ScrollingMovementMethod());	// enable text view scrolling

		// the button to press for publishing data
		button = (Button)findViewById(R.id.button);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				publishData();
			}
		});

		// create the main RestClient
		RestClient.create(this, new RestClient.Listener() {
			@Override
			public void onReady(RestClient rc) {
				restClient  = rc;
				restManager = rc.getManager(new Opts().setAuth(AUTH_PLUGID, AUTH_PASS, AUTH_MASTER));
				log("==== CloudPlugs RestClient is ready");
			}
		});
	}

	@Override
	public void onDestroy() {
		// application is quitting: stop the RestClient and release all associated resources
		if(restClient != null) {
			restClient.destroy();
			restClient  = null;
			restManager = null;
		}
		super.onDestroy();
	}

	private void publishData() {
		// check if the RestClient instance has been created
		if(restManager != null) {
			// the RestClient has been created: let's publish data
			double data = getDataToPublish();
			int id = restManager.execPublishData(CHANNEL, data, null, this);
			log("<<<< [ request "+id+" ] publishing data to channel '"+CHANNEL+"': "+data);
		} else {
			// the RestClient instance has not been created yet
			log("**** Please wait until RestClient becomes ready");
		}
	}

	private void log(final String msg) {
		// log the message to the text view and to the standard Android log system
		Log.d(TAG, msg);
		handler.post(new Runnable() {
			@Override
			public void run() {
				textView.append(msg + "\n");
			}
		});
	}

	// retrieve the data to publish in the cloud
	private static double getDataToPublish() {
		return Math.random() * 100;
	}

	/**
	 * Implementation of com.cloudplugs.rest.RestCallback .
	 * It will be automatically called after each executed request.
	 * @param request the generated request
	 * @param response the received response from the server
	 */
	@Override
	public void on(Request request, Response response) {
		// just log the result
		String state = null;
		if(!response.isCompleted())   state = "interrupted";
		else if(response.isSuccess()) state = "successful";
		else if(response.isPartial()) state = "partially completed";
		else if(response.isFailed())  state = "failed";
		log(">>>> [ request " + request.getId() + " ] " + state + ", response is: " + response);
	}

	private RestClient  restClient  = null;
	private RestManager restManager = null;

	private Handler  handler;
	private TextView textView;
	private Button   button;

	private static final String TAG = BasicExample.class.getSimpleName();
}
