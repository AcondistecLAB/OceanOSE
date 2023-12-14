/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.vmware.osis.huawei.utils;

import static com.vmware.osis.model.OsisUser.RoleEnum;

import com.vmware.osis.huawei.model.Account;
import com.vmware.osis.huawei.model.AccountUser;
import com.vmware.osis.huawei.model.BucketBean;
import com.vmware.osis.huawei.model.BucketBeanInfo;
import com.vmware.osis.huawei.model.UserAccessKey;
import com.vmware.osis.huawei.util.RandomUtil;
import com.vmware.osis.model.OsisBucketMeta;
import com.vmware.osis.model.OsisS3Credential;
import com.vmware.osis.model.OsisTenant;
import com.vmware.osis.model.OsisUsage;
import com.vmware.osis.model.OsisUser;

import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public final class ModelConverter {

    private ModelConverter() {
    }

    public static AccountUser toAccountUser(OsisUser osisUser) {
        AccountUser user = new AccountUser();
        user.setAccountId(osisUser.getTenantId());
        user.setUserId(osisUser.getUserId());
        user.setUserName(osisUser.getUsername());
        user.setCdUserId(osisUser.getCdUserId());
        user.setCdTenantId(osisUser.getCdTenantId());
        user.setCanonicalUserId(osisUser.getCanonicalUserId());
        user.setEmail(osisUser.getEmail());
        user.setStatus(osisUser.getActive() == null ? true : osisUser.getActive());
        user.setRole(osisUser.getRole().getValue());
        return user;
    }

    public static OsisS3Credential toOsisS3Credential(UserAccessKey userAccessKey) {
        return new OsisS3Credential().accessKey(userAccessKey.getAccessKeyId())
            .secretKey(userAccessKey.getSecretAccessKey())
            .active("Active".equals(userAccessKey.getStatus()))
            .immutable(false)
            .tenantId(userAccessKey.getAccountId())
            .userId(userAccessKey.getUserId())
            .username(userAccessKey.getUserName())
            .cdTenantId(userAccessKey.getCdTenantId())
            .creationDate(Instant.parse(userAccessKey.getCreateDate()))
            .cdUserId(userAccessKey.getCdUserId());
    }

    public static OsisTenant toOsisTenant(Account account) {
        return new OsisTenant().name(account.getAccountName())
            .active(account.getStatus().equals("Active"))
            .cdTenantIds(Arrays.asList(account.getCdTenantId().split(",")))
            .tenantId(account.getAccountId());
    }

    public static OsisUser toOsisUser(AccountUser user, List<OsisS3Credential> osisS3Credentials) {
        return new OsisUser().active(user.getStatus())
            .email(user.getEmail())
            .role(RoleEnum.fromValue(user.getRole()))
            .tenantId(user.getAccountId())
            .userId(user.getUserId())
            .canonicalUserId(user.getCanonicalUserId())
            .displayName(user.getUserName())
            .cdTenantId(user.getCdTenantId())
            .cdUserId(user.getCdUserId())
            .osisS3Credentials(osisS3Credentials);
    }

    public static OsisUsage hwtoOsisUsage(List<BucketBeanInfo> bi) {
        OsisUsage result = new OsisUsage();
        if (bi == null || bi.isEmpty()) {
            return result;
        }
        result.setBucketCount((long) bi.size());
        result.setObjectCount(bi.stream().mapToLong(BucketBeanInfo::getObjectNumber).sum());
        result.setUsedBytes(bi.stream().mapToLong(BucketBeanInfo::getSize).sum());
        return result;
    }

    public static OsisBucketMeta toOsisBucketMeta(BucketBean bucketBean) {
        return new OsisBucketMeta().name(bucketBean.getName())
            .creationDate(Instant.parse(bucketBean.getCreationDate()))
            .userId(bucketBean.getOwnerId());
    }

    public static Account toHwAccount(OsisTenant osisTenant) {
        Account account = new Account();
        if (StringUtils.isBlank(osisTenant.getTenantId())) {
            account.setAccountId(RandomUtil.random(9));
        } else {
            account.setAccountId(osisTenant.getTenantId());
        }
        account.setAccountName(osisTenant.getName());
        String cdIds = String.join(",", osisTenant.getCdTenantIds());
        account.setCdTenantId(cdIds);
        return account;
    }
}