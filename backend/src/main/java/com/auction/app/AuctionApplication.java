package com.auction.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableCaching
public class AuctionApplication extends Application {

	private ConfigurableApplicationContext context;

	@Override
	public void init() {
		this.context = new SpringApplicationBuilder()
				.sources(AuctionApplication.class)
				.run();
	}

	@Override
	public void start(Stage primaryStage) {
		this.context.publishEvent(new StageReadyEvent(primaryStage));
	}

	@Override
	public void stop() {
		this.context.close();
		Platform.exit();
	}

	public static void main(String[] args) {
		Application.launch(AuctionApplication.class, args);
	}
}