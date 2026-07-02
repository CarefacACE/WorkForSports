package com.zhixun.erp.video.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.video.entity.VideoChannel;
import com.zhixun.erp.video.service.VideoChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/video")
public class VideoChannelController {

    private final VideoChannelService videoChannelService;

    @GetMapping("/list")
    public Result<IPage<VideoChannel>> listVideos(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String platform,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "12") int pageSize) {
        return Result.success(videoChannelService.listVideos(keyword, category, platform, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public Result<VideoChannel> getVideo(@PathVariable Long id) {
        VideoChannel video = videoChannelService.getById(id);
        if (video == null) {
            return Result.fail(404, "视频不存在");
        }
        return Result.success(video);
    }

    @PostMapping("/crawl")
    public Result<String> triggerCrawl(@RequestParam(defaultValue = "健身教学") String keyword) {
        int count = videoChannelService.crawlFromBilibili(keyword);
        return Result.success("爬取完成，新增 " + count + " 个视频");
    }

    @PostMapping("/seed")
    public Result<String> triggerSeed() {
        int count = videoChannelService.seedVideos();
        return Result.success("种子数据注入完成，新增 " + count + " 个视频");
    }

    @GetMapping("/categories")
    public Result<List<String>> getCategories() {
        return Result.success(videoChannelService.getCategories());
    }
}
