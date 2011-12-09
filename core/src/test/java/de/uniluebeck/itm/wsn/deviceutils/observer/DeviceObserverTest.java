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


import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import de.uniluebeck.itm.wsn.deviceutils.macreader.DeviceMacReader;
import de.uniluebeck.itm.wsn.drivers.core.MacAddress;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeviceObserverTest {

	private DeviceObserver deviceObserver;

	@Mock
	private DeviceCsvProvider deviceCsvProvider;

	@Mock
	private DeviceMacReader deviceMacReader;

	@Mock
	private DeviceObserverListener deviceObserverListener;

	@Mock
	private DeviceObserverListener deviceObserverListener2;

	private final String device1Csv = "01234,/dev/ttyUSB0,isense";

	private final String device2Csv = "12345,/dev/ttyUSB1,telosb";

	private final String device3Csv = "23456,/dev/ttyUSB2,pacemate";

	private final String device4Csv = "34567,/dev/ttyUSB3,isense";

	private DeviceInfo device1Info = new DeviceInfo("isense", "/dev/ttyUSB0", "01234", null);

	private DeviceInfo device2Info = new DeviceInfo("telosb", "/dev/ttyUSB1", "12345", null);

	private DeviceInfo device3Info = new DeviceInfo("pacemate", "/dev/ttyUSB2", "23456", null);

	private DeviceInfo device4Info = new DeviceInfo("isense", "/dev/ttyUSB3", "34567", null);

	private final DeviceEvent device1AttachedEvent = new DeviceEvent(DeviceEvent.Type.ATTACHED, device1Info);

	private final DeviceEvent device2AttachedEvent = new DeviceEvent(DeviceEvent.Type.ATTACHED, device2Info);

	private final DeviceEvent device3AttachedEvent = new DeviceEvent(DeviceEvent.Type.ATTACHED, device3Info);

	private final DeviceEvent device4AttachedEvent = new DeviceEvent(DeviceEvent.Type.ATTACHED, device4Info);

	private final DeviceEvent device1RemovedEvent = new DeviceEvent(DeviceEvent.Type.REMOVED, device1Info);

	private final DeviceEvent device2RemovedEvent = new DeviceEvent(DeviceEvent.Type.REMOVED, device2Info);

	private final DeviceEvent device3RemovedEvent = new DeviceEvent(DeviceEvent.Type.REMOVED, device3Info);

	private final DeviceEvent device4RemovedEvent = new DeviceEvent(DeviceEvent.Type.REMOVED, device4Info);

	private final byte[] device1MacAddressBytes =
			new byte[]{0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x3};

	private final MacAddress device1MacAddress = new MacAddress(device1MacAddressBytes);


	@Before
	public void setUp() {
		final Injector injector = Guice.createInjector(new Module() {
			@Override
			public void configure(final Binder binder) {
				binder.bind(DeviceMacReader.class).toInstance(deviceMacReader);
				binder.bind(DeviceCsvProvider.class).toInstance(deviceCsvProvider);
				binder.bind(DeviceInfoCsvParser.class).to(DeviceInfoCsvParserImpl.class);
				binder.bind(DeviceObserverListenerManager.class).to(DeviceObserverListenerManagerImpl.class);
				binder.bind(DeviceObserver.class).to(DeviceObserverImpl.class);
			}
		}
		);
		deviceObserver = injector.getInstance(DeviceObserver.class);
	}

	@Test
	public void noDeviceFoundNoDeviceAttached() {
		setObserverStateForCsvRows();
		assertTrue(deviceObserver.getEvents(null).isEmpty());
	}

	@Test
	public void singleDeviceFoundWhenNoDevicesWereAttachedBefore() {

		setObserverStateForCsvRows();

		final ImmutableList<DeviceEvent> actualEvents = getObserverEventsForCsvRows(null, device1Csv);

		assertEqualEvents(actualEvents, device1AttachedEvent);
	}

	@Test
	public void singleDeviceFoundWhenSomeDevicesWereAttachedBefore() {

		getObserverEventsForCsvRows(null, device1Csv, device2Csv);
		ImmutableMap<String, DeviceInfo> lastState = deviceObserver.getCurrentState();

		final ImmutableList<DeviceEvent> events =
				getObserverEventsForCsvRows(lastState, device1Csv, device2Csv, device3Csv);

		assertEquals(1, events.size());
		final DeviceEvent actualEvent = events.iterator().next();
		assertEquals(device3AttachedEvent, actualEvent);
	}

	@Test
	public void multipleDevicesFoundWhenNoDevicesWereAttachedBefore() {

		setObserverStateForCsvRows();

		final ImmutableList<DeviceEvent> events = getObserverEventsForCsvRows(null, device1Csv, device2Csv);

		assertEqualEvents(events, device1AttachedEvent, device2AttachedEvent);
	}

	@Test
	public void multipleDevicesFoundWhenMultipleDevicesWereAttachedBefore() {

		setObserverStateForCsvRows(device1Csv, device2Csv);
		ImmutableMap<String, DeviceInfo> lastState = deviceObserver.getCurrentState();

		final ImmutableList<DeviceEvent> events =
				getObserverEventsForCsvRows(lastState, device1Csv, device2Csv, device3Csv, device4Csv);

		assertEqualEvents(events, device3AttachedEvent, device4AttachedEvent);
	}

	@Test
	public void singleDeviceRemovedWhenOnlyOneDeviceWasAttachedBefore() {

		setObserverStateForCsvRows(device1Csv);
		ImmutableMap<String, DeviceInfo> lastState = deviceObserver.getCurrentState();

		final ImmutableList<DeviceEvent> events = getObserverEventsForCsvRows(lastState);

		assertEqualEvents(events, device1RemovedEvent);
	}

	@Test
	public void singleDeviceRemovedWhenMultipleDevicesWereAttachedBefore() {

		setObserverStateForCsvRows(device1Csv, device2Csv);
		ImmutableMap<String, DeviceInfo> lastState = deviceObserver.getCurrentState();

		final ImmutableList<DeviceEvent> events = getObserverEventsForCsvRows(lastState, device1Csv);

		assertEqualEvents(events, device2RemovedEvent);
	}

	@Test
	public void multipleDevicesRemovedWhenOnlyTheseWereAttachedBefore() {

		setObserverStateForCsvRows(device1Csv, device2Csv);
		ImmutableMap<String, DeviceInfo> lastState = deviceObserver.getCurrentState();

		final ImmutableList<DeviceEvent> events = getObserverEventsForCsvRows(lastState);

		assertEqualEvents(events, device1RemovedEvent, device2RemovedEvent);
	}

	@Test
	public void multipleDevicesRemovedWhenSomeDevicesBefore() {

		setObserverStateForCsvRows(device1Csv, device2Csv, device3Csv, device4Csv);
		ImmutableMap<String, DeviceInfo> lastState = deviceObserver.getCurrentState();

		final ImmutableList<DeviceEvent> events = getObserverEventsForCsvRows(lastState, device1Csv, device2Csv);

		assertEqualEvents(events, device3RemovedEvent, device4RemovedEvent);
	}

	@Test
	public void readMacFromDevice() throws Exception {

		setObserverStateForCsvRows();

		when(deviceMacReader.readMac(device1Info.getPort(), device1Info.getType(), device1Info.getReference()))
				.thenReturn(device1MacAddress);

		final ImmutableList<DeviceEvent> events = getObserverEventsForCsvRows(null, device1Csv);

		assertEquals(device1MacAddress, events.get(0).getDeviceInfo().getMacAddress());
	}

	/**
	 * https://github.com/itm/wsn-device-utils/issues/36
	 *
	 * @throws Exception
	 * 		if the test fails
	 */
	@Test
	public void testNoLoadingAndEvaluationWillBeDoneUnlessAtLeastOneListenerIsRegistered() throws Exception {

		setCsvProviderState();

		deviceObserver.run();
		verify(deviceCsvProvider, never()).getDeviceCsv();

		deviceObserver.addListener(deviceObserverListener);
		deviceObserver.run();
		verify(deviceCsvProvider).getDeviceCsv();
	}

	/**
	 * https://github.com/itm/wsn-device-utils/issues/31
	 *
	 * @throws Exception
	 * 		if the test fails
	 */
	@Test
	public void testIfRemovedEventHoldsMacAddressWhenItWasKnownBefore() throws Exception {

		MacAddress expectedMacAddress = new MacAddress("0x0123");

		setCsvProviderState(device1Csv);
		when(deviceMacReader.readMac(device1Info.getPort(), device1Info.getType(), device1Info.getReference()))
				.thenReturn(expectedMacAddress);

		deviceObserver.updateState();
		ImmutableMap<String, DeviceInfo> lastState = deviceObserver.getCurrentState();

		// simulate some time passing (which introduced the original bug)
		deviceObserver.updateState();
		deviceObserver.updateState();

		setCsvProviderState();
		deviceObserver.updateState();
		ImmutableList<DeviceEvent> events = deviceObserver.getEvents(lastState);

		assertEquals(1, events.size());
		assertEquals(DeviceEvent.Type.REMOVED, events.get(0).getType());
		assertEquals(expectedMacAddress, events.get(0).getDeviceInfo().getMacAddress());
	}

	/**
	 * https://github.com/itm/wsn-device-utils/issues/37
	 *
	 * @throws Exception
	 * 		if the test fails
	 */
	@Test
	public void testIfListenerGetsFullDiffToNullStateEvenIfNoChangesOccurred() throws Exception {

		setCsvProviderState(device1Csv);
		deviceObserver.run();

		deviceObserver.addListener(deviceObserverListener);
		deviceObserver.run();

		ArgumentCaptor<DeviceEvent> argumentCaptor = ArgumentCaptor.forClass(DeviceEvent.class);
		verify(deviceObserverListener).deviceEvent(argumentCaptor.capture());

		assertEqualEvents(argumentCaptor.getAllValues(), device1AttachedEvent);
	}

	/**
	 * https://github.com/itm/wsn-device-utils/issues/37
	 *
	 * @throws Exception
	 * 		if the test fails
	 */
	@Test
	public void testIfSecondListenerGetsFullDiffAlthoughFirstListenerGetsNone() throws Exception {

		setCsvProviderState(device1Csv);
		deviceObserver.run();

		deviceObserver.addListener(deviceObserverListener);
		deviceObserver.run();

		ArgumentCaptor<DeviceEvent> argumentCaptor = ArgumentCaptor.forClass(DeviceEvent.class);
		verify(deviceObserverListener).deviceEvent(argumentCaptor.capture());
		assertEqualEvents(argumentCaptor.getAllValues(), device1AttachedEvent);

		deviceObserver.addListener(deviceObserverListener2);
		deviceObserver.run();

		ArgumentCaptor<DeviceEvent> argumentCaptor2 = ArgumentCaptor.forClass(DeviceEvent.class);
		verify(deviceObserverListener2).deviceEvent(argumentCaptor2.capture());
		assertEquals(argumentCaptor2.getValue(), device1AttachedEvent);
	}

	private ImmutableList<DeviceEvent> getObserverEventsForCsvRows(
			@Nullable final ImmutableMap<String, DeviceInfo> lastState,
			final String... csvRows) {

		setCsvProviderState(csvRows);
		deviceObserver.updateState();
		return deviceObserver.getEvents(lastState);
	}

	private void setObserverStateForCsvRows(final String... csvRows) {
		setCsvProviderState(csvRows);
		deviceObserver.updateState();
	}

	private void setCsvProviderState(final String... csvRows) {
		if (csvRows.length == 0) {
			when(deviceCsvProvider.getDeviceCsv()).thenReturn("");
		} else {
			when(deviceCsvProvider.getDeviceCsv()).thenReturn(Joiner.on("\n").join(csvRows));
		}
	}

	private void assertEqualEvents(final List<DeviceEvent> actualEvents, final DeviceEvent... expectedEvents) {
		assertEquals(newHashSet(expectedEvents), newHashSet(actualEvents));
	}

	private void assertEqualEvents(final ImmutableList<DeviceEvent> actualEvents,
								   final DeviceEvent... expectedEvents) {
		assertEquals(newHashSet(expectedEvents), newHashSet(actualEvents));
	}

}
