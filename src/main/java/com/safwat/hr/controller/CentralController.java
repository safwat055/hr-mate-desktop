package com.safwat.hr.controller;

import java.util.HashMap;
import java.util.Map;

public class CentralController {

    private static CentralController instance;
    private final Map<String, Object> components = new HashMap<>();
    private final Map<String, Object> pages = new HashMap<>();

    // إضافة جديدة: خريطة للخرائط المتعددة للكومبوننتس، حيث يمكن تحديد اسم الخريطة (مثل اسم الواجهة أو الكنترولر)
    private final Map<String, Map<String, Object>> namedComponentMaps = new HashMap<>();

    public static CentralController getInstance() {

        if (instance == null) {
            instance = new CentralController();
        }
        return instance;

    }

    public void registerComponent(String name, Object component) {
        components.put(name, component);

    }

    public void registerViews(String name, Object component) {
        pages.put(name, component);

    }

    public Object getpages(String name) {
        return pages.get(name);

    }

    public Object getComponent(String name) {
        return components.get(name);

    }

    public <T> T getComponent(String name, Class<T> type) {

        return type.cast(components.get(name));
    }

    // إضافة جديدة: تسجيل كومبوننت في خريطة محددة باسمها (string)
    public void registerComponentInMap(String mapName, String name, Object component) {
        Map<String, Object> map = namedComponentMaps.computeIfAbsent(mapName, k -> new HashMap<>());
        map.put(name, component);
    }

    // إضافة جديدة: استدعاء كومبوننت من خريطة محددة باسمها
    public Object getComponentFromMap(String mapName, String name) {
        Map<String, Object> map = namedComponentMaps.get(mapName);
        return (map != null) ? map.get(name) : null;
    }

    // إضافة جديدة: استدعاء كومبوننت من خريطة محددة مع تحويل النوع
    public <T> T getComponentFromMap(String mapName, String name, Class<T> type) {
        Object obj = getComponentFromMap(mapName, name);
        return (obj != null) ? type.cast(obj) : null;
    }
}
