package com.nikeschuh.tekkensavemanager;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.nikeschuh.tekkensavemanager.SaveManager;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setResizable(true);
		config.setTitle("Tekken 8 Save Manager");
		config.setWindowedMode(1080, 720);
		config.setWindowIcon(Files.FileType.Internal, "icon.png");
		new Lwjgl3Application(new SaveManager(), config);
	}
}
