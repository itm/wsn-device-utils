package de.uniluebeck.itm.wsn.deviceutils.listener;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import javax.annotation.Nonnull;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class WriterHandler extends SimpleChannelHandler {

	protected BufferedWriter output;

	private final OutputStream out;

	public WriterHandler(@Nonnull final OutputStream out) {
		checkNotNull(out);
		this.out = out;
	}

	@Override
	public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
		this.output = new BufferedWriter(new OutputStreamWriter(out));
	}

	@Override
	public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
		try {
			output.flush();
		} finally {
			output.close();
		}
	}

	protected byte[] getBufferBytes(final MessageEvent e) {
		ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
		byte[] packet = new byte[buffer.readableBytes()];
		buffer.getBytes(0, packet);
		return packet;
	}
}
