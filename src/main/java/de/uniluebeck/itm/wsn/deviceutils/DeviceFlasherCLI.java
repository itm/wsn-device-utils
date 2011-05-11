package de.uniluebeck.itm.wsn.deviceutils;

import java.io.File;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.wsn.drivers.core.Device;
import de.uniluebeck.itm.wsn.drivers.core.async.AsyncCallback;
import de.uniluebeck.itm.wsn.drivers.core.async.DeviceAsync;
import de.uniluebeck.itm.wsn.drivers.core.async.OperationQueue;
import de.uniluebeck.itm.wsn.drivers.core.async.QueuedDeviceAsync;
import de.uniluebeck.itm.wsn.drivers.core.async.thread.PausableExecutorOperationQueue;
import de.uniluebeck.itm.wsn.drivers.core.operation.ProgramOperation;
import de.uniluebeck.itm.wsn.drivers.core.serialport.SerialPortConnection;
import de.uniluebeck.itm.wsn.drivers.jennic.JennicDevice;
import de.uniluebeck.itm.wsn.drivers.jennic.JennicSerialPortConnection;
import de.uniluebeck.itm.wsn.drivers.pacemate.PacemateDevice;
import de.uniluebeck.itm.wsn.drivers.pacemate.PacemateSerialPortConnection;
import de.uniluebeck.itm.wsn.drivers.telosb.TelosbDevice;
import de.uniluebeck.itm.wsn.drivers.telosb.TelosbSerialPortConnection;

public class DeviceFlasherCLI {

	private static final Logger log = LoggerFactory.getLogger(DeviceFlasherCLI.class);

	public static void main(String[] args) throws Exception {

		Logging.setLoggingDefaults();

		org.apache.log4j.Logger itmLogger = org.apache.log4j.Logger.getLogger("de.uniluebeck.itm");
		org.apache.log4j.Logger wisebedLogger = org.apache.log4j.Logger.getLogger("eu.wisebed");
		org.apache.log4j.Logger coaLogger = org.apache.log4j.Logger.getLogger("com.coalesenses");

		itmLogger.setLevel(Level.INFO);
		wisebedLogger.setLevel(Level.INFO);
		coaLogger.setLevel(Level.INFO);

		if (args.length < 3) {
			System.out.println("Usage: " + DeviceFlasherCLI.class.getSimpleName()
					+ " SENSOR_TYPE SERIAL_PORT IMAGE_FILE");
			System.out.println("Example: " + DeviceFlasherCLI.class.getSimpleName()
					+ " isense /dev/ttyUSB0 demoapplication.bin");
			System.exit(1);
		}

		final SerialPortConnection connection;
		if ("isense".equals(args[0])) {
			connection = new JennicSerialPortConnection();
		} else if ("pacemate".equals(args[0])) {
			connection = new PacemateSerialPortConnection();
		} else if ("telosb".equals(args[0])) {
			connection = new TelosbSerialPortConnection();
		} else {
			throw new RuntimeException("Device type \"" + args[0] + "\" unknown!");
		}

		connection.connect(args[1]);

		if (!connection.isConnected()) {
			throw new RuntimeException("Connection to device at port \"" + args[1] + "\" could not be established!");
		}

		final Device<SerialPortConnection> device;
		if ("isense".equals(args[0])) {
			device = new JennicDevice(connection);
		} else if ("pacemate".equals(args[0])) {
			device = new PacemateDevice(connection);
		} else if ("telosb".equals(args[0])) {
			device = new TelosbDevice(connection);
		} else {
			throw new RuntimeException("Device type " + args[0] + " unknown!");
		}

		ProgramOperation programOperation = device.createProgramOperation();
		programOperation.setTimeout(120000);

		final OperationQueue queue = new PausableExecutorOperationQueue();
		DeviceAsync deviceAsync = new QueuedDeviceAsync(queue, device);

		AsyncCallback<Void> callback = new AsyncCallback<Void>() {
			private int lastProgress = -1;
			@Override
			public void onProgressChange(float fraction) {
				int newProgress = (int) Math.floor(fraction * 100);
				if (lastProgress < newProgress) {
					lastProgress = newProgress;
					log.info("Progress: {}%", newProgress);
				}
				
			}
			@Override
			public void onSuccess(Void result) {
				log.info("Progress: {}%", 100);
				log.info("Flashing node done!");
				connection.shutdown(true);
				queue.shutdown(true);
			}
			@Override
			public void onFailure(Throwable throwable) {
				log.error("Flashing node failed with Exception: " + throwable, (Exception) throwable);
				connection.shutdown(true);
				queue.shutdown(true);
			}
			@Override
			public void onExecute() {
				log.info("Starting flash operation...");
			}
			@Override
			public void onCancel() {
				log.info("Flashing was canceled!");
				connection.shutdown(true);
				queue.shutdown(true);
			}
		};
		deviceAsync.program(com.google.common.io.Files.toByteArray(new File(args[2])), 120000, callback);

	}

}
