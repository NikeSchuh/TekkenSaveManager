package com.nikeschuh.tekkensavemanager.save;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;

public class JsonFile {

    private FileHandle handle;
    private Json json;
    private ObjectMap<String, Object> dataMap;

    public JsonFile(FileHandle handle) {
        this.handle = handle;
        this.json = new Json();
        this.dataMap = new ObjectMap<>();
        loadData();
    }

    private void loadData() {
        if (handle.exists()) {
            String jsonData = handle.readString();
            dataMap = json.fromJson(ObjectMap.class, jsonData);
        }
    }

    private void saveData() {
        String jsonData = json.prettyPrint(dataMap);
        handle.writeString(jsonData, false);
    }

    public String getString(String key, String defaultValue) {
        if(dataMap.containsKey(key)) {
            return (String) dataMap.get(key);
        }
        setString(key, defaultValue);
        return defaultValue;
    }

    public void setString(String key, String value) {
        dataMap.put(key, value);
        saveData();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        if(dataMap.containsKey(key)) {
            return (boolean) dataMap.get(key);
        }
        setBoolean(key, defaultValue);
        return defaultValue;
    }

    public void setBoolean(String key, boolean value) {
        dataMap.put(key, value);
        saveData();
    }

    // Similar methods for other types: double, int, float, lists, etc.
}