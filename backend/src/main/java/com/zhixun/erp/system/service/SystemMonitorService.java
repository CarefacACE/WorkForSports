package com.zhixun.erp.system.service;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class SystemMonitorService {

    private final SystemInfo systemInfo = new SystemInfo();
    private final HardwareAbstractionLayer hal = systemInfo.getHardware();
    private final OperatingSystem os = systemInfo.getOperatingSystem();

    private long[] prevCpuLoadTicks;

    private static final long startTime = System.currentTimeMillis();

    public SystemMonitorService() {
        prevCpuLoadTicks = hal.getProcessor().getSystemCpuLoadTicks();
    }

    public Map<String, Object> getSystemInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("os", getOsInfo());
        info.put("cpu", getCpuInfo());
        info.put("memory", getMemoryInfo());
        info.put("disk", getDiskInfo());
        info.put("network", getNetworkInfo());
        info.put("jvm", getJvmInfo());
        info.put("app", getAppInfo());
        info.put("timestamp", System.currentTimeMillis());
        return info;
    }

    private Map<String, Object> getOsInfo() {
        Map<String, Object> osInfo = new LinkedHashMap<>();
        osInfo.put("name", os.getFamily() + " " + os.toString());
        osInfo.put("version", System.getProperty("os.version"));
        osInfo.put("arch", System.getProperty("os.arch"));
        osInfo.put("uptime", formatUptime(os.getSystemUptime()));
        return osInfo;
    }

    private Map<String, Object> getCpuInfo() {
        Map<String, Object> cpuInfo = new LinkedHashMap<>();
        CentralProcessor processor = hal.getProcessor();

        long[] loadTicks = processor.getSystemCpuLoadTicks();
        long idle = loadTicks[CentralProcessor.TickType.IDLE.getIndex()] - prevCpuLoadTicks[CentralProcessor.TickType.IDLE.getIndex()];
        long total = 0;
        for (int i = 0; i < loadTicks.length; i++) {
            total += loadTicks[i] - prevCpuLoadTicks[i];
        }
        double cpuLoad = (total > 0) ? (double) (total - idle) / total * 100.0 : 0.0;
        prevCpuLoadTicks = loadTicks;

        cpuInfo.put("name", processor.getProcessorIdentifier().getName());
        cpuInfo.put("cores", processor.getPhysicalProcessorCount());
        cpuInfo.put("logicalCores", processor.getLogicalProcessorCount());
        cpuInfo.put("load", Math.round(cpuLoad * 100.0) / 100.0);
        cpuInfo.put("frequency", formatFrequency(processor.getProcessorIdentifier().getVendorFreq()));
        return cpuInfo;
    }

    private Map<String, Object> getMemoryInfo() {
        Map<String, Object> memInfo = new LinkedHashMap<>();
        GlobalMemory memory = hal.getMemory();

        long total = memory.getTotal();
        long available = memory.getAvailable();
        long used = total - available;
        double usage = (double) used / total * 100.0;

        memInfo.put("total", formatSize(total));
        memInfo.put("used", formatSize(used));
        memInfo.put("available", formatSize(available));
        memInfo.put("usage", Math.round(usage * 100.0) / 100.0);

        if (memory.getVirtualMemory() != null) {
            long swapTotal = memory.getVirtualMemory().getVirtualMax();
            long swapUsed = memory.getVirtualMemory().getVirtualInUse();
            memInfo.put("swapTotal", formatSize(swapTotal));
            memInfo.put("swapUsed", formatSize(swapUsed));
        }
        return memInfo;
    }

    private List<Map<String, Object>> getDiskInfo() {
        List<Map<String, Object>> diskList = new ArrayList<>();
        FileSystem fileSystem = os.getFileSystem();

        for (OSFileStore fs : fileSystem.getFileStores()) {
            long total = fs.getTotalSpace();
            if (total <= 0) continue;

            Map<String, Object> disk = new LinkedHashMap<>();
            long free = fs.getFreeSpace();
            long used = total - free;
            double usage = (double) used / total * 100.0;

            String label = fs.getLabel();
            String mount = fs.getMount();
            String name;
            if (label != null && !label.isEmpty()) {
                name = label + " (" + mount + ")";
            } else {
                name = mount;
            }

            disk.put("name", name);
            disk.put("mount", mount);
            disk.put("type", fs.getType());
            disk.put("total", formatSize(total));
            disk.put("used", formatSize(used));
            disk.put("free", formatSize(free));
            disk.put("usage", Math.round(usage * 100.0) / 100.0);
            diskList.add(disk);
        }
        return diskList;
    }

    private List<Map<String, Object>> getNetworkInfo() {
        List<Map<String, Object>> networkList = new ArrayList<>();
        hal.getNetworkIFs().forEach(nif -> {
            Map<String, Object> network = new LinkedHashMap<>();
            network.put("name", nif.getName());
            network.put("displayName", nif.getDisplayName());
            network.put("mac", nif.getMacaddr());
            network.put("ipv4", Arrays.toString(nif.getIPv4addr()));
            network.put("ipv6", Arrays.toString(nif.getIPv6addr()));
            network.put("speed", nif.getSpeed() + " bps");
            networkList.add(network);
        });
        return networkList;
    }

    private Map<String, Object> getJvmInfo() {
        Map<String, Object> jvmInfo = new LinkedHashMap<>();
        Runtime runtime = Runtime.getRuntime();

        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double usage = (double) usedMemory / totalMemory * 100.0;

        jvmInfo.put("name", System.getProperty("java.vm.name"));
        jvmInfo.put("version", System.getProperty("java.version"));
        jvmInfo.put("home", System.getProperty("java.home"));
        jvmInfo.put("totalMemory", formatSize(totalMemory));
        jvmInfo.put("freeMemory", formatSize(freeMemory));
        jvmInfo.put("usedMemory", formatSize(usedMemory));
        jvmInfo.put("usage", Math.round(usage * 100.0) / 100.0);
        jvmInfo.put("maxMemory", formatSize(runtime.maxMemory()));
        jvmInfo.put("availableProcessors", runtime.availableProcessors());
        return jvmInfo;
    }

    private Map<String, Object> getAppInfo() {
        Map<String, Object> appInfo = new LinkedHashMap<>();

        long startupTime = ManagementFactory.getRuntimeMXBean().getStartTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String startupTimeStr = sdf.format(new Date(startupTime));

        long runTimeMillis = System.currentTimeMillis() - startTime;
        long runTimeSeconds = runTimeMillis / 1000;
        String runTime = formatDuration(runTimeSeconds);

        appInfo.put("name", "智训业财云");
        appInfo.put("version", "1.0.0");
        appInfo.put("startupTime", startupTimeStr);
        appInfo.put("runTime", runTime);
        appInfo.put("runTimeMillis", runTimeMillis);

        return appInfo;
    }

    private String formatDuration(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("天 ");
        if (hours > 0) sb.append(hours).append("小时 ");
        if (minutes > 0) sb.append(minutes).append("分钟 ");
        sb.append(secs).append("秒");

        return sb.toString();
    }

    private String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        digitGroups = Math.min(digitGroups, units.length - 1);
        return String.format("%.2f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private String formatFrequency(long hz) {
        if (hz <= 0) return "N/A";
        if (hz >= 1000000000) {
            return String.format("%.2f GHz", hz / 1000000000.0);
        } else if (hz >= 1000000) {
            return String.format("%.2f MHz", hz / 1000000.0);
        } else {
            return hz + " Hz";
        }
    }

    private String formatUptime(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        return days + "天 " + hours + "小时 " + minutes + "分钟";
    }
}
