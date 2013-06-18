package de.uniluebeck.itm.wsn.deviceutils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import de.uniluebeck.itm.util.concurrent.ForwardingScheduledExecutorService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ScheduledExecutorServiceModule implements Module {

	private final String threadName;
	
	public ScheduledExecutorServiceModule(String threadName) {
		this.threadName = threadName;
	}
	
	@Override
	public void configure(Binder binder) {
		
	}
	
	@Provides
	public ScheduledExecutorService provideScheduledExecutorService() {
		final ScheduledExecutorService scheduleService = Executors.newScheduledThreadPool(1,
				new ThreadFactoryBuilder().setNameFormat(threadName + "Scheduler-Thread %d").build()
		);
		final ExecutorService executorService = Executors.newCachedThreadPool(
				new ThreadFactoryBuilder().setNameFormat(threadName + "-Thread %d").build()
		);
		return new ForwardingScheduledExecutorService(scheduleService, executorService);
	}

}
