package de.uniluebeck.itm.wsn.deviceutils.observer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.uniluebeck.itm.tr.util.Listenable;

public interface DeviceObserverListenerManager extends Listenable<DeviceObserverListener> {

	ImmutableMap<String, DeviceInfo> getLastState(DeviceObserverListener listener);

	void updateLastState(DeviceObserverListener listener, ImmutableMap<String, DeviceInfo> newState);

	ImmutableList<DeviceObserverListener> getListeners();

}
