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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.wsn.deviceutils.DeviceUtilsModule;
import de.uniluebeck.itm.wsn.deviceutils.macreader.DeviceMacReferenceMap;
import de.uniluebeck.itm.wsn.drivers.core.MacAddress;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class DeviceObserverCLI {

	private static final Logger log = LoggerFactory.getLogger(DeviceObserverCLI.class);

	private static final int EXIT_CODE_REFERENCE_FILE_NOT_EXISTING = 2;

	private static final int EXIT_CODE_REFERENCE_FILE_NOT_READABLE = 3;

	private static final int EXIT_CODE_REFERENCE_FILE_IS_DIRECTORY = 4;

	public static void main(String[] args) throws IOException {

		Logging.setLoggingDefaults();

		org.apache.log4j.Logger.getLogger("de.uniluebeck.itm.wsn.drivers").setLevel(Level.ERROR);
		org.apache.log4j.Logger.getLogger("de.uniluebeck.itm").setLevel(Level.INFO);
		org.apache.log4j.Logger.getLogger("eu.wisebed").setLevel(Level.INFO);
		org.apache.log4j.Logger.getLogger("com.coalesenses").setLevel(Level.INFO);

		DeviceMacReferenceMap deviceMacReferenceMap = null;

		if (args.length > 0) {
			deviceMacReferenceMap = readDeviceMacReferenceMap(args[0]);
		}

		final DeviceObserver deviceObserver = Guice
				.createInjector(new DeviceUtilsModule(deviceMacReferenceMap))
				.getInstance(DeviceObserver.class);

		deviceObserver.addListener(new DeviceObserverListener() {
			@Override
			public void deviceEvent(final DeviceEvent event) {
				log.info("{}", event);
			}
		});

		final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("DeviceObserver %d").build();
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, threadFactory);
		scheduler.scheduleAtFixedRate(deviceObserver, 0, 1, TimeUnit.SECONDS);
	}

	private static DeviceMacReferenceMap readDeviceMacReferenceMap(final String fileName) throws IOException {

		final DeviceMacReferenceMap deviceMacReferenceMap;
		final File referenceToMacMapPropertiesFile = new File(fileName);

		if (!referenceToMacMapPropertiesFile.exists()) {
			log.error("Reference file {} does not exist!");
			System.exit(EXIT_CODE_REFERENCE_FILE_NOT_EXISTING);
		} else if (!referenceToMacMapPropertiesFile.canRead()) {
			log.error("Reference file {} is not readable!");
			System.exit(EXIT_CODE_REFERENCE_FILE_NOT_READABLE);
		} else if (referenceToMacMapPropertiesFile.isDirectory()) {
			log.error("Reference file {} is a directory!");
			System.exit(EXIT_CODE_REFERENCE_FILE_IS_DIRECTORY);
		}

		Properties properties = new Properties();
		properties.load(new FileInputStream(referenceToMacMapPropertiesFile));

		deviceMacReferenceMap = new DeviceMacReferenceMap();

		for (Object key : properties.keySet()) {
			final String value = (String) properties.get(key);
			deviceMacReferenceMap.put((String) key, new MacAddress(value));
		}

		return deviceMacReferenceMap;
	}

}
