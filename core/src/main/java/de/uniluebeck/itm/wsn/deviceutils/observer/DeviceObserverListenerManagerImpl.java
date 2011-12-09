package de.uniluebeck.itm.wsn.deviceutils.observer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.collect.Maps.newHashMap;

public class DeviceObserverListenerManagerImpl implements DeviceObserverListenerManager {

	private final Lock listenerMapLock = new ReentrantLock();

	private Map<DeviceObserverListener, ImmutableMap<String, DeviceInfo>> listenerMap = newHashMap();

	@Override
	public ImmutableMap<String, DeviceInfo> getLastState(final DeviceObserverListener listener) {
		listenerMapLock.lock();
		try {
			if (!listenerMap.containsKey(listener)) {
				throw new IllegalArgumentException("The listener instance " + listener + " is not registered!");
			}
			return listenerMap.get(listener);
		} finally {
			listenerMapLock.unlock();
		}
	}

	@Override
	public void updateLastState(final DeviceObserverListener listener,
								final ImmutableMap<String, DeviceInfo> newState) {
		listenerMapLock.lock();
		try {
			if (!listenerMap.containsKey(listener)) {
				throw new IllegalArgumentException("The listener instance " + listener + " was not yet registered!");
			}
			listenerMap.put(listener, newState);
		} finally {
			listenerMapLock.unlock();
		}
	}

	@Override
	public ImmutableList<DeviceObserverListener> getListeners() {
		ImmutableList.Builder<DeviceObserverListener> builder = ImmutableList.builder();
		listenerMapLock.lock();
		try {
			builder.addAll(listenerMap.keySet());
		} finally {
			listenerMapLock.unlock();
		}
		return builder.build();
	}

	@Override
	public void addListener(final DeviceObserverListener listener) {
		listenerMapLock.lock();
		try {
			if (listenerMap.containsKey(listener)) {
				throw new IllegalArgumentException("The listener instance " + listener + " is already registered!");
			}
			listenerMap.put(listener, null);
		} finally {
			listenerMapLock.unlock();
		}
	}

	@Override
	public void removeListener(final DeviceObserverListener listener) {
		listenerMapLock.lock();
		try {
			listenerMap.remove(listener);
		} finally {
			listenerMapLock.unlock();
		}
	}
}
