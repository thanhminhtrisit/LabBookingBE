package com.prm.labbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.*;

@SpringBootApplication
public class LabbookingApplication {

	public static void main(String[] args) {
		loadEnv();
		SpringApplication.run(LabbookingApplication.class, args);
	}

	private static void loadEnv() {
		File envFile = new File(".env");
		if (!envFile.exists()) return;
		try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) continue;
				int idx = line.indexOf('=');
				if (idx < 0) continue;
				String key = line.substring(0, idx).trim();
				String value = line.substring(idx + 1).trim();
				if (System.getenv(key) == null) System.setProperty(key, value);
			}
		} catch (Exception e) {
			System.err.println("[.env] Could not load: " + e.getMessage());
		}
	}

}
