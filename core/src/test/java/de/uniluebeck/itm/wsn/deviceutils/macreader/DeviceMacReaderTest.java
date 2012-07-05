package de.uniluebeck.itm.wsn.deviceutils.macreader;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import de.uniluebeck.itm.wsn.drivers.core.Device;
import de.uniluebeck.itm.wsn.drivers.core.MacAddress;
import de.uniluebeck.itm.wsn.drivers.core.operation.OperationFuture;
import de.uniluebeck.itm.wsn.drivers.core.operation.OperationListener;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactory;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DeviceMacReaderTest {

	private DeviceMacReader deviceMacReader;

	@Mock
	private ExecutorService executorService;

	@Mock
	private DeviceFactory deviceFactory;

	@Mock
	private DeviceMacReferenceMap deviceMacReferenceMap;

	@Mock
	private Device device;

	@Mock
	private OperationFuture<MacAddress> future;

	private final byte[] device64BitMacAddressBytes = new byte[]{0x0, 0x5, 0x0, 0x0, 0x0, 0x0, 0x0, 0x3};

	private final MacAddress device64BitMacAddress = new MacAddress(device64BitMacAddressBytes);

	private final byte[] device16BitMacAddressBytes = new byte[]{0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x3};

	private final MacAddress device16BitMacAddress = new MacAddress(device16BitMacAddressBytes);

	private String deviceTypeString = "isense";

	private String port = "/dev/ttyUSB0";

	private DeviceType deviceType = DeviceType.ISENSE;

	public void setUp(final boolean use16BitMode) throws Exception {

		final Injector injector = Guice.createInjector(new Module() {
			@Override
			public void configure(final Binder binder) {
				binder.bind(ExecutorService.class).toInstance(executorService);
				binder.bind(DeviceFactory.class).toInstance(deviceFactory);
				binder.bind(DeviceMacReferenceMap.class).toInstance(deviceMacReferenceMap);
				binder.bind(Boolean.class).annotatedWith(Names.named("use16BitMode")).toInstance(use16BitMode);
				binder.bind(DeviceMacReader.class).to(DeviceMacReaderImpl.class);
			}
		}
		);

		deviceMacReader = injector.getInstance(DeviceMacReader.class);

		when(deviceFactory.create(executorService, deviceType, null)).thenReturn(device);
		doNothing().when(device).connect(port);
		when(device.isConnected()).thenReturn(true);
		when(device.readMac(Matchers.anyInt(), Matchers.<OperationListener<MacAddress>>any())).thenReturn(future);
		when(future.get()).thenReturn(device64BitMacAddress);
	}

	@Test
	public void test16BitMode() throws Exception {
		setUp(true);
		final MacAddress macAddress = deviceMacReader.readMac(port, deviceTypeString, null, null);
		assertEquals(device16BitMacAddress, macAddress);
	}

	@Test
	public void test64BitMode() throws Exception {
		setUp(false);
		final MacAddress macAddress = deviceMacReader.readMac(port, deviceTypeString, null, null);
		assertEquals(device64BitMacAddress, macAddress);
	}
}
