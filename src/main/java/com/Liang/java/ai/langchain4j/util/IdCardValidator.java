package com.Liang.java.ai.langchain4j.util;

/**
 * 中国居民身份证号验证工具
 * 实现18位身份证号的格式校验和校验码算法验证
 */
public class IdCardValidator {

    // 加权因子
    private static final int[] WEIGHT = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    // 校验码映射
    private static final char[] CHECK_CODE = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    /**
     * 验证18位身份证号是否合法
     * @param idCard 身份证号
     * @return 验证结果消息，null 表示验证通过
     */
    public static String validate(String idCard) {
        if (idCard == null) {
            return "身份证号不能为空";
        }

        // 去除首尾空格
        idCard = idCard.trim().toUpperCase();

        // 长度校验
        if (idCard.length() != 18) {
            return "身份证号必须为18位";
        }

        // 前17位必须为数字
        String body = idCard.substring(0, 17);
        if (!body.matches("\\d{17}")) {
            return "身份证号前17位必须为数字";
        }

        // 校验码验证
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (body.charAt(i) - '0') * WEIGHT[i];
        }
        char expectedCheckCode = CHECK_CODE[sum % 11];
        char actualCheckCode = idCard.charAt(17);

        if (expectedCheckCode != actualCheckCode) {
            return "身份证号校验码不正确，请检查";
        }

        // 出生日期校验
        String birth = idCard.substring(6, 14);
        try {
            int year = Integer.parseInt(birth.substring(0, 4));
            int month = Integer.parseInt(birth.substring(4, 6));
            int day = Integer.parseInt(birth.substring(6, 8));
            if (year < 1900 || year > 2100) {
                return "身份证号出生年份不合法";
            }
            if (month < 1 || month > 12) {
                return "身份证号出生月份不合法";
            }
            if (day < 1 || day > 31) {
                return "身份证号出生日期不合法";
            }
        } catch (NumberFormatException e) {
            return "身份证号出生日期格式不正确";
        }

        return null; // 验证通过
    }
}