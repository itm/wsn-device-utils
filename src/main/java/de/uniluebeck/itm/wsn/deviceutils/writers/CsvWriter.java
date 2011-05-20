package de.uniluebeck.itm.wsn.deviceutils.writers;

import de.uniluebeck.itm.tr.util.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class CsvWriter implements Writer {

	private final static org.slf4j.Logger log = LoggerFactory.getLogger(CsvWriter.class);

	private final BufferedWriter output;

	private final DateTimeFormatter timeFormatter = DateTimeFormat.fullDateTime();

	public CsvWriter(OutputStream out) throws IOException {
		this.output = new BufferedWriter(new OutputStreamWriter(out));
		this.output.write("\"Time\";\"Type\";\"Content as String\";\"Content as Hex-Bytes\";\"Unix-Timestamp\"\n");
	}

	@Override
	public void write(byte[] packet) {
		try {

			output.write("\", Hex=\"");
			output.write(StringUtils.toHexString(packet));
			output.write("\"");
			output.newLine();
			output.flush();

			output.write("\"");
			output.write(timeFormatter.print(new DateTime()));
			output.write("\";\"");
			output.write(StringUtils.replaceNonPrintableAsciiCharacters(new String(packet)));
			output.write("\";\"");
			output.write(StringUtils.toHexString(packet));
			output.write("\";\"");
			output.write("" + (System.currentTimeMillis() / 1000));
			output.write("\"");
			output.newLine();
			output.flush();
		} catch (IOException e) {
			log.warn("Unable to write message:" + e, e);
		}
	}

	@Override
	public void shutdown() {
		try {
			output.flush();
		} catch (IOException e) {
			log.warn("" + e, e);
		}
		try {
			output.close();
		} catch (IOException e) {
			log.warn("" + e, e);
		}
	}

}
