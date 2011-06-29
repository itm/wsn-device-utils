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

package de.uniluebeck.itm.wsn.deviceutils.flasher;

import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.tr.util.ForwardingScheduledExecutorService;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.wsn.drivers.core.Connection;
import de.uniluebeck.itm.wsn.drivers.core.async.AsyncCallback;
import de.uniluebeck.itm.wsn.drivers.core.async.DeviceAsync;
import de.uniluebeck.itm.wsn.drivers.core.async.ExecutorServiceOperationQueue;
import de.uniluebeck.itm.wsn.drivers.core.async.OperationQueue;
import de.uniluebeck.itm.wsn.drivers.factories.ConnectionFactoryImpl;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceAsyncFactoryImpl;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactoryImpl;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
					+ " isense /dev/ttyUSB0 application.bin");
			System.exit(1);
		}

		final String deviceType = args[0];
		final String port = args[1];

		final Connection connection = new ConnectionFactoryImpl().create(deviceType);
		connection.connect(port);

		if (!connection.isConnected()) {
			throw new RuntimeException("Connection to device at port \"" + args[1] + "\" could not be established!");
		}

		final ScheduledExecutorService scheduleService = Executors.newScheduledThreadPool(1,
				new ThreadFactoryBuilder().setNameFormat("DeviceFlasherScheduler-Thread %d").build()
		);
		final ExecutorService executorService = Executors.newCachedThreadPool(
				new ThreadFactoryBuilder().setNameFormat("DeviceFlasher-Thread %d").build()
		);
		final ForwardingScheduledExecutorService delegate = new ForwardingScheduledExecutorService(scheduleService, 
				executorService);
		final OperationQueue operationQueue = new ExecutorServiceOperationQueue(delegate, new SimpleTimeLimiter(delegate));
		final DeviceAsync deviceAsync = new DeviceAsyncFactoryImpl(new DeviceFactoryImpl()).create(delegate, deviceType, connection, operationQueue);

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
				closeConnection(delegate, connection);
			}

			@Override
			public void onFailure(Throwable throwable) {
				log.error("Flashing node failed with Exception: " + throwable, throwable);
				closeConnection(delegate, connection);
			}

			@Override
			public void onExecute() {
				log.info("Starting flash operation...");
			}

			@Override
			public void onCancel() {
				log.info("Flashing was canceled!");
				closeConnection(delegate, connection);
			}
		};

		deviceAsync.program(Files.toByteArray(new File(args[2])), 120000, callback);

	}

	private static void closeConnection(final ExecutorService executorService, final Connection connection) {
		Closeables.closeQuietly(connection);
		ExecutorUtils.shutdown(executorService, 10, TimeUnit.SECONDS);
	}

}
