package de.uniluebeck.itm.wsn.deviceutils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

public class CliUtils {

	public static void assertParametersPresent(final CommandLine line, char... parameter) throws Exception {
		Set<Character> missingParameters = newHashSet();
		for (char p : parameter) {
			if (!line.hasOption(p)) {
				missingParameters.add(p);
			}
		}
		if (!missingParameters.isEmpty()) {
			throw new Exception("Command line parameter(s) " + missingParameters + " are missing!");
		}
	}

	public static void printUsageAndExit(Class<?> clazz, Options options, int exitCode) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(120, clazz.getCanonicalName(), null, options, null);
		System.exit(exitCode);
	}

}
