/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller that monitors date changes and notifies registered callbacks
 * when the calendar date changes.
 */
public class OADateChangeController {

	/**
	 * List of weakly referenced callbacks to be notified on date changes.
	 */
	private static final List<WeakReference<Callback>> alCallback = new ArrayList<>();

	/**
	 * Background thread used to monitor date changes.
	 */
	private static Thread thread;

	/**
	 * Callback interface used to receive date change notifications.
	 */
	public interface Callback {
		public void onDateChange();
	};

	/**
	 * Registers a callback to be notified when the date changes.
	 *
	 * @param callback the callback to register
	 */
	public static void onChange(Callback callback) {
		if (callback == null) {
			return;
		}

		WeakReference<Callback> wref = new WeakReference<Callback>(callback);

		synchronized (alCallback) {
			alCallback.add(wref);
			if (thread != null) {
				return;
			}

			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					process();
				}
			}, "OADateChangeNotifier");
			thread.setDaemon(true);
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.start();
		}
	}

	/**
	 * Background processing loop that waits for date changes and invokes
	 * registered callbacks when a new date is detected.
	 */
	protected static void process() {
		OADate dateLast = new OADate();
		for (;;) {
			OADateTime dtNext = new OADate(); // today midnight
			dtNext = dtNext.addDays(1); // tomorrow midnight

			if (!dtNext.equals(dateLast)) {
				OADateTime dtNow = new OADateTime();
				long diff = dtNext.getTime() - dtNow.getTime();
				try {
					Thread.sleep(diff);
				} catch (Exception ex) {
				}
			}

			OADate today = new OADate();
			if (today.equals(dateLast)) {
				continue;
			}
			dateLast = today;

			ArrayList<WeakReference<Callback>> al = new ArrayList();
			synchronized (alCallback) {
				al.addAll(alCallback);
			}

			for (WeakReference<Callback> wref : al) {
				Callback cb = wref.get();
				if (cb == null) {
					synchronized (alCallback) {
						alCallback.remove(wref);
					}
					continue;
				}
				cb.onDateChange();
			}
		}
	}
}
