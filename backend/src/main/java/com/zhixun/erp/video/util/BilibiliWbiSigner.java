package com.zhixun.erp.video.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

/**
 * B站 WBI 签名工具
 * 参考: https://github.com/SocialSisterYi/bilibili-API-collect/blob/master/docs/misc/sign/wbi.md
 */
public class BilibiliWbiSigner {

    private static final Logger log = LoggerFactory.getLogger(BilibiliWbiSigner.class);
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 混肴密钥的置换表
    private static final int[] MIXIN_KEY_ENC_TAB = {
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
            27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
            37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
            22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52
    };

    private String imgKey = "";
    private String subKey = "";
    private String mixedKey = "";
    private long lastRefresh = 0;
    private static final long REFRESH_INTERVAL = 30 * 60 * 1000; // 30分钟刷新一次

    /**
     * 刷新 WBI 密钥（从 B站 nav 接口获取）
     */
    public synchronized void refreshKeys() {
        if (System.currentTimeMillis() - lastRefresh < REFRESH_INTERVAL && !mixedKey.isEmpty()) {
            return;
        }
        try {
            HttpHeaders headers = buildCommonHeaders();
            ResponseEntity<String> resp = restTemplate.exchange(
                    "https://api.bilibili.com/x/web-interface/nav",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            String body = resp.getBody();
            if (body == null) return;

            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.get("data");
            if (data == null) return;

            JsonNode wbiImg = data.get("wbi_img");
            if (wbiImg == null) return;

            String imgUrl = wbiImg.has("img_url") ? wbiImg.get("img_url").asText() : "";
            String subUrl = wbiImg.has("sub_url") ? wbiImg.get("sub_url").asText() : "";

            this.imgKey = extractKey(imgUrl);
            this.subKey = extractKey(subUrl);
            this.mixedKey = mixKeys(this.imgKey, this.subKey);
            this.lastRefresh = System.currentTimeMillis();

            log.info("WBI 密钥已刷新: mixKey前8位={}", mixedKey.substring(0, 8));
        } catch (Exception e) {
            log.warn("刷新 WBI 密钥失败: {}", e.getMessage());
        }
    }

    /**
     * 对请求参数进行 WBI 签名，返回完整的 query string
     */
    public String signParams(Map<String, String> params) {
        if (mixedKey.isEmpty()) {
            refreshKeys();
        }

        // 添加当前时间戳
        long wts = System.currentTimeMillis() / 1000;
        params.put("wts", String.valueOf(wts));

        // 按 key 排序
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(e.getKey()).append("=").append(e.getValue());
        }

        // 计算 w_rid = MD5(queryString + mixedKey)
        String wRid = md5(sb.toString() + mixedKey);
        sb.append("&w_rid=").append(wRid);

        return sb.toString();
    }

    /**
     * 构建通用请求头（模拟浏览器）
     */
    public static HttpHeaders buildCommonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
        headers.set("Referer", "https://www.bilibili.com/");
        headers.set("Accept", "application/json, text/plain, */*");
        headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        headers.set("Origin", "https://www.bilibili.com");
        return headers;
    }

    // ────────── 内部方法 ──────────

    private String extractKey(String url) {
        // URL 格式: https://i0.hdslb.com/bfs/wbi/xxx.png
        if (url == null || url.isEmpty()) return "";
        String name = url.substring(url.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private String mixKeys(String imgKey, String subKey) {
        String combined = imgKey + subKey;
        char[] result = new char[32];
        for (int i = 0; i < 32; i++) {
            result[i] = combined.charAt(MIXIN_KEY_ENC_TAB[i]);
        }
        return new String(result);
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 计算失败", e);
        }
    }
}
