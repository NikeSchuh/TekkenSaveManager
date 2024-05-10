package com.nikeschuh.tekkensavemanager;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.SplitPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;
import com.nikeschuh.tekkensavemanager.save.BackupManager;
import com.nikeschuh.tekkensavemanager.save.JsonFile;
import com.nikeschuh.tekkensavemanager.utils.ByteUtils;
import com.nikeschuh.tekkensavemanager.utils.RenderTask;
import com.nikeschuh.tekkensavemanager.widgets.SaveInfoWidget;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class SaveManager extends ApplicationAdapter {

	private static Queue<RenderTask> renderTasks = new LinkedList<>();
	Stage stage;
	VerticalGroup leftSideContent;
	SplitPane splitPane;
	VisScrollPane scrollPane;
	VisTable scrollPaneContents;
	Viewport viewport;
	OrthographicCamera camera;

	JsonFile configFile;

	VisCheckBox backupGhosts;
	VisCheckBox backupReplays;

	FileHandle saveDirectory;
	FileHandle backupDirectory;

	ThreadPoolExecutor taskExecutor;
	boolean lock = false;

	@Override
	public void create () {
		configFile = new JsonFile(Gdx.files.external("Appdata/Local/Tekken8SaveManager/config.json"));

		taskExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
		FileHandle handle = Gdx.files.external("AppData/Local/TEKKEN 8/Saved/SaveGames");

		backupDirectory = Gdx.files.external("Appdata/Local/Tekken8SaveManager");

		if(!backupDirectory.exists()) backupDirectory.mkdirs();

		for(FileHandle fileHandle : handle.list()) {
			saveDirectory = fileHandle;
			break;
		}


		VisUI.load();
		Gdx.graphics.setContinuousRendering(false);


		camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		viewport = new FillViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), camera);

		stage = new Stage(viewport);
		scrollPaneContents = new VisTable();
		scrollPaneContents.padTop(10F);
		scrollPaneContents.padBottom(10F);

		loadBackupFiles();

		scrollPane = new VisScrollPane(scrollPaneContents);

		leftSideContent = new VerticalGroup();
		leftSideContent.align(Align.center).pad(5F);

		boolean backupReplaysB = configFile.getBoolean("backupReplays", false);
		boolean backupGhostsB = configFile.getBoolean("backupGhosts", false);

		this.backupGhosts = new VisCheckBox("Backup Ghosts", backupGhostsB);
		this.backupReplays = new VisCheckBox("Backup Replays", backupReplaysB);

		backupGhosts.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
			//	backupGhosts.setChecked(!backupGhosts.isChecked());
				configFile.setBoolean("backupGhosts", backupGhosts.isChecked());
				super.clicked(event, x, y);
			}
		});

		backupReplays.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
			//	backupReplays.setChecked(!backupReplays.isChecked());
				configFile.setBoolean("backupReplays", backupReplays.isChecked());
				super.clicked(event, x, y);
			}
		});

		leftSideContent.addActor(backupGhosts);
		leftSideContent.addActor(backupReplays);

		leftSideContent.addActor(new VisTextButton("Create Backup", new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
					if(lock) return;
				    taskExecutor.submit(()->{
						BackupManager.createBackup(saveDirectory, backupDirectory.child("T8Backup-" +getCurrentDateTimeFormatted() + ".t8backup"), (file) ->{
							queueRendering(()->addSaveWidget(file));
							}, backupReplays.isChecked(), backupGhosts.isChecked());;
						System.gc();
						lock = false;
					});
			}
		}));

		splitPane = new SplitPane(leftSideContent, scrollPane, false, VisUI.getSkin());
		splitPane.setSplitAmount(0.2F);
		splitPane.setMaxSplitAmount(0.5F);
		splitPane.setFillParent(true);


		stage.addActor(splitPane);
		Gdx.input.setInputProcessor(stage);
	}

	public void update(float deltaTime) {
		stage.act();
	}

	public void loadBackupFiles() {
		for(FileHandle fileHandle : backupDirectory.list("t8backup")){
			addSaveWidget(fileHandle);
		}
	}

	public void addSaveWidget(FileHandle fileHandle) {
		byte[] array = new byte[16];
		fileHandle.readBytes(array, 0, 16);
		long signature = ByteUtils.readLong(array, 0);

		if(signature != BackupManager.FORMAT_SIGNATURE) {
			return;
		}

		long timeStamp = ByteUtils.readLong(array, 8);

		SaveInfoWidget saveInfoWidget = null;
		VisTextButton loadButton = null;
		VisTextButton deleteButton = null;

		scrollPaneContents.add(saveInfoWidget = new SaveInfoWidget(fileHandle.nameWithoutExtension(), new Date(timeStamp), fileHandle.length(), VisUI.getSkin()));
		scrollPaneContents.add(loadButton = new VisTextButton("Load", new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if(lock) return;
				taskExecutor.submit(()->{
					lock = true;
					BackupManager.createBackup(saveDirectory, backupDirectory.child("T8Backup-" +getCurrentDateTimeFormatted() + "S.t8backup"), (file) ->{
						queueRendering(()->addSaveWidget(file));
					}, backupReplays.isChecked(), backupGhosts.isChecked());;
					BackupManager.readBackup(fileHandle, saveDirectory);
					lock = false;
					System.gc();
				});

			}
		})).padRight(10F).fillY().padLeft(10F).padBottom(5).padTop(5);
		scrollPaneContents.add(deleteButton = new VisTextButton("Delete", new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if(lock) return;
				lock = true;
				if(fileHandle.delete()) {
					scrollPaneContents.clear();
					loadBackupFiles();
				}
				lock = false;
			}
		})).padRight(10F).fillY().padLeft(10F).padBottom(5).padTop(5).row();
	}

	@Override
	public void resize(int width, int height) {
		viewport.setScreenSize(width, height);
		viewport.update(width, height);
		viewport.apply(true);
		super.resize(width, height);
	}

	@Override
	public void render () {
		update(Gdx.graphics.getDeltaTime());
		while (!renderTasks.isEmpty()) {
			renderTasks.poll().run();
		}
		ScreenUtils.clear(0.1F, 0.1F, 0.1F, 1);
		stage.draw();
	}
	
	@Override
	public void dispose () {
		stage.dispose();
		taskExecutor.shutdown();
	}

	private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd-HH.mm.ss");

	public static String getCurrentDateTimeFormatted() {
		LocalDateTime now = LocalDateTime.now();
		return now.format(formatter);
	}

	public static void queueRendering(RenderTask renderTask) {
		Gdx.graphics.requestRendering();
		renderTasks.add(renderTask);
	}
}
