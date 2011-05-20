package de.uniluebeck.itm.wsn.deviceutils.writers;


public interface Writer {

	public void write(byte[] packet);

	public void shutdown();

}