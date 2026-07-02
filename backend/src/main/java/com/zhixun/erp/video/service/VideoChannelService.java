package com.zhixun.erp.video.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.erp.video.entity.VideoChannel;
import com.zhixun.erp.video.mapper.VideoChannelMapper;
import com.zhixun.erp.video.util.BilibiliWbiSigner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class VideoChannelService {

    private static final Logger log = LoggerFactory.getLogger(VideoChannelService.class);
    private final VideoChannelMapper videoChannelMapper;
    private final BilibiliWbiSigner wbiSigner = new BilibiliWbiSigner();
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VideoChannelService(VideoChannelMapper videoChannelMapper) {
        this.videoChannelMapper = videoChannelMapper;
    }

    private static final String[] KEYWORDS = {"健身教学", "减脂训练", "增肌训练", "瑜伽教程", "HIIT训练", "有氧运动", "力量训练", "拳击教学", "舞蹈健身"};

    private static final String[][] SEED_VIDEOS = {
        // 瑜伽
        {"BV1YV411C7Nr", "30分钟流瑜伽全身拉伸", "30分钟温和流瑜伽，适合所有水平，提升柔韧性与身心放松", "BILIBILI", "瑜伽", "3012345", "30:25", "YogawithAdriene"},
        {"BV1ss4y1X7Vt", "帕梅拉15分钟瑜伽拉伸", "帕梅拉出品，15分钟瑜伽拉伸全身放松解放僵硬肌肉", "BILIBILI", "瑜伽", "4567890", "15:10", "帕梅拉PamelaReif"},
        // HIIT
        {"BV1Dh411m7tA", "帕梅拉20分钟HIIT训练", "帕梅拉经典20分钟HIIT，高效燃脂无需器械", "BILIBILI", "HIIT", "8123456", "20:00", "帕梅拉PamelaReif"},
        {"BV19T411z7F2", "10分钟超燃脂HIIT", "短时高效HIIT训练，适合忙碌人群，零基础友好", "BILIBILI", "HIIT", "2345678", "10:30", "周六野Zoey"},
        // 减脂
        {"BV1sP4y1B7hJ", "刘畊宏本草纲目暴汗燃脂操", "刘畊宏暴汗燃脂本草纲目完整跟练，全民健身热门", "BILIBILI", "减脂", "20123456", "35:00", "刘畊宏"},
        {"BV1UB4y1X7hQ", "30分钟站立燃脂训练", "无跳跃无深蹲，保护膝盖的有氧燃脂训练", "BILIBILI", "减脂", "1890123", "30:00", "周六野Zoey"},
        // 增肌
        {"BV1z4411i7KK", "俯卧撑入门到高手训练计划", "从零开始俯卧撑训练，居家也能练出胸肌线条", "BILIBILI", "增肌", "5123456", "18:00", "Jeff健身"},
        {"BV1nG411p7AM", "哑铃全身增肌训练", "一副哑铃练全身，适合家庭增肌塑形", "BILIBILI", "增肌", "3890123", "45:00", "JeremyEthier"},
        // 力量
        {"BV1Hv4y1C7Fx", "深蹲标准动作详解", "深蹲动作分解教学，纠正常见错误，提升下肢力量", "BILIBILI", "力量", "1678901", "12:00", "健身博士"},
        {"BV1kB4y1r7XM", "硬拉教程从入门到精通", "硬拉完整教学，掌握正确发力模式避免受伤", "BILIBILI", "力量", "2789012", "15:00", "MarkRippetoe"},
        // 有氧
        {"BV1Nx4y1G7Pc", "40分钟高效有氧操", "40分钟快乐有氧操，心肺功能提升效果极佳", "BILIBILI", "有氧", "1987654", "40:00", "小马哥运动"},
        {"BV1De4y1m7oL", "跳绳减脂教程完整版", "跳绳从基础到进阶教学，有氧燃脂的最佳选择", "BILIBILI", "有氧", "1567890", "22:00", "跳绳达人"},
        // 拳击
        {"BV1uN4y1w7Pv", "拳击基础入门教学", "零基础拳击教学，学会直拳摆拳勾拳基本动作", "BILIBILI", "拳击", "3456789", "25:00", "拳击训练营"},
        {"BV1Ad4y1Y7gL", "拳击燃脂训练跟练", "拳击有氧组合训练，燃脂暴汗塑形减压", "BILIBILI", "拳击", "1567890", "28:00", "拳击教练Mike"},
        // 舞蹈
        {"BV1EG411E7iZ", "Kpop女团舞蹈教学", "热门Kpop舞蹈教学跟练，零基础也能学会", "BILIBILI", "舞蹈", "4567890", "18:00", "舞蹈教室"},
        {"BV1m54y1V7Wt", "Zumba尊巴燃脂舞", "30分钟Zumba快乐燃脂舞，舞蹈与健身的完美结合", "BILIBILI", "舞蹈", "2890123", "30:00", "ZumbaFitness"},
        // 综合健身
        {"BV1DK4y1W7Ei", "家庭徒手健身计划", "无需器械的居家健身计划，从热身到拉伸完整跟练", "BILIBILI", "综合", "7234567", "35:00", "健身博主Alan"},
        {"BV1uY411W7hx", "5分钟早晨唤醒运动", "每日5分钟晨间唤醒，激活身体提升一天状态", "BILIBILI", "综合", "15678901", "5:00", "运动康复师"},
    };

    @Transactional
    public int seedVideos() {
        int count = 0;
        for (String[] s : SEED_VIDEOS) {
            String sourceUrl = "https://www.bilibili.com/video/" + s[0];
            Long exist = videoChannelMapper.selectCount(
                    new LambdaQueryWrapper<VideoChannel>().eq(VideoChannel::getSourceUrl, sourceUrl));
            if (exist > 0) continue;

            VideoChannel v = new VideoChannel();
            v.setTitle(s[1]);
            v.setDescription(s[2]);
            v.setPlatform(s[3]);
            v.setSourceUrl(sourceUrl);
            v.setEmbedUrl("//player.bilibili.com/player.html?bvid=" + s[0] + "&page=1&high_quality=1");
            v.setThumbnailUrl("");
            v.setPlayCount(Long.parseLong(s[5]));
            v.setDuration(s[6]);
            v.setAuthor(s[7]);
            v.setTags(s[4]);
            v.setCategory(s[4]);
            v.setCreateTime(LocalDateTime.now());
            videoChannelMapper.insert(v);
            count++;
        }
        log.info("种子数据注入完成，新增 {} 条视频", count);
        return count;
    }

    @Transactional
    public int crawlFromBilibili(String keyword) {
        int newCount = 0;
        try {
            wbiSigner.refreshKeys();

            Map<String, String> params = new java.util.HashMap<>();
            params.put("search_type", "video");
            params.put("keyword", keyword);
            params.put("page", "1");
            params.put("page_size", "10");

            String signedQuery = wbiSigner.signParams(params);
            String url = "https://api.bilibili.com/x/web-interface/wbi/search/type?" + signedQuery;

            HttpHeaders headers = BilibiliWbiSigner.buildCommonHeaders();
            headers.set("Cookie", "buvid3=auto-generated; buvid4=auto-generated");

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);
            String body = resp.getBody();
            if (body == null) return 0;

            JsonNode root = objectMapper.readTree(body);
            if (root.get("code").asInt() != 0) {
                log.warn("B站 API 返回非0: code={}, message={}", root.get("code").asInt(), root.get("message").asText());
                return 0;
            }

            JsonNode results = root.get("data").get("result");
            if (results == null || !results.isArray()) return 0;

            for (JsonNode item : results) {
                String bvid = item.has("bvid") ? item.get("bvid").asText() : "";
                if (bvid.isEmpty()) continue;

                String sourceUrl = "https://www.bilibili.com/video/" + bvid;
                Long exist = videoChannelMapper.selectCount(
                        new LambdaQueryWrapper<VideoChannel>().eq(VideoChannel::getSourceUrl, sourceUrl));
                if (exist > 0) continue;

                VideoChannel v = new VideoChannel();
                v.setTitle(item.has("title") ? removeHtml(item.get("title").asText()) : "");
                v.setDescription(item.has("description") ? removeHtml(item.get("description").asText()) : "");
                v.setPlatform("BILIBILI");
                v.setSourceUrl(sourceUrl);
                v.setEmbedUrl("//player.bilibili.com/player.html?bvid=" + bvid + "&page=1&high_quality=1");
                v.setThumbnailUrl(item.has("pic") ? "https:" + item.get("pic").asText() : "");
                v.setPlayCount(item.has("play") ? item.get("play").asLong() : 0);
                v.setDuration(item.has("duration") ? item.get("duration").asText() : "");
                v.setAuthor(item.has("author") ? item.get("author").asText() : "");
                v.setTags(keyword);
                v.setCategory(mapCategory(keyword));
                v.setCreateTime(LocalDateTime.now());
                videoChannelMapper.insert(v);
                newCount++;
            }
            log.info("爬取关键词[{}]完成，新增 {} 条视频", keyword, newCount);
        } catch (Exception e) {
            log.error("B站爬取失败[{}]: {}", keyword, e.getMessage());
        }
        return newCount;
    }

    @PostConstruct
    public void init() {
        seedVideos();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startCrawl() {
        // 异步爬取，避免阻塞启动
        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            for (String kw : KEYWORDS) {
                try {
                    crawlFromBilibili(kw);
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
            }
            log.info("WBI 爬取全部完成");
        }).start();
    }

    public IPage<VideoChannel> listVideos(String keyword, String category, String platform, int pageNum, int pageSize) {
        Page<VideoChannel> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<VideoChannel> wrapper = new LambdaQueryWrapper<VideoChannel>()
                .orderByDesc(VideoChannel::getPlayCount);

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(VideoChannel::getTitle, keyword.trim())
                    .or().like(VideoChannel::getTags, keyword.trim())
                    .or().like(VideoChannel::getDescription, keyword.trim()));
        }
        if (category != null && !category.trim().isEmpty()) {
            wrapper.eq(VideoChannel::getCategory, category.trim());
        }
        if (platform != null && !platform.trim().isEmpty()) {
            wrapper.eq(VideoChannel::getPlatform, platform.trim().toUpperCase());
        }
        return videoChannelMapper.selectPage(page, wrapper);
    }

    public VideoChannel getById(Long id) {
        return videoChannelMapper.selectById(id);
    }

    public List<String> getCategories() {
        List<VideoChannel> list = videoChannelMapper.selectList(
                new LambdaQueryWrapper<VideoChannel>().select(VideoChannel::getCategory).groupBy(VideoChannel::getCategory));
        List<String> categories = new ArrayList<>();
        for (VideoChannel v : list) {
            if (v.getCategory() != null && !v.getCategory().isEmpty()) {
                categories.add(v.getCategory());
            }
        }
        return categories;
    }

    private String mapCategory(String keyword) {
        if (keyword == null) return "综合";
        return keyword;
    }

    private String removeHtml(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]+>", "").replaceAll("&nbsp;", " ").trim();
    }
}
