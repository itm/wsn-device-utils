package de.uniluebeck.itm.wsn.deviceutils;

import com.google.common.base.Joiner;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.wsn.deviceutils.writers.CsvWriter;
import de.uniluebeck.itm.wsn.deviceutils.writers.HumanReadableWriter;
import de.uniluebeck.itm.wsn.deviceutils.writers.WiseMLWriter;
import de.uniluebeck.itm.wsn.deviceutils.writers.Writer;
import de.uniluebeck.itm.wsn.drivers.core.Connection;
import de.uniluebeck.itm.wsn.drivers.core.Device;
import de.uniluebeck.itm.wsn.drivers.core.serialport.SerialPortConnection;
import de.uniluebeck.itm.wsn.drivers.factories.ConnectionFactory;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactory;
import org.apache.commons.cli.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

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

		final SerialPortConnection connection = ConnectionFactory.create(deviceType);
		connection.connect(port);

		if (!connection.isConnected()) {
			throw new RuntimeException("Connection to device at port \"" + args[1] + "\" could not be established!");
		}

		final Device<? extends Connection> device = DeviceFactory.create(deviceType, connection);

		// TODO attach netty and fragment decoders to device InputStream and print to writer

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
