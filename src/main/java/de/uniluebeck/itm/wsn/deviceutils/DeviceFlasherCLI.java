package de.uniluebeck.itm.wsn.deviceutils;

import com.google.common.io.Files;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.wsn.drivers.core.async.AsyncCallback;
import de.uniluebeck.itm.wsn.drivers.core.async.DeviceAsync;
import de.uniluebeck.itm.wsn.drivers.core.async.OperationQueue;
import de.uniluebeck.itm.wsn.drivers.core.async.thread.PausableExecutorOperationQueue;
import de.uniluebeck.itm.wsn.drivers.core.serialport.SerialPortConnection;
import de.uniluebeck.itm.wsn.drivers.factories.ConnectionFactory;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceAsyncFactory;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

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

		final String deviceType = args[0];
		final String port = args[1];

		final SerialPortConnection connection = ConnectionFactory.create(deviceType);
		connection.connect(port);

		if (!connection.isConnected()) {
			throw new RuntimeException("Connection to device at port \"" + args[1] + "\" could not be established!");
		}

		final OperationQueue operationQueue = new PausableExecutorOperationQueue();
		final DeviceAsync deviceAsync = DeviceAsyncFactory.create(deviceType, connection, operationQueue);

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
				operationQueue.shutdown(true);
			}

			@Override
			public void onFailure(Throwable throwable) {
				log.error("Flashing node failed with Exception: " + throwable, (Exception) throwable);
				connection.shutdown(true);
				operationQueue.shutdown(true);
			}

			@Override
			public void onExecute() {
				log.info("Starting flash operation...");
			}

			@Override
			public void onCancel() {
				log.info("Flashing was canceled!");
				connection.shutdown(true);
				operationQueue.shutdown(true);
			}
		};
		deviceAsync.program(Files.toByteArray(new File(args[2])), 120000, callback);

	}

}
