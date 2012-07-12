import com.google.common.collect.ImmutableMap;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.wsn.deviceutils.DeviceUtilsModule;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceInfo;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceObserver;
import de.uniluebeck.itm.wsn.drivers.core.Device;
import de.uniluebeck.itm.wsn.drivers.core.MacAddress;
import de.uniluebeck.itm.wsn.drivers.core.operation.OperationAdapter;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactory;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactoryModule;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceType;
import org.apache.log4j.Level;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.iostream.IOStreamAddress;
import org.jboss.netty.channel.iostream.IOStreamChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class WsnDeviceUtilsGui {

	static {
		Logging.setLoggingDefaults(Level.TRACE);
	}

	private static final Logger log = LoggerFactory.getLogger(WsnDeviceUtilsGui.class);

	private DeviceObserver deviceObserver;

	private DevicePane devicePane = new DevicePane();

	private JFrame frame;

	private Device device;

	private DeviceFactory deviceFactory = Guice
			.createInjector(new DeviceFactoryModule())
			.getInstance(DeviceFactory.class);

	private final ExecutorService executorService;

	private final DeviceConfigurationDialog deviceConfigurationDialog = new DeviceConfigurationDialog();

	private Map<String, String> deviceConfiguration = null;

	public WsnDeviceUtilsGui(final ExecutorService executorService) {

		this.executorService = executorService;

		final Injector deviceUtilsInjector = Guice.createInjector(new DeviceUtilsModule(executorService, null));
		deviceObserver = deviceUtilsInjector.getInstance(DeviceObserver.class);
	}

	private void createAndShowGUI() {

		frame = new JFrame("WSN Device Utils GUI");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.getContentPane().add(devicePane.contentPane);

		disconnect();

		deviceObserver.updateState();
		ImmutableMap<String, DeviceInfo> currentState = deviceObserver.getCurrentState();
		Object[] devicePorts = currentState.keySet().toArray();
		Object[] items = new String[devicePorts.length + 1];
		items[0] = "Mock Device";
		System.arraycopy(devicePorts, 0, items, 1, devicePorts.length);
		devicePane.selectionComboBox.setModel(new DefaultComboBoxModel(items));

		if (items.length > 0) {
			devicePane.selectionComboBox.setSelectedIndex(0);
		}

		devicePane.editConfigurationButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				deviceConfigurationDialog.pack();
				deviceConfigurationDialog.setVisible(true);
				deviceConfiguration = deviceConfigurationDialog.getConfiguration();
			}
		}
		);

		devicePane.connectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {

				String devicePort = (String) devicePane.selectionComboBox.getSelectedItem();

				if ("Mock Device".equals(devicePort)) {
					connect(DeviceType.MOCK.toString(), devicePort, deviceConfiguration);
				} else {
					connect(getDeviceType(devicePort), devicePort, deviceConfiguration);
				}
			}
		}
		);

		devicePane.disconnectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				disconnect();
			}
		}
		);

		devicePane.clearOutputButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				devicePane.outputTextArea.setText(null);
			}
		}
		);

		devicePane.wrapLinesCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				devicePane.outputTextArea.setLineWrap(devicePane.wrapLinesCheckBox.isSelected());
			}
		}
		);

		devicePane.programButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {

				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				int returnCode = fileChooser.showOpenDialog(frame);

				if (returnCode == JFileChooser.APPROVE_OPTION) {

					byte[] selectedFileBytes;
					try {

						File selectedFile = fileChooser.getSelectedFile();
						selectedFileBytes = Files.toByteArray(selectedFile);

					} catch (IOException e1) {

						log.warn("Error while reading file: {}", e1.getMessage(), e1);
						JOptionPane.showMessageDialog(frame, "Error while reading file: " + e1.getMessage());
						return;
					}

					device.program(selectedFileBytes, 120000, new DevicePaneOperationListener<Void>(devicePane));
				}
			}
		}
		);

		devicePane.resetButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				device.reset(1000, new DevicePaneOperationListener<Void>(devicePane));
			}
		}
		);

		devicePane.readMACButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				DevicePaneOperationListener<MacAddress> callback =
						new DevicePaneOperationListener<MacAddress>(devicePane) {
							@Override
							public void onSuccess(final MacAddress result) {
								super.onSuccess(result);
								JOptionPane.showMessageDialog(frame, "MAC address: " + result.toHexString());
							}
						};
				device.readMac(5000, callback);
			}
		}
		);

		devicePane.writeMACButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {

				devicePane.progressBar.setValue(0);
				devicePane.progressBar.setEnabled(true);

				String macAddressString = JOptionPane.showInputDialog("Please enter MAC address to write:");
				MacAddress macAddress = new MacAddress(macAddressString);

				device.writeMac(macAddress, 120000, new DevicePaneOperationListener<Void>(devicePane));
			}
		}
		);

		devicePane.sendButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				try {

					byte[] messageBytes;

					final String inputMode = (String) devicePane.inputMode.getSelectedItem();

					if ("ASCII".equals(inputMode)) {
						messageBytes = devicePane.sendTextField.getText().getBytes();
					} else if ("Hex-String".equals(inputMode)) {
						messageBytes = StringUtils.fromStringToByteArray(devicePane.sendTextField.getText());
					} else {
						throw new RuntimeException("Unknown input mode \"" + inputMode + "\"");
					}

					synchronized (device.getOutputStream()) {
						device.getOutputStream().write(messageBytes);
						device.getOutputStream().flush();
					}

				} catch (Exception e1) {
					log.warn("Error while parsing message: {}", e1.getMessage(), e1);
					JOptionPane.showMessageDialog(frame, "Error while parsing message: " + e1.getMessage());
				}
			}
		}
		);

		frame.pack();
		frame.setVisible(true);
	}

	private class DevicePaneOperationListener<T> extends OperationAdapter<T> {

		private final DevicePane devicePane;

		private DevicePaneOperationListener(final DevicePane devicePane) {
			this.devicePane = devicePane;
		}

		@Override
		public void onExecute() {

			devicePane.progressBar.setValue(0);
			devicePane.progressBar.setEnabled(true);

			devicePane.setDeviceControlsEnabled(false);
		}

		@Override
		public void onSuccess(final T result) {

			devicePane.progressBar.setValue(100);
			devicePane.progressBar.setEnabled(false);

			devicePane.setDeviceControlsEnabled(true);
		}

		@Override
		public void onCancel() {

			devicePane.progressBar.setValue(0);
			devicePane.progressBar.setEnabled(false);

			devicePane.setDeviceControlsEnabled(true);
		}

		@Override
		public void onFailure(final Throwable throwable) {

			devicePane.progressBar.setValue(0);
			devicePane.progressBar.setEnabled(false);

			devicePane.setDeviceControlsEnabled(true);

			JOptionPane.showMessageDialog(frame, "Failed executing operation. Reason: " + throwable);
		}

		@Override
		public void onProgressChange(final float fraction) {
			devicePane.progressBar.setValue((int) (fraction * 100));
		}
	}

	private void connect(final String deviceType, final String devicePort, final Map<String, String> configuration) {

		disconnect();

		device = deviceFactory.create(executorService, deviceType, configuration);

		try {
			device.connect(devicePort);
		} catch (IOException e) {
			log.warn("{}", e.getMessage(), e);
			JOptionPane.showMessageDialog(frame, e.getMessage());
			devicePane.setDeviceControlsEnabled(false);
			return;
		}

		if (!device.isConnected()) {
			JOptionPane.showMessageDialog(frame, "Could not connect to device (unknown error)");
			devicePane.setDeviceControlsEnabled(false);
			return;
		}

		final InputStream inputStream = device.getInputStream();
		final OutputStream outputStream = device.getOutputStream();

		final ClientBootstrap bootstrap = new ClientBootstrap(new IOStreamChannelFactory(executorService));

		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {
				DefaultChannelPipeline pipeline = new DefaultChannelPipeline();
				pipeline.addLast("loggingHandler", new SimpleChannelHandler() {
					@Override
					public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e)
							throws Exception {

						final ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
						byte[] messageBytes = new byte[buffer.readableBytes()];
						buffer.readBytes(messageBytes);

						final String outputMode = (String) devicePane.outputMode.getSelectedItem();
						final String message;

						if ("ASCII".equals(outputMode)) {
							message = new String(messageBytes);
						} else if ("Hex-String".equals(outputMode)) {
							message = StringUtils.toHexString(messageBytes);
						} else if ("Replace non-printable ASCII characters".equals(outputMode)) {
							message = StringUtils.toPrintableString(messageBytes);
						} else {
							throw new RuntimeException();
						}

						log.debug("Device output: {}", message);
						devicePane.outputTextArea.append(message + '\n');
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

		log.debug("Connected to {} device at port {}", deviceType, devicePort);

		devicePane.selectionComboBox.setEnabled(false);
		devicePane.connectButton.setEnabled(false);
		devicePane.disconnectButton.setEnabled(true);
		devicePane.editConfigurationButton.setEnabled(false);

		devicePane.setDeviceControlsEnabled(true);
		devicePane.setStatusText("Connected to " + deviceType + " device at port " + devicePort);
	}

	private String getDeviceType(final String devicePort) {
		return deviceObserver.getCurrentState().get(devicePort).getType();
	}

	private void disconnect() {
		if (device != null && device.isConnected()) {
			Closeables.closeQuietly(device);
		}
		devicePane.setDeviceControlsEnabled(false);
		devicePane.outputTextArea.setText(null);
		devicePane.setStatusText("Not connected");

		devicePane.selectionComboBox.setEnabled(true);
		devicePane.connectButton.setEnabled(true);
		devicePane.disconnectButton.setEnabled(false);
		devicePane.editConfigurationButton.setEnabled(true);
	}

	public static void main(String[] args) {

		final ThreadFactory threadFactory =
				new ThreadFactoryBuilder().setNameFormat("WsnDeviceUtilsGui-Thread %d").build();
		final ExecutorService executorService = Executors.newCachedThreadPool(threadFactory);

		Runtime.getRuntime().addShutdownHook(new Thread("WsnDeviceUtilsGui-ShutdownHook") {
			@Override
			public void run() {
				log.info("Received shutdown signal. Exiting...");
				ExecutorUtils.shutdown(executorService, 1, TimeUnit.SECONDS);
			}
		}
		);

		final WsnDeviceUtilsGui gui = new WsnDeviceUtilsGui(executorService);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.createAndShowGUI();
			}
		}
		);

	}
}
