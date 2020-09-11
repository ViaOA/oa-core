package com.viaoa.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class OADateChangeController {

	private static final ArrayList<WeakReference<Callback>> alCallback = new ArrayList<>();
	private static Thread thread;

	public interface Callback {
		public void onDateChange();
	};

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

		}

		thread = new Thread(new Runnable() {
			@Override
			public void run() {
				process();
			}
		}, "OADateChangeNotifier");
		thread.setDaemon(true);
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}

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

	public static void main(String[] args) {
		OADateChangeController.onChange(() -> System.out.println("asdfa"));
	}
}
