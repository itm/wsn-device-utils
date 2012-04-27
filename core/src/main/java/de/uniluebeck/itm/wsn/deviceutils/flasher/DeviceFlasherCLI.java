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

import com.google.common.base.Joiner;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.wsn.deviceutils.DeviceUtilsModule;
import de.uniluebeck.itm.wsn.drivers.core.Device;
import de.uniluebeck.itm.wsn.drivers.core.operation.OperationListener;
import de.uniluebeck.itm.wsn.drivers.core.operation.StateChangedEvent;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Maps.newHashMap;
import static de.uniluebeck.itm.wsn.deviceutils.CliUtils.assertParametersPresent;
import static de.uniluebeck.itm.wsn.deviceutils.CliUtils.printUsageAndExit;

public class DeviceFlasherCLI {

	private final static Level[] LOG_LEVELS = {Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR};

	private static final Logger log = LoggerFactory.getLogger(DeviceFlasherCLI.class);

	public static void main(String[] args) throws Exception {

		Logging.setLoggingDefaults(Level.WARN);

		CommandLineParser parser = new PosixParser();
		Options options = createCommandLineOptions();

		String deviceType = null;
		String port = null;
		File imageFile = null;
		Map<String, String> configuration = newHashMap();

		try {

			CommandLine line = parser.parse(options, args, true);

			if (line.hasOption('h')) {
				printUsageAndExit(DeviceFlasherCLI.class, options, 0);
			}

			if (line.hasOption('v')) {
				org.apache.log4j.Logger.getRootLogger().setLevel(Level.DEBUG);
			}

			if (line.hasOption('l')) {
				Level level = Level.toLevel(line.getOptionValue('l'));
				org.apache.log4j.Logger.getRootLogger().setLevel(level);
			}

			if (line.hasOption('c')) {
				final String configurationFileString = line.getOptionValue('c');
				final File configurationFile = new File(configurationFileString);
				final Properties configurationProperties = new Properties();
				configurationProperties.load(new FileReader(configurationFile));
				for (Map.Entry<Object, Object> entry : configurationProperties.entrySet()) {
					configuration.put((String) entry.getKey(), (String) entry.getValue());
				}
			}

			assertParametersPresent(line, 't', 'p', 'i');

			deviceType = line.getOptionValue('t');
			port = line.getOptionValue('p');
			imageFile = new File(line.getOptionValue('i'));

		} catch (Exception e) {
			log.error("Invalid command line: " + e);
			printUsageAndExit(DeviceFlasherCLI.class, options, 1);
		}

		final ExecutorService executorService = Executors.newCachedThreadPool(
				new ThreadFactoryBuilder().setNameFormat("DeviceFlasher %d").build()
		);

		final Injector injector = Guice.createInjector(new DeviceUtilsModule(executorService, null));
		final Device device = injector.getInstance(DeviceFactory.class).create(executorService, deviceType);

		device.connect(port);
		if (!device.isConnected()) {
			throw new RuntimeException("Connection to device at port \"" + args[1] + "\" could not be established!");
		}

		OperationListener<Void> callback = new OperationListener<Void>() {
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
				log.info("Flashing node done!");
			}

			@Override
			public void onFailure(Throwable throwable) {
				log.error("Flashing node failed with Exception: " + throwable, throwable);
			}

			@Override
			public void beforeStateChanged(final StateChangedEvent<Void> stateChangedEvent) {
				log.info("Operation state about to change: {}", stateChangedEvent);
			}

			@Override
			public void afterStateChanged(final StateChangedEvent<Void> stateChangedEvent) {
				log.info("Operation state changed: {}", stateChangedEvent);
			}

			@Override
			public void onExecute() {
				log.info("Starting flash operation...");
			}

			@Override
			public void onCancel() {
				log.info("Flashing was canceled!");
			}
		};

		try {
			device.program(Files.toByteArray(imageFile), 120000, callback).get();
		} finally {
			closeConnection(executorService, device);
		}
	}

	private static void closeConnection(final ExecutorService executorService, final Closeable connection) {
		Closeables.closeQuietly(connection);
		ExecutorUtils.shutdown(executorService, 10, TimeUnit.SECONDS);
	}

	private static Options createCommandLineOptions() {

		Options options = new Options();

		// add all available options
		options.addOption("p", "port", true, "Serial port to which the device is attached");
		options.getOption("p").setRequired(true);

		options.addOption("t", "type", true, "Type of the device");
		options.getOption("t").setRequired(true);

		options.addOption("i", "image", true, "Image file to flash onto the device");
		options.getOption("i").setRequired(true);

		options.addOption("c", "configuration", true,
				"Optional: file name of a configuration file containing key value pairs to configure the device"
		);
		options.addOption("v", "verbose", false, "Optional: verbose logging output (equal to -l DEBUG)");
		options.addOption("l", "logging", true,
				"Optional: set logging level (one of [" + Joiner.on(", ").join(LOG_LEVELS) + "])"
		);
		options.addOption("h", "help", false, "Optional: print help");

		return options;
	}

}
