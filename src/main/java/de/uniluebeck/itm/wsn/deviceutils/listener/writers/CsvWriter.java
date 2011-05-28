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

package de.uniluebeck.itm.wsn.deviceutils.listener.writers;

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
