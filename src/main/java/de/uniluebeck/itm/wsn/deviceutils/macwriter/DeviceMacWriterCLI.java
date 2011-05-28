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

package de.uniluebeck.itm.wsn.deviceutils.macwriter;

import com.google.common.io.Closeables;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.wsn.drivers.core.MacAddress;
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

public class DeviceMacWriterCLI {

	private static final Logger log = LoggerFactory.getLogger(DeviceMacWriterCLI.class);

	public static void main(String[] args) throws Exception {

		Logging.setLoggingDefaults();

		org.apache.log4j.Logger itmLogger = org.apache.log4j.Logger.getLogger("de.uniluebeck.itm");
		org.apache.log4j.Logger wisebedLogger = org.apache.log4j.Logger.getLogger("eu.wisebed");
        org.apache.log4j.Logger coaLogger = org.apache.log4j.Logger.getLogger("com.coalesenses");

		itmLogger.setLevel(Level.INFO);
		wisebedLogger.setLevel(Level.INFO);
		coaLogger.setLevel(Level.INFO);

		if (args.length < 3) {
			System.out.println(
					"Usage: " + DeviceMacWriterCLI.class.getSimpleName() + " SENSOR_TYPE SERIAL_PORT MAC_ADRESS"
			);
			System.out.println(
					"Example: " + DeviceMacWriterCLI.class.getSimpleName() + " isense /dev/ttyUSB0 0x1234"
			);
			System.exit(1);
		}

		long macAddressLower16 = StringUtils.parseHexOrDecLong(args[2]);
		final MacAddress macAddress = new MacAddress(new byte[] {
				0,
				0,
				0,
				0,
				0,
				0,
				(byte) (0xFF & (macAddressLower16 >> 8)),
				(byte) (0xFF & (macAddressLower16))
		});

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
				log.info("Writing MAC address {} of {} device at port {} done!",
						new Object[]{macAddress, deviceType, port}
				);
				closeConnection(operationQueue, connection);
			}

			@Override
			public void onFailure(Throwable throwable) {
				log.error("Writing MAC address failed with Exception: " + throwable, throwable);
				closeConnection(operationQueue, connection);
			}

			@Override
			public void onExecute() {
				log.info("Starting to write MAC address...");
			}

			@Override
			public void onCancel() {
				log.info("Writing MAC address was canceled!");
				closeConnection(operationQueue, connection);
			}
		};

		deviceAsync.writeMac(macAddress, 120000, callback);

	}

	private static void closeConnection(final OperationQueue operationQueue, final SerialPortConnection connection) {
		try {
			operationQueue.shutdown(false);
		} catch (Exception e) {
			log.error("Exception while shutting down operation queue: " + e, e);
		}
		Closeables.closeQuietly(connection);
	}

}
