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

import org.apache.commons.codec.binary.Base64;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.io.OutputStream;

public class WiseMLWriterHandler extends WriterHandler {

	private final DateTimeFormatter timeFormatter = DateTimeFormat.fullDateTime();

	private final String nodeUrn;

	private boolean writeHeaderAndFooter;

	private volatile boolean traceOpen = false;

	public WiseMLWriterHandler(OutputStream out, String nodeUrn, boolean writeHeaderAndFooter) {
		super(out);
		this.writeHeaderAndFooter = writeHeaderAndFooter;
		this.nodeUrn = nodeUrn;
	}

	@Override
	public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {

		super.channelConnected(ctx, e);

		if (writeHeaderAndFooter) {
			writeHeader();
		}
	}

	@Override
	public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {

		closeTraceTagIfOpen();

		if (writeHeaderAndFooter) {
			writeFooter();
		}

		super.channelDisconnected(ctx, e);
	}

	@Override
	public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {

		openTraceTagIfNotOpenYet();

		ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
		final byte[] packet = new byte[buffer.readableBytes()];
		buffer.getBytes(0, packet);

		output.write("\t<timestamp>" + System.currentTimeMillis() + "</timestamp>\n");

		output.write("\t<node id=\"" + nodeUrn + "\">");
		output.newLine();

		output.write("\t\t<data>" + Base64.encodeBase64String(packet) + "</data>");
		output.newLine();

		output.write("\t</node>");
		output.newLine();
		output.flush();
	}

	private void writeHeader() throws IOException {
		output.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		output.newLine();
		output.write(
				"<wiseml xmlns=\"http://wisebed.eu/ns/wiseml/1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://wisebed.eu/ns/wiseml/1.0\" version=\"1.0\">"
		);
		output.newLine();
	}

	private void openTraceTagIfNotOpenYet() throws IOException {

		if (!traceOpen) {
			output.write("<trace id=\"" + timeFormatter.print(System.currentTimeMillis()) + "\">\n");
			traceOpen = true;
		}
	}

	private void closeTraceTagIfOpen() throws IOException {

		if (traceOpen) {
			output.write("</trace>\n");
			traceOpen = false;
		}
	}

	private void writeFooter() throws IOException {

		output.write("</wiseml>");
		output.newLine();
	}
}
