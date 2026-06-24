package com.zhixun.erp.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.erp.health.entity.MemberHealthProfile;
import com.zhixun.erp.health.mapper.MemberHealthProfileMapper;
import com.zhixun.erp.user.entity.User;
import com.zhixun.erp.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberImportService {

    private final UserMapper userMapper;
    private final MemberHealthProfileMapper healthMapper;

    private static final Map<String, String> GOAL_MAP = new HashMap<>();
    static {
        GOAL_MAP.put("增肌", "MUSCLE_GAIN");
        GOAL_MAP.put("减脂", "FAT_LOSS");
        GOAL_MAP.put("塑形", "BODY_SHAPING");
        GOAL_MAP.put("体能提升", "FITNESS");
    }

    public Map<String, Object> importMembers(MultipartFile file) {
        int total = 0, success = 0, skipped = 0, failed = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            // Skip header
            String line = reader.readLine();
            if (line == null) {
                throw new RuntimeException("CSV 文件为空");
            }

            while ((line = reader.readLine()) != null) {
                total++;
                try {
                    String[] fields = parseCSVLine(line);
                    if (fields.length < 28) {
                        log.warn("Row {} has {} fields (expected 28), skipping", total, fields.length);
                        failed++;
                        continue;
                    }

                    String username = fields[1].trim();

                    // Check duplicate
                    User existing = userMapper.selectOne(new LambdaQueryWrapper<User>()
                            .eq(User::getUsername, username));
                    if (existing != null) {
                        skipped++;
                        continue;
                    }

                    // Insert user
                    User user = new User();
                    user.setUsername(username);
                    user.setPassword("123456");
                    user.setRealName(fields[2].trim());
                    user.setGender(fields[3].trim());
                    user.setBirthday(fields[4].trim());
                    user.setPhone(fields[5].trim());
                    user.setEmail(fields[6].trim());
                    user.setRole("MEMBER");
                    user.setCreateTime(LocalDateTime.now());
                    userMapper.insert(user);

                    // Insert health profile
                    MemberHealthProfile hp = new MemberHealthProfile();
                    hp.setUserId(user.getId());
                    hp.setHeight(parseBigDecimal(fields[7]));
                    hp.setWeight(parseBigDecimal(fields[8]));
                    // fields[9] = BMI, skip
                    hp.setBodyFat(parseBigDecimal(fields[10]));
                    hp.setMuscleMass(parseBigDecimal(fields[11]));
                    hp.setRestingHeartRate(parseInteger(fields[12]));
                    hp.setBpSystolic(parseInteger(fields[13]));
                    hp.setBpDiastolic(parseInteger(fields[14]));
                    hp.setBloodType(parseString(fields[15]));
                    hp.setAllergies(parseString(fields[16]));
                    hp.setMedicalHistory(parseString(fields[17]));
                    hp.setCurrentMedications(parseString(fields[18]));
                    hp.setEmergencyContactName(parseString(fields[19]));
                    hp.setEmergencyContactPhone(parseString(fields[20]));
                    hp.setTargetWeight(parseBigDecimal(fields[21]));
                    hp.setTargetBodyFat(parseBigDecimal(fields[22]));
                    hp.setTargetMuscleMass(parseBigDecimal(fields[23]));
                    hp.setFitnessGoal(GOAL_MAP.getOrDefault(fields[24].trim(), fields[24].trim()));
                    hp.setWeeklyWorkoutFreq(parseInteger(fields[25]));
                    hp.setTargetDate(parseDate(fields[26]));
                    hp.setGoalNotes(parseString(fields[27]));
                    hp.setCreateTime(LocalDateTime.now());
                    healthMapper.insert(hp);

                    success++;
                } catch (Exception e) {
                    log.error("Row {} failed: {}", total, e.getMessage());
                    failed++;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("读取 CSV 文件失败: " + e.getMessage(), e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("skipped", skipped);
        result.put("failed", failed);
        return result;
    }

    private String[] parseCSVLine(String line) {
        // Simple CSV parser handling quoted fields
        return line.split(",", -1);
    }

    private BigDecimal parseBigDecimal(String s) {
        if (s == null || s.trim().isEmpty() || "无".equals(s.trim())) return null;
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInteger(String s) {
        if (s == null || s.trim().isEmpty() || "无".equals(s.trim())) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String parseString(String s) {
        if (s == null || s.trim().isEmpty() || "无".equals(s.trim())) return null;
        return s.trim();
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.trim().isEmpty() || "无".equals(s.trim())) return null;
        try {
            return LocalDate.parse(s.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
