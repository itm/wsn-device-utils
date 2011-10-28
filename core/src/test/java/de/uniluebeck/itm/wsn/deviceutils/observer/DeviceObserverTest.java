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
import com.google.common.collect.UnmodifiableIterator;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import de.uniluebeck.itm.wsn.deviceutils.macreader.DeviceMacReader;
import de.uniluebeck.itm.wsn.drivers.core.MacAddress;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DeviceObserverTest {

	private DeviceObserver deviceObserver;

	@Mock
	private DeviceCsvProvider deviceCsvProvider;

	@Mock
	private DeviceMacReader deviceMacReader;

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
				binder.bind(DeviceObserver.class).to(DeviceObserverImpl.class);
			}
		}
		);
		deviceObserver = injector.getInstance(DeviceObserver.class);
	}

	@Test
	public void noDeviceFoundNoDeviceAttached() {
		setObserverStateForCsvRows();
		assertEquals(0, deviceObserver.getEvents().size());
	}

	@Test
	public void singleDeviceFoundWhenNoDevicesWereAttachedBefore() {

		setObserverStateForCsvRows();

		final ImmutableList<DeviceEvent> events = getObserverEventsForCsvRows(device1Csv);

		assertEquals(1, events.size());
		final DeviceEvent actualEvent = events.iterator().next();
		assertEquals(device1AttachedEvent, actualEvent);
	}

	@Test
	public void singleDeviceFoundWhenSomeDevicesWereAttachedBefore() {

		setObserverStateForCsvRows(device1Csv, device2Csv);

		final ImmutableList<DeviceEvent> events = getObserverEventsForCsvRows(device1Csv, device2Csv, device3Csv);

		assertEquals(1, events.size());
		final DeviceEvent actualEvent = events.iterator().next();
		assertEquals(device3AttachedEvent, actualEvent);
	}

	@Test
	public void multipleDevicesFoundWhenNoDevicesWereAttachedBefore() {

		setObserverStateForCsvRows();

		final ImmutableList<DeviceEvent> events = getObserverEventsForCsvRows(device1Csv, device2Csv);

		assertEquals(2, events.size());
		final UnmodifiableIterator<DeviceEvent> iterator = events.iterator();
		assertEquals(device1AttachedEvent, iterator.next());
		assertEquals(device2AttachedEvent, iterator.next());
	}

	@Test
	public void multipleDevicesFoundWhenMultipleDevicesWereAttachedBefore() {

		setObserverStateForCsvRows(device1Csv, device2Csv);

		final ImmutableList<DeviceEvent> events =
				getObserverEventsForCsvRows(device1Csv, device2Csv, device3Csv, device4Csv);
		assertEquals(2, events.size());
		final UnmodifiableIterator<DeviceEvent> iterator = events.iterator();
		assertEquals(device3AttachedEvent, iterator.next());
		assertEquals(device4AttachedEvent, iterator.next());
	}

	@Test
	public void singleDeviceRemovedWhenOnlyOneDeviceWasAttachedBefore() {

		setObserverStateForCsvRows(device1Csv);

		final ImmutableList<DeviceEvent> events = getObserverEventsForCsvRows();
		assertEquals(1, events.size());
		assertEquals(device1RemovedEvent, events.iterator().next());
	}

	@Test
	public void singleDeviceRemovedWhenMultipleDevicesWereAttachedBefore() {

		setObserverStateForCsvRows(device1Csv, device2Csv);

		final ImmutableList<DeviceEvent> events = getObserverEventsForCsvRows(device1Csv);
		assertEquals(1, events.size());
		assertEquals(device2RemovedEvent, events.iterator().next());
	}

	@Test
	public void multipleDevicesRemovedWhenOnlyTheseWereAttachedBefore() {

		setObserverStateForCsvRows(device1Csv, device2Csv);

		final ImmutableList<DeviceEvent> events = getObserverEventsForCsvRows();
		assertEquals(2, events.size());
		assertEquals(device1RemovedEvent, events.get(0));
		assertEquals(device2RemovedEvent, events.get(1));
	}

	@Test
	public void multipleDevicesRemovedWhenSomeDevicesBefore() {

		setObserverStateForCsvRows(device1Csv, device2Csv, device3Csv, device4Csv);

		final ImmutableList<DeviceEvent> events = getObserverEventsForCsvRows(device1Csv, device2Csv);
		assertEquals(2, events.size());
		assertEquals(device3RemovedEvent, events.get(0));
		assertEquals(device4RemovedEvent, events.get(1));
	}

	@Test
	public void readMacFromDevice() throws Exception {

		setObserverStateForCsvRows();

		when(deviceMacReader.readMac(device1Info.getPort(), device1Info.getType(), device1Info.getReference()))
				.thenReturn(device1MacAddress);

		final ImmutableList<DeviceEvent> events = getObserverEventsForCsvRows(device1Csv);

		assertEquals(device1MacAddress, events.get(0).getDeviceInfo().getMacAddress());
	}

	private ImmutableList<DeviceEvent> getObserverEventsForCsvRows(final String... csvRows) {
		setCsvProviderState(csvRows);
		return deviceObserver.getEvents();
	}

	private void setObserverStateForCsvRows(final String... csvRows) {
		setCsvProviderState(csvRows);
		deviceObserver.getEvents();
	}

	private void setCsvProviderState(final String... csvRows) {
		if (csvRows.length == 0) {
			when(deviceCsvProvider.getDeviceCsv()).thenReturn("");
		} else {
			when(deviceCsvProvider.getDeviceCsv()).thenReturn(Joiner.on("\n").join(csvRows));
		}
	}

}
