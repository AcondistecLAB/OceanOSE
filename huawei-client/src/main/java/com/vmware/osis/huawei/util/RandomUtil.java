package com.vmware.osis.huawei.util;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.Random;

/**
 * @author Administrator
 * @ClassName SnowflakeUtil
 * @Description TODO
 **/
public class RandomUtil {
    private RandomUtil() {

    }

    public static String random(int count) {
        return RandomStringUtils.randomNumeric(count);
    }
}
