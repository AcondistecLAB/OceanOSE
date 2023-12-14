/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.vmware.osis.huawei.service.impl;

import static com.vmware.osis.huawei.utils.HuaweiConstants.IAM_PREFIX;
import static com.vmware.osis.huawei.utils.HuaweiConstants.OSIS_ACCESS_KEY;
import static com.vmware.osis.huawei.utils.HuaweiConstants.OSIS_ACTIVE;
import static com.vmware.osis.huawei.utils.HuaweiConstants.OSIS_CANONICAL_USER_ID;
import static com.vmware.osis.huawei.utils.HuaweiConstants.OSIS_CD_TENANT_ID;
import static com.vmware.osis.huawei.utils.HuaweiConstants.OSIS_CD_USER_ID;
import static com.vmware.osis.huawei.utils.HuaweiConstants.OSIS_DISPLAY_NAME;
import static com.vmware.osis.huawei.utils.HuaweiConstants.OSIS_TENANT_ID;
import static com.vmware.osis.huawei.utils.HuaweiConstants.OSIS_USER_ID;
import static com.vmware.osis.huawei.utils.HuaweiUtil.paginate;
import static com.vmware.osis.huawei.utils.HuaweiUtil.parseFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.vmware.osis.huawei.AppEnv;
import com.vmware.osis.huawei.HwAdmin;
import com.vmware.osis.huawei.model.Account;
import com.vmware.osis.huawei.model.AccountUser;
import com.vmware.osis.huawei.model.BucketBean;
import com.vmware.osis.huawei.model.BucketBeanInfo;
import com.vmware.osis.huawei.model.UserAccessKey;
import com.vmware.osis.huawei.utils.HuaweiUtil;
import com.vmware.osis.huawei.utils.ModelConverter;
import com.vmware.osis.model.Information;
import com.vmware.osis.model.InformationServices;
import com.vmware.osis.model.OsisBucketMeta;
import com.vmware.osis.model.OsisCaps;
import com.vmware.osis.model.OsisS3Capabilities;
import com.vmware.osis.model.OsisS3Credential;
import com.vmware.osis.model.OsisTenant;
import com.vmware.osis.model.OsisUsage;
import com.vmware.osis.model.OsisUser;
import com.vmware.osis.model.PageOfOsisBucketMeta;
import com.vmware.osis.model.PageOfS3Credentials;
import com.vmware.osis.model.PageOfTenants;
import com.vmware.osis.model.PageOfUsers;
import com.vmware.osis.model.exception.BadRequestException;
import com.vmware.osis.model.exception.InternalException;
import com.vmware.osis.model.exception.NotFoundException;
import com.vmware.osis.resource.OsisCapsManager;
import com.vmware.osis.service.OsisService;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class HuaweiOsisService implements OsisService {
    private static final Logger logger = LoggerFactory.getLogger(HuaweiOsisService.class);

    private static final String S3_CAPABILITIES_JSON = "s3capabilities.json";

    @Autowired
    private AppEnv appEnv;

    @Autowired
    private HwAdmin hwAdmin;

    @Autowired
    private OsisCapsManager osisCapsManager;

    @Autowired
    private Gson gson;

    @Override
    public OsisTenant createTenant(OsisTenant osisTenant) {
        Account account = ModelConverter.toHwAccount(osisTenant);
        Account reAccount = hwAdmin.createAccount(account);
        return ModelConverter.toOsisTenant(reAccount);
    }

    @Override
    public PageOfTenants queryTenants(long offset, long limit, String filter) {
        Map<String, String> kvMap = parseFilter(filter);
        String tenantId = kvMap.get(OSIS_TENANT_ID);
        String cdTenantId = kvMap.get(OSIS_CD_TENANT_ID);

        Account matcher = new Account();
        matcher.setAccountId(tenantId);
        matcher.setCdTenantId(cdTenantId);
        Page<Account> accountPage = hwAdmin.listTenants(offset, limit, matcher);

        long total = 0;
        long offset_ = offset;
        List<OsisTenant> list = Collections.emptyList();
        if (!accountPage.isEmpty()) {
            total = accountPage.getTotalElements();
            list = accountPage.stream()
                .map(account -> ModelConverter.toOsisTenant(account))
                .collect(Collectors.toList());
            offset_ = accountPage.getPageable().getPageNumber() * limit;
        }
        return paginate(offset_, limit, new PageOfTenants(), list, total);
    }

    @Override
    public PageOfTenants listTenants(long offset, long limit) {
        return this.queryTenants(offset, limit, null);
    }

    @Override
    public OsisUser createUser(OsisUser osisUser) {
        String errorMsg = String.format("The tenant %s does not exist.", osisUser.getTenantId());
        if (!headTenant(osisUser.getTenantId())) {
            logger.error(errorMsg);
            throw new BadRequestException(errorMsg);
        }
        Account account = this.hwAdmin.getAccount(osisUser.getTenantId()).get();
        if (hasUser(osisUser.getTenantId(), osisUser.getUserId())) {
            logger.info("The user has exists!");
            return getUser(osisUser.getTenantId(), osisUser.getUserId());
        }

        if (StringUtils.isBlank(osisUser.getCanonicalUserId())) {
            osisUser.setCanonicalUserId(account.getCanonicalUserId());
        }

        AccountUser user = ModelConverter.toAccountUser(osisUser);
        logger.info("create store user begin!body={}", gson.toJson(user));
        AccountUser reUser = hwAdmin.createUser(user);
        if (reUser == null) {
            throw new BadRequestException("create user return null!");
        }
        logger.info("create store user end!");

        return ModelConverter.toOsisUser(reUser,
            this.getOsisS3CredentialsByUserId(reUser.getAccountId(), reUser.getUserId(), true));
    }

    @Override
    public PageOfUsers queryUsers(long offset, long limit, String filter) {
        logger.info("queryUsers!offset={},limit={},filter={}", offset, limit, filter);

        Map<String, String> kvMap = parseFilter(filter);
        String tenantId = kvMap.get(OSIS_TENANT_ID);
        String cdTenantId = kvMap.get(OSIS_CD_TENANT_ID);
        String userId = kvMap.get(OSIS_USER_ID);
        String cdUserId = kvMap.get(OSIS_CD_USER_ID);
        String canonicalUserId = kvMap.get(OSIS_CANONICAL_USER_ID);
        String displayName = kvMap.get(OSIS_DISPLAY_NAME);
        String activeStr = kvMap.get(OSIS_ACTIVE);

        AccountUser matchUser = new AccountUser();
        matchUser.setAccountId(tenantId);
        matchUser.setUserId(userId);
        matchUser.setCdTenantId(cdTenantId);
        matchUser.setCdUserId(cdUserId);
        matchUser.setCanonicalUserId(canonicalUserId);
        matchUser.setUserName(displayName);
        if (StringUtils.isNotBlank(activeStr)) {
            matchUser.setStatus("Active".equals(activeStr));
        }
        Page<AccountUser> accountUserPage = hwAdmin.listUser(offset, limit, matchUser);

        long total = 0;
        long offset_ = offset;
        List<OsisUser> osisUsers = Collections.emptyList();
        if (!accountUserPage.isEmpty()) {
            total = accountUserPage.getTotalElements();
            osisUsers = accountUserPage.stream()
                .map(user -> ModelConverter.toOsisUser(user,
                    this.getOsisS3CredentialsByUserId(user.getAccountId(), user.getUserId(), false)))
                .collect(Collectors.toList());
            offset_ = accountUserPage.getPageable().getPageNumber() * limit;
        }
        return paginate(offset_, limit, new PageOfUsers(), osisUsers, total);
    }

    @Override
    public OsisS3Credential createS3Credential(String tenantId, String userId) {
        logger.info("createS3Credential begin!tenantId={},userId={}", tenantId, userId);
        Optional<Account> account = hwAdmin.getAccount(tenantId);
        if (!account.isPresent()) {
            throw new NotFoundException(String.format("The tenant %s doesn't exist", tenantId));
        }
        Optional<AccountUser> user = hwAdmin.getUser(userId);
        if (!user.isPresent()) {
            throw new NotFoundException(String.format("The user %s in tenant %s doesn't exist", userId, tenantId));
        }

        UserAccessKey userAccessKey = hwAdmin.createUserAccessKey(tenantId, userId);
        if (userAccessKey == null) {
            throw new InternalException(
                String.format("Fail to create credential for user %s in tenant %s", userId, tenantId));
        }
        logger.info("createS3Credential end!tenantId={},userId={}", tenantId, userId);
        return ModelConverter.toOsisS3Credential(userAccessKey);
    }

    @Override
    public PageOfS3Credentials queryS3Credentials(long offset, long limit, String filter) {
        Map<String, String> kvMap = parseFilter(filter);
        String tenantId = kvMap.get(OSIS_TENANT_ID);
        String cdTenantId = kvMap.get(OSIS_CD_TENANT_ID);
        String userId = kvMap.get(OSIS_USER_ID);
        String cdUserId = kvMap.get(OSIS_CD_USER_ID);
        String activeStr = kvMap.get(OSIS_ACTIVE);
        String accessKey = kvMap.get(OSIS_ACCESS_KEY);

        UserAccessKey userAccessKey = new UserAccessKey();
        userAccessKey.setAccountId(tenantId);
        userAccessKey.setCdTenantId(cdTenantId);
        userAccessKey.setUserId(userId);
        userAccessKey.setCdUserId(cdUserId);
        userAccessKey.setStatus(activeStr);
        userAccessKey.setAccessKeyId(accessKey);

        Page<UserAccessKey> userAccessKeyPage = hwAdmin.listUserAccessKey(offset, limit, userAccessKey);

        long total = 0;
        long offset_ = offset;
        List<OsisS3Credential> list = Collections.emptyList();
        if (!userAccessKeyPage.isEmpty()) {
            total = userAccessKeyPage.getTotalElements();
            list = userAccessKeyPage.stream()
                .map(accessKeyObj -> ModelConverter.toOsisS3Credential(accessKeyObj))
                .collect(Collectors.toList());
            offset_ = userAccessKeyPage.getPageable().getPageNumber() * limit;
        }
        return paginate(offset_, limit, new PageOfS3Credentials(), list, total);
    }

    @Override
    public String getProviderConsoleUrl() {
        return appEnv.getConsoleEndpoint();
    }

    @Override
    public String getTenantConsoleUrl(String tenantId) {
        return appEnv.getConsoleEndpoint();
    }

    @Override
    public OsisS3Capabilities getS3Capabilities() {
        OsisS3Capabilities osisS3Capabilities = new OsisS3Capabilities();
        try {
            osisS3Capabilities = new ObjectMapper().readValue(
                new ClassPathResource(S3_CAPABILITIES_JSON).getInputStream(), OsisS3Capabilities.class);
        } catch (IOException e) {
            logger.info("Fail to load S3 capabilities from configuration file {}.", S3_CAPABILITIES_JSON);
        }
        return osisS3Capabilities;
    }

    @Override
    public void deleteS3Credential(String tenantId, String userId, String accessKey) {
        hwAdmin.deleteUserAccessKey(tenantId, userId, accessKey);
    }

    @Override
    public OsisS3Credential updateCredentialStatus(String tenantId, String userId, String accessKey, boolean active) {
        UserAccessKey userAccessKey = hwAdmin.updateUserAccessKey(tenantId, userId, accessKey, active);
        if (userAccessKey != null) {
            return ModelConverter.toOsisS3Credential(userAccessKey);
        }
        throw new NotFoundException("updateCredentialStatus error,S3 credential not found!");
    }

    @Override
    public void deleteTenant(String tenantId, Boolean purgeData) {
        hwAdmin.deleteAccount(tenantId);
    }

    @Override
    public OsisTenant updateTenant(String tenantId, OsisTenant osisTenant) {
        this.headTenant(tenantId);

        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("name", osisTenant.getName());
        paramMap.put("status", osisTenant.getActive() ? "Active" : "Inactive");
        paramMap.put("email", paramMap.get("email"));
        if (osisTenant.getCdTenantIds() != null && !osisTenant.getCdTenantIds().isEmpty()) {
            paramMap.put("cd_tenant_id", String.join(",", osisTenant.getCdTenantIds()));
        }
        Account newAccount = hwAdmin.modAccount(tenantId, paramMap);

        return ModelConverter.toOsisTenant(newAccount);
    }

    @Override
    public void deleteUser(String tenantId, String userId, Boolean purgeData) {
        try {
            if (!hasUser(tenantId, userId)) {
                return;
            }
        } catch (Exception e) {
            return;
        }
        hwAdmin.deleteUser(tenantId, userId);
    }

    @Override
    public OsisS3Credential getS3Credential(String accessKey) {
        PageOfS3Credentials pageOfS3Credentials = this.queryS3Credentials(0L, 1L,
            HuaweiUtil.generateFilter(OSIS_ACCESS_KEY, accessKey));

        if (pageOfS3Credentials.getPageInfo().getTotal() < 1) {
            throw new NotFoundException("S3 credential not found");
        }

        return pageOfS3Credentials.getItems().get(0);
    }

    @Override
    public OsisTenant getTenant(String tenantId) {
        Optional<Account> tenant = this.hwAdmin.getAccount(tenantId);
        if (!tenant.isPresent()) {
            throw new NotFoundException("The tenant not found!");
        }

        return ModelConverter.toOsisTenant(tenant.get());
    }

    @Override
    public OsisUser getUser(String canonicalUserId) {
        Optional<AccountUser> user = this.hwAdmin.getUserWithCanonicalUserId(canonicalUserId);
        if (!user.isPresent()) {
            String errorMsg = "The user not found!";
            logger.error(errorMsg + "canonicalUserId={}", canonicalUserId);
            throw new NotFoundException(errorMsg);
        }

        return ModelConverter.toOsisUser(user.get(),
            this.getOsisS3CredentialsByUserId(user.get().getAccountId(), user.get().getUserId(), false));
    }

    @Override
    public OsisUser getUser(String tenantId, String userId) {
        Optional<AccountUser> user = this.hwAdmin.getUser(userId);
        if (!user.isPresent()) {
            String errorMsg = "The user not found!";
            logger.error(errorMsg + "tenantId={}, userId={}", tenantId, userId);
            throw new NotFoundException(errorMsg);
        }
        return ModelConverter.toOsisUser(user.get(),
            this.getOsisS3CredentialsByUserId(user.get().getAccountId(), user.get().getUserId(), false));
    }

    @Override
    public boolean headTenant(String tenantId) {
        try {
            return this.getTenant(tenantId) != null;
        } catch (Exception e) {
            throw new NotFoundException(String.format("No tenant found with tenantId=%s", tenantId));
        }
    }

    @Override
    public boolean headUser(String tenantId, String userId) {
        if (hasUser(tenantId, userId)) {
            return true;
        } else {
            throw new NotFoundException(
                String.format("No user found with tenantId=%s and userId=%s", tenantId, userId));
        }
    }

    @Override
    public PageOfS3Credentials listS3Credentials(String tenantId, String userId, Long offset, Long limit) {
        return this.queryS3Credentials(offset, limit,
            HuaweiUtil.generateFilter(OSIS_TENANT_ID, tenantId, OSIS_USER_ID, userId));
    }

    @Override
    public PageOfUsers listUsers(String tenantId, long offset, long limit) {
        return this.queryUsers(offset, limit, HuaweiUtil.generateFilter(OSIS_TENANT_ID, tenantId));
    }

    @Override
    public OsisUser updateUser(String tenantId, String userId, OsisUser osisUser) {
        AccountUser user = this.hwAdmin.updateUserStatus(userId, osisUser.getActive());
        return ModelConverter.toOsisUser(user,
            this.getOsisS3CredentialsByUserId(user.getAccountId(), user.getUserId(), false));
    }

    @Override
    public Information getInformation(String domain) {
        return new Information().addAuthModesItem(
                appEnv.isApiTokenEnabled() ? Information.AuthModesEnum.BEARER : Information.AuthModesEnum.BASIC)
            .storageClasses(appEnv.getStorageInfo())
            .regions(appEnv.getRegionInfo())
            .platformName(appEnv.getPlatformName())
            .platformVersion(appEnv.getPlatformVersion())
            .apiVersion(appEnv.getApiVersion())
            .notImplemented(osisCapsManager.getNotImplements())
            .logoUri(HuaweiUtil.getLogoUri(domain))
            .services(new InformationServices().iam(domain + IAM_PREFIX).s3(appEnv.getS3Endpoint()))
            .status(checkStatus())
            .iam(true);
    }

    private Information.StatusEnum checkStatus() {
        Information.StatusEnum statusEnum = Information.StatusEnum.NORMAL;
        try {
            this.hwAdmin.checkStatus();
        } catch (Exception ex) {
            statusEnum = Information.StatusEnum.ERROR;
            logger.error("checkStatus error!", ex);
        }
        return statusEnum;
    }

    @Override
    public OsisCaps updateOsisCaps(OsisCaps osisCaps) {
        osisCapsManager.updateOsisCaps(osisCaps);
        return osisCapsManager.getOsisCaps();
    }

    @Override
    public PageOfOsisBucketMeta getBucketList(String tenantId, long offset, long limit) {
        Optional<Account> account = hwAdmin.getAccount(tenantId);
        if (!account.isPresent()) {
            throw new NotFoundException(String.format("The tenant %s doesn't exist!", tenantId));
        }
        List<BucketBean> bucketBeans = hwAdmin.listBucket(tenantId, offset, limit);
        List<OsisBucketMeta> list = bucketBeans.stream()
            .map(bucketBean -> ModelConverter.toOsisBucketMeta(bucketBean))
            .collect(Collectors.toList());
        return paginate(offset, limit, new PageOfOsisBucketMeta(), list);
    }

    @Override
    public OsisUsage getOsisUsage(Optional<String> tenantId, Optional<String> userId) {
        List<BucketBeanInfo> bi;
        if (tenantId.isPresent() && userId.isPresent()) {
            bi = null;
        } else if (tenantId.isPresent()) {
            bi = hwAdmin.listBucketInfo(tenantId.get(), 0, 0);
        } else {
            bi = null;
        }
        return ModelConverter.hwtoOsisUsage(bi);
    }

    private boolean hasUser(String tenantId, String userId) {
        try {
            return this.getUser(tenantId, userId) != null;
        } catch (Exception e) {
            logger.info("No user found with tenantId={} and userId={}", tenantId, userId);
            return false;
        }
    }

    private List<OsisS3Credential> getOsisS3CredentialsByUserId(String tenantId, String userId, boolean createNew) {
        PageOfS3Credentials pageOfS3Credentials = this.queryS3Credentials(0L, 10L,
            HuaweiUtil.generateFilter(OSIS_USER_ID, userId));
        if (pageOfS3Credentials != null && pageOfS3Credentials.getPageInfo().getTotal().intValue() > 0) {
            return pageOfS3Credentials.getItems();
        }
        OsisS3Credential s3Credential = null;
        if (createNew) {
            logger.info("not found S3Credential,create new!tenantId={},userId={}", tenantId, userId);
            s3Credential = this.createS3Credential(tenantId, userId);
        }

        return Arrays.asList(s3Credential);
    }
}
