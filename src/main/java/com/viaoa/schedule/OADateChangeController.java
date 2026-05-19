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
package com.viaoa.schedule;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import com.viaoa.datetime.OADate;
import com.viaoa.datetime.OADateTime;


/*qqqqqqqqqqqqqqq
CODEX

1. src/main/java/com/viaoa/schedule/OADateChangeController.java:53 onChange
     Bug/risk: the new notifier thread is assigned to a local variable Thread thread, not the static field private
     static Thread thread.
     Production impact: thread remains null, so every onChange registration starts another daemon notifier. Each
     thread snapshots the same callback list and can fire callbacks independently on date change, causing duplicate
     callback execution and unbounded daemon thread accumulation.
     Severity: High
     Minimal hardening: assign to OADateChangeController.thread, clear it only on intentional shutdown/failure, and
     guard startup with a real lifecycle state.

2. src/main/java/com/viaoa/schedule/OADateChangeController.java:82 process
     Bug/risk: cb.onDateChange() is not isolated per callback.
     Production impact: one callback exception terminates the notifier thread and prevents remaining callbacks from
     running. With the current shadowed-thread bug, later registrations may accidentally restart notification; once
     fixed, date-change delivery can silently stop permanently.

6. src/main/java/com/viaoa/schedule/OADateChangeController.java:82 onChange
     Bug/risk: registrations are weak-only. If a caller registers an anonymous/lambda callback and does not keep
     another strong reference, the callback can be garbage collected before the date changes.
     Production impact: registration appears successful, but date-change work silently never runs.
     Severity: Medium
     Minimal hardening: either document the strong-reference requirement clearly or retain strong registrations with
     an explicit unregister API.
  7. src/main/java/com/viaoa/schedule/OADateChangeController.java:106 process
     Bug/risk: there is no shutdown/stop path and weak-reference cleanup only happens during date-change processing.
     Production impact: after all callbacks are gone, the daemon thread remains forever and stale weak refs can
     accumulate until the next date change.
     Severity: Low
     Minimal hardening: add unregister/shutdown or self-stop when the callback list becomes empty.



*/

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
