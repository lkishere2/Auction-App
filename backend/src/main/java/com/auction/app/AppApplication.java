package com.auction.app;

import org.springframework.boot.SpringApplication;
import javafx.application.Platform;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableCaching
public class AppApplication {

	public static void main(String[] args) {
		Thread springThread = new Thread(() -> {
			SpringApplication.run(AppApplication.class, args);
		}, "SpringBootThread");
		springThread.setDaemon(true);
		springThread.start();

		// Give Spring a moment to initialize before launching JavaFX
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		// Launch JavaFX on the main thread using Platform.startup()
		Platform.startup(() -> {
			new FxWindow().show();
		});
	}
}
