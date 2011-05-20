package de.uniluebeck.itm.wsn.deviceutils.writers;

import de.uniluebeck.itm.tr.util.StringUtils;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class HumanReadableWriter implements Writer {
	private final static org.slf4j.Logger log = LoggerFactory.getLogger(HumanReadableWriter.class);

	private final BufferedWriter output;

	public HumanReadableWriter(OutputStream out) {
		this.output = new BufferedWriter(new OutputStreamWriter(out));
	}

	@Override
	public void write(byte[] packet) {
		try {
			output.write("String=\"");
			output.write(StringUtils.replaceNonPrintableAsciiCharacters(new String(packet)));
			output.write("\", Hex=\"");
			output.write(StringUtils.toHexString(packet));
			output.write("\"");
			output.newLine();
			output.flush();
		} catch (IOException e) {
			log.warn("Unable to write messge:" + e, e);
		}
	}

	@Override
	public void shutdown() {
		try {
			output.flush();
		} catch (IOException e) {
			log.warn("" + e, e);
		}
	}

}
