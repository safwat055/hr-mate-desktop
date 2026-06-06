package com.safwat.hr.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class NetworkScanner {

    // ========== الحصول على الـ IP الحقيقي (كما هو) ==========
    public static String getRealLocalIPAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                String name = iface.getDisplayName().toLowerCase();
                if (name.contains("virtual") || name.contains("vethernet") || name.contains("docker") || name.contains("hyper-v")) {
                    continue;
                }

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("192.168.") || ip.startsWith("10.")
                                || (ip.startsWith("172.") && isPrivate172(ip))) {
                            return ip;
                        }
                    }
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private static boolean isPrivate172(String ip) {
        try {
            String[] parts = ip.split("\\.");
            int second = Integer.parseInt(parts[1]);
            return second >= 16 && second <= 31;
        } catch (Exception e) {
            return false;
        }
    }

    // ========== دالة ping سريعة باستخدام Process ==========
    private static boolean fastPing(String ip, int timeoutMs) {
        try {
            // -n 1 : عدد مرات الإرسال 1
            // -w timeout : مهلة بالملي ثانية (في Windows)
            Process p = Runtime.getRuntime().exec("ping -n 1 -w " + timeoutMs + " " + ip);
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // ========== الحصول على اسم NetBIOS (محسّن) ==========
    private static String getNetBiosName(String ip) {
        try {
            Process p = Runtime.getRuntime().exec("nbtstat -A " + ip);
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine()) != null) {
                if (line.contains("<00>") && line.contains("UNIQUE")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) return parts[0];
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // ========== الحصول على الأجهزة من ARP مع تصفية أفضل ==========
    private static Map<String, String> getDevicesFromArp(String localSubnet) {
        Map<String, String> devices = new HashMap<>();
        try {
            Process p = Runtime.getRuntime().exec("arp -a");
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\s+");
                if (parts.length >= 2 && parts[0].matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                    String ip = parts[0];
                    // تجاهل عناوين البث والعناوين غير المحلية
                    if (ip.endsWith(".255") || ip.endsWith(".0") || ip.startsWith("224.") || ip.startsWith("239.")) {
                        continue;
                    }
                    // التأكد من أن IP ضمن الشبكة المحلية الحقيقية
                    if (!ip.startsWith(localSubnet)) continue;

                    String hostname = "ARP-" + ip;
                    // محاولة استخراج الاسم باستخدام nbtstat (إذا كان Windows)
                    String nbName = getNetBiosName(ip);
                    if (nbName != null) hostname = nbName;
                    devices.put(ip, hostname); // نستخدم IP كمفتاح مؤقت لمنع التكرار
                }
            }
        } catch (Exception ignored) {
        }
        return devices;
    }

    // ========== المسح الرئيسي ==========
    public static Map<String, String> getAllDevicesOnLocalNetwork() {
        Map<String, String> result = new HashMap<>(); // المفتاح: IP, القيمة: الاسم
        String localIP = getRealLocalIPAddress();
        if (localIP == null || localIP.equals("127.0.0.1")) {
            System.err.println("لا يمكن تحديد الشبكة المحلية.");
            return result;
        }

        String subnet = localIP.substring(0, localIP.lastIndexOf('.'));
        System.out.println("مسح الشبكة: " + subnet + ".0/24");

        // إضافة الجهاز الحالي
        try {
            String localHostname = InetAddress.getLocalHost().getHostName();
            result.put(localIP, localHostname);
        } catch (UnknownHostException e) {
            result.put(localIP, "ThisDevice");
        }

        // الحصول على الأجهزة من ARP أولاً (سريع)
        Map<String, String> arpDevices = getDevicesFromArp(subnet);
        result.putAll(arpDevices);

        // مسح النطاق بالـ ping (متعدد الخيوط)
        ExecutorService executor = Executors.newFixedThreadPool(50); // زيادة عدد الخيوط
        List<Future<Map.Entry<String, String>>> futures = new ArrayList<>();

        for (int i = 1; i <= 254; i++) {
            String targetIP = subnet + "." + i;
            // نتجنب إعادة فحص IP الموجود بالفعل في ARP (اختياري)
            if (result.containsKey(targetIP)) continue;

            futures.add(executor.submit(() -> {
                if (fastPing(targetIP, 200)) { // مهلة 200ms
                    String hostname = "Unknown-" + targetIP;
                    // محاولة الحصول على الاسم عبر DNS
                    try {
                        InetAddress inet = InetAddress.getByName(targetIP);
                        String dnsName = inet.getCanonicalHostName();
                        if (dnsName != null && !dnsName.equals(targetIP) && !dnsName.endsWith(".local")) {
                            hostname = dnsName;
                        } else {
                            // محاولة NetBIOS
                            String nbName = getNetBiosName(targetIP);
                            if (nbName != null) hostname = nbName;
                        }
                    } catch (Exception e) {
                    }
                    return new AbstractMap.SimpleEntry<>(targetIP, hostname);
                }
                return null;
            }));
        }

        executor.shutdown();
        try {
            executor.awaitTermination(2, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // جمع النتائج
        for (var future : futures) {
            try {
                var entry = future.get(1, TimeUnit.SECONDS);
                if (entry != null) {
                    result.putIfAbsent(entry.getKey(), entry.getValue());
                }
            } catch (Exception ignored) {
            }
        }

        // تحويل الخريطة من (IP -> اسم) إلى (اسم -> IP) مع حل النزاعات
        Map<String, String> finalMap = new HashMap<>();
        for (Map.Entry<String, String> e : result.entrySet()) {
            String ip = e.getKey();
            String name = e.getValue();
            // إذا كان الاسم موجوداً مسبقاً (بسبب ARP و ping)، نأخذ الاسم الأفضل
            finalMap.merge(name, ip, (oldVal, newVal) -> oldVal); // الأسبقية للأول
        }
        return finalMap;
    }

    // ========== دالة الحصول على IP من اسم جهاز (تبحث أولاً في الخريطة) ==========
    private static Map<String, String> cachedDevices = null;

    public static String getIPFromHostName(String hostName) {
        // إذا كان لدينا كاش، نبحث فيه أولاً
        if (cachedDevices == null) {
            cachedDevices = getAllDevicesOnLocalNetwork(); // يمكن أن تكون ثقيلة، لذا نستخدمها بحذر
        }
        for (Map.Entry<String, String> entry : cachedDevices.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(hostName)) {
                return entry.getValue();
            }
        }
        // إذا لم نجد، نحاول DNS
        try {
            InetAddress address = InetAddress.getByName(hostName);
            return address.getHostAddress();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * ترجع قائمة بجميع أسماء الأجهزة المكتشفة على الشبكة المحلية (شاملة الأسماء المؤقتة مثل ARP-... و Unknown-...)
     */
    public static List<String> getAllDeviceNames() {
        Map<String, String> devices = getAllDevicesOnLocalNetwork();
        return new ArrayList<>(devices.keySet());
    }

    /**
     * ترجع قائمة بأسماء الأجهزة الحقيقية فقط (تستثني الأسماء التي تبدأ بـ "ARP-" أو "Unknown-")
     */
    public static List<String> getRealDeviceNames() {
        Map<String, String> devices = getAllDevicesOnLocalNetwork();
        List<String> realNames = new ArrayList<>();
        for (String name : devices.keySet()) {
            if (!name.startsWith("ARP-") && !name.startsWith("Unknown-")) {
                realNames.add(name);
            }
        }
        return realNames;
    }

    // ========== اختبار ==========
    public static void main(String[] args) {
        System.out.println("الـ IP المحلي الحقيقي: " + getRealLocalIPAddress());
        long start = System.currentTimeMillis();
      /* Map<String, String> devices = getAllDevicesOnLocalNetwork();
        long end = System.currentTimeMillis();

        System.out.println("\nالوقت المستغرق: " + (end - start) + " مللي ثانية");
        System.out.println("عدد الأجهزة المكتشفة: " + devices.size());
        for (Map.Entry<String, String> entry : devices.entrySet()) {
            System.out.println("الجهاز: " + entry.getKey() + " -> " + entry.getValue());
        }

        String testName = "Safwat055";
        String ip = getIPFromHostName(testName);
        System.out.println("\nIP للاسم " + testName + " : " + ip);*/
        /*List<String> allNames = getAllDeviceNames();
        System.out.println("جميع الأسماء: " + allNames);
*/
        List<String> realNames = getRealDeviceNames();
        System.out.println("الأسماء الحقيقية: " + realNames);
        long end = System.currentTimeMillis();

        System.out.println("\nالوقت المستغرق: " + (end - start) + " مللي ثانية");
    }
}