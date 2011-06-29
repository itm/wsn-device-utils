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

package de.uniluebeck.itm.wsn.deviceutils.listener;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.DefaultChannelPipeline;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.iostream.IOStreamAddress;
import org.jboss.netty.channel.iostream.IOStreamChannelFactory;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.uniluebeck.itm.netty.handlerstack.dlestxetx.DleStxEtxFramingDecoderFactory;
import de.uniluebeck.itm.netty.handlerstack.dlestxetx.DleStxEtxFramingEncoderFactory;
import de.uniluebeck.itm.tr.util.ForwardingScheduledExecutorService;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.tr.util.Tuple;
import de.uniluebeck.itm.wsn.deviceutils.listener.writers.CsvWriter;
import de.uniluebeck.itm.wsn.deviceutils.listener.writers.HumanReadableWriter;
import de.uniluebeck.itm.wsn.deviceutils.listener.writers.WiseMLWriter;
import de.uniluebeck.itm.wsn.deviceutils.listener.writers.Writer;
import de.uniluebeck.itm.wsn.drivers.core.Connection;
import de.uniluebeck.itm.wsn.drivers.core.async.DeviceAsync;
import de.uniluebeck.itm.wsn.drivers.core.async.ExecutorServiceOperationQueue;
import de.uniluebeck.itm.wsn.drivers.core.async.OperationQueue;
import de.uniluebeck.itm.wsn.drivers.factories.ConnectionFactoryImpl;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceAsyncFactoryImpl;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactoryImpl;

public class DeviceListenerCLI {

	private final static Level[] LOG_LEVELS = {Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR};

	private final static org.slf4j.Logger log = LoggerFactory.getLogger(DeviceListenerCLI.class);

	public static void main(String[] args) throws InterruptedException, IOException {

		Logging.setLoggingDefaults();

		CommandLineParser parser = new PosixParser();
		Options options = createCommandLineOptions();

		String deviceType = null;
		String port = null;

		OutputStream outStream = System.out;
		Writer outWriter = null;

		try {

			CommandLine line = parser.parse(options, args);

			if (line.hasOption('h')) {
				printUsageAndExit(options);
			}

			if (line.hasOption('v')) {
				Logger.getRootLogger().setLevel(Level.DEBUG);
			}

			if (line.hasOption('l')) {
				Level level = Level.toLevel(line.getOptionValue('l'));
				Logger.getRootLogger().setLevel(level);
			}

			deviceType = line.getOptionValue('t');
			port = line.getOptionValue('p');

			if (line.hasOption('o')) {
				String filename = line.getOptionValue('o');
				log.info("Using outfile {}", filename);
				outStream = new FileOutputStream(filename);
			}

			if (line.hasOption('f')) {

				String format = line.getOptionValue('f');

				if ("csv".equals(format)) {
					outWriter = new CsvWriter(outStream);
				} else if ("wiseml".equals(format)) {
					outWriter = new WiseMLWriter(outStream, "node at " + line.getOptionValue('p'), true);
				} else {
					throw new Exception("Unknown format " + format);
				}

				log.info("Using format {}", format);

			} else {
				outWriter = new HumanReadableWriter(outStream);
			}

		} catch (Exception e) {
			log.error("Invalid command line: " + e);
			printUsageAndExit(options);
		}

		if (outWriter == null) {
			throw new RuntimeException("This should not happen!");
		}

		final Writer finalOutWriter = outWriter;
		Runtime.getRuntime().addShutdownHook(new Thread(DeviceListenerCLI.class.getName() + "-ShutdownThread") {
			@Override
			public void run() {
				try {
					finalOutWriter.shutdown();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
		);

		final Connection connection = new ConnectionFactoryImpl().create(deviceType);
		connection.connect(port);

		if (!connection.isConnected()) {
			throw new RuntimeException("Connection to device at port \"" + args[1] + "\" could not be established!");
		}

		final ScheduledExecutorService scheduleService = Executors.newScheduledThreadPool(1,
				new ThreadFactoryBuilder().setNameFormat("DeviceMacWriter-Thread %d").build()
		);
		final ExecutorService executorService = Executors.newCachedThreadPool(
				new ThreadFactoryBuilder().setNameFormat("DeviceMacWriter-Thread %d").build()
		);
		final ForwardingScheduledExecutorService delegate = new ForwardingScheduledExecutorService(scheduleService, 
				executorService);
		OperationQueue operationQueue = new ExecutorServiceOperationQueue(delegate, new SimpleTimeLimiter(delegate));
		final DeviceAsync deviceAsync = new DeviceAsyncFactoryImpl(new DeviceFactoryImpl())
				.create(delegate, deviceType, connection, operationQueue);

		final InputStream inputStream = deviceAsync.getInputStream();
		final OutputStream outputStream = deviceAsync.getOutputStream();

		final ClientBootstrap bootstrap = new ClientBootstrap(new IOStreamChannelFactory(executorService));

		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {
				DefaultChannelPipeline pipeline = new DefaultChannelPipeline();

				final List<Tuple<String, ChannelHandler>> decoders = new DleStxEtxFramingDecoderFactory().create(
						"frameDecoder",
						HashMultimap.<String, String>create()
				);
				pipeline.addLast("frameDecoder", decoders.get(0).getSecond());

				final List<Tuple<String, ChannelHandler>> encoders = new DleStxEtxFramingEncoderFactory().create(
						"frameEncoder",
						HashMultimap.<String, String>create()
				);
				pipeline.addLast("frameEncoder", encoders.get(0).getSecond());

				pipeline.addLast("loggingHandler", new SimpleChannelHandler() {
					@Override
					public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e)
							throws Exception {
						final ChannelBuffer message = (ChannelBuffer) e.getMessage();
						byte[] messageBytes = new byte[message.readableBytes()];
						message.readBytes(messageBytes);
						finalOutWriter.write(messageBytes);
					}
				}
				);
				return pipeline;
			}
		}
		);

		// Make a new connection.
		ChannelFuture connectFuture = bootstrap.connect(new IOStreamAddress(inputStream, outputStream));

		// Wait until the connection is made successfully.
		connectFuture.awaitUninterruptibly().getChannel();

	}

	private static Options createCommandLineOptions() {

		Options options = new Options();

		// add all available options
		options.addOption("p", "port", true, "Serial port to use");
		options.getOption("p").setRequired(true);
		options.addOption("t", "type", true, "Device type");
		options.getOption("t").setRequired(true);

		options.addOption("f", "format", true, "Optional: Output format, options: csv, wiseml");
		options.addOption("o", "outfile", true, "Optional: Redirect output to file");
		options.addOption("v", "verbose", false, "Optional: Verbose logging output (equal to -l DEBUG)");
		options.addOption("l", "logging", true,
				"Optional: Set logging level (one of [" + Joiner.on(", ").join(LOG_LEVELS) + "])"
		);
		options.addOption("h", "help", false, "Help output");

		return options;
	}

	private static void printUsageAndExit(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(120, DeviceListenerCLI.class.getCanonicalName(), null, options, null);
		System.exit(1);
	}
}
