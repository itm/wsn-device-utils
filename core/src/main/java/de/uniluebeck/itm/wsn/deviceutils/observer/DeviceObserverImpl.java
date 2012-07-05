/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or        *
 *   promote products derived from this software without specific prior written permission.                           *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.wsn.deviceutils.observer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import de.uniluebeck.itm.wsn.deviceutils.macreader.DeviceMacReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

class DeviceObserverImpl implements DeviceObserver {

	private static final Logger log = LoggerFactory.getLogger(DeviceObserver.class);

	@Inject
	private DeviceObserverListenerManager listenerManager;

	@Inject
	private DeviceMacReader macReader;

	@Inject
	private DeviceCsvProvider csvProvider;

	@Inject
	private DeviceInfoCsvParser csvParser;

	private ImmutableMap<String, DeviceInfo> currentState = ImmutableMap.of();

	@Override
	public ImmutableList<DeviceEvent> getEvents(final ImmutableMap<String, DeviceInfo> lastState) {
		return deriveEvents(lastState, currentState);
	}

	@Override
	public ImmutableMap<String, DeviceInfo> updateState() {
		return updateState(true);
	}

	@Override
	public ImmutableMap<String, DeviceInfo> updateState(boolean readMacAddress) {

		final ImmutableMap<String, DeviceInfo> oldState = currentState;
		currentState = ImmutableMap.copyOf(csvParser.parseCsv(csvProvider.getDeviceCsv()));

		if (readMacAddress) {

			for (Map.Entry<String, DeviceInfo> currentStateEntry : currentState.entrySet()) {

				if (oldState.containsKey(currentStateEntry.getKey())) {

					DeviceInfo oldDeviceInfo = oldState.get(currentStateEntry.getKey());
					DeviceInfo newDeviceInfo = currentStateEntry.getValue();

					if (oldDeviceInfo.getMacAddress() != null) {
						newDeviceInfo.macAddress = oldDeviceInfo.macAddress;
					} else {
						tryToEnrichWithMacAddress(newDeviceInfo);
					}
				} else {
					tryToEnrichWithMacAddress(currentStateEntry.getValue());
				}
			}
		}

		return oldState;
	}

	@Override
	public ImmutableMap<String, DeviceInfo> getCurrentState() {
		return currentState;
	}

	@Override
	public void run() {

		if (listenerManager.getListeners().isEmpty()) {
			return;
		}

		updateState();

		for (DeviceObserverListener listener : listenerManager.getListeners()) {

			final ImmutableMap<String, DeviceInfo> listenerLastState = listenerManager.getLastState(listener);
			final ImmutableList<DeviceEvent> events = deriveEvents(listenerLastState, currentState);

			for (DeviceEvent event : events) {
				notifyListener(listener, event);
			}

			listenerManager.updateLastState(listener, currentState);
		}
	}

	@Override
	public void addListener(final DeviceObserverListener listener) {
		listenerManager.addListener(listener);
	}

	@Override
	public void removeListener(final DeviceObserverListener listener) {
		listenerManager.removeListener(listener);
	}

	private ImmutableList<DeviceEvent> deriveEvents(final Map<String, DeviceInfo> lastState,
													final Map<String, DeviceInfo> currentState) {

		final ImmutableList.Builder<DeviceEvent> resultBuilder = ImmutableList.builder();

		resultBuilder.addAll(deriveAttachedEvents(lastState, currentState));
		resultBuilder.addAll(deriveRemovedEvents(lastState, currentState));

		return resultBuilder.build();
	}

	private List<DeviceEvent> deriveAttachedEvents(final Map<String, DeviceInfo> lastState,
												   final Map<String, DeviceInfo> currentState) {

		List<DeviceEvent> events = Lists.newArrayList();

		for (DeviceInfo newInfo : currentState.values()) {

			if (lastState == null || !lastState.containsKey(newInfo.getPort())) {
				events.add(new DeviceEvent(DeviceEvent.Type.ATTACHED, newInfo));
			}
		}

		return events;
	}

	private void tryToEnrichWithMacAddress(final DeviceInfo deviceInfo) {
		try {
			deviceInfo.macAddress = macReader.readMac(deviceInfo.port, deviceInfo.type, null, deviceInfo.reference);
		} catch (Exception e) {
			log.trace("Could not read MAC address of {} node on port {}. Reason: {}",
					new Object[]{deviceInfo.type, deviceInfo.port, e}
			);
		}
	}

	private List<DeviceEvent> deriveRemovedEvents(final Map<String, DeviceInfo> lastState,
												  final Map<String, DeviceInfo> currentState) {

		if (lastState == null) {
			return newArrayList();
		}

		List<DeviceEvent> events = Lists.newArrayList();

		for (DeviceInfo lastStateDevice : lastState.values()) {

			if (!currentState.containsKey(lastStateDevice.getPort())) {
				events.add(new DeviceEvent(DeviceEvent.Type.REMOVED, lastStateDevice));
			}
		}

		return events;
	}

	private void notifyListener(final DeviceObserverListener listener, final DeviceEvent event) {
		try {
			listener.deviceEvent(event);
		} catch (Exception e) {
			log.warn("Exception occurred while notifying {} listener: {}", listener, e);
		}
	}
}
