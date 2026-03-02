package me.bytebeats.mns.tool;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class GsonUtils {
    private static final Gson GSON = new Gson();

    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return GSON.fromJson(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    public static <T> T fromJson(String json, Type typeOfT) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return GSON.fromJson(json, typeOfT);
        } catch (Exception e) {
            return null;
        }
    }

    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        return GSON.toJson(obj);
    }
}