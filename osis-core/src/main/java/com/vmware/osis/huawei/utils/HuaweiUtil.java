/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.vmware.osis.huawei.utils;

import static com.vmware.osis.huawei.utils.HuaweiConstants.ICON_PATH;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.vmware.osis.model.Page;
import com.vmware.osis.model.PageInfo;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HuaweiUtil {

    private HuaweiUtil() {
    }

    public static <P extends Page, T> P paginate(long offset, long limit, P page, List<T> items) {
        if (offset < items.size()) {
            long end = (offset + limit) < items.size() ? offset + limit : items.size();
            page.setPageInfo(new PageInfo().total((long) items.size()).offset(offset).limit(limit));
            page.setItems(items.subList((int) offset, (int) end));
            return page;
        } else {
            page.setPageInfo(new PageInfo().total((long) items.size()).offset(offset).limit(limit));
            page.setItems(Collections.emptyList());
            return page;
        }
    }

    public static <P extends Page, T> P paginate(long offset, long limit, P page, List<T> items, Long total) {
        PageInfo pageInfo = new PageInfo();
        pageInfo.setLimit(limit);
        pageInfo.setOffset(offset);
        pageInfo.setTotal(0L);
        page.setPageInfo(pageInfo);
        page.setItems(Collections.emptyList());

        if (items != null && items.size() > 0) {
            pageInfo.setTotal(total);
            pageInfo.setOffset(offset);
            page.setItems(items);
        }

        return page;
    }

    public static Map<String, String> parseFilter(String filter) {
        if (StringUtils.isBlank(filter)) {
            return Collections.emptyMap();
        }

        Map<String, String> kvMap = new HashMap<>();
        Arrays.stream(StringUtils.split(filter, ";"))
            .filter(exp -> exp.contains("==") && exp.indexOf("==") == exp.lastIndexOf("=="))
            .forEach(exp -> {
                String[] kv = StringUtils.split(exp, "==");
                if (kv.length == 2) {
                    kvMap.put(kv[0], kv[1]);
                }

            });
        return kvMap;
    }

    public static String generateFilter(String... kvPairs) {
        if (kvPairs == null || kvPairs.length == 0 || kvPairs.length % 2 == 1) {
            return null;
        }

        List<String> exps = new ArrayList<>();
        for (int i = 0; i < kvPairs.length / 2; i++) {
            if (Strings.isNullOrEmpty(kvPairs[2 * i + 1])) {
                continue;
            }
            exps.add(String.format("%s==%s", kvPairs[2 * i], kvPairs[2 * i + 1]));
        }

        return Joiner.on(";").join(exps);
    }

    public static String getLogoPath() {
        return ICON_PATH;
    }

    public static URI getLogoUri(String domain) {
        return URI.create(domain + ICON_PATH);
    }
}
