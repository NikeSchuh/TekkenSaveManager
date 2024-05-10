package com.nikeschuh.tekkensavemanager.widgets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SaveInfoWidget extends Actor {

    private String fileName;
    private Date date;
    private long fileSizeBytes;

    private Label nameLabel;
    private Label dateLabel;
    private Label sizeLabel;

    private Table table;

    public SaveInfoWidget(String fileName, Date date, long fileSizeBytes, Skin skin) {
        this.fileName = fileName;
        this.date = date;
        this.fileSizeBytes = fileSizeBytes;

        nameLabel = new Label(fileName, skin);
        dateLabel = new Label(formatDate(date), skin);
        sizeLabel = new Label(formatSize(fileSizeBytes), skin);

        table = new Table();
        table.add(nameLabel).expandX().fillY().align(Align.left).padLeft(10).padRight(100F);
        table.add(sizeLabel).expandX().fillY().align(Align.topRight).padRight(10);
        table.row();
        table.add(dateLabel).expandX().fillY().align(Align.bottomRight).colspan(2).padRight(10).padBottom(10);
        table.pack();

        setWidth(table.getWidth());
        setHeight(table.getHeight());
    }

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String formatDate(Date date) {
        return sdf.format(date);
    }

    private String formatSize(long fileSizeBytes) {
        if (fileSizeBytes < 1024) {
            return fileSizeBytes + " B";
        } else if (fileSizeBytes < 1024 * 1024) {
            return String.format("%.2f KB", fileSizeBytes / 1024.0);
        } else if (fileSizeBytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", fileSizeBytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", fileSizeBytes / (1024.0 * 1024 * 1024));
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        table.setPosition(getX(), getY());
        table.draw(batch, parentAlpha);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        table.act(delta);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
        nameLabel.setText(fileName);
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
        dateLabel.setText(formatDate(date));
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
        sizeLabel.setText(formatSize(fileSizeBytes));
    }
}