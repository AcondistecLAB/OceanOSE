package com.vmware.osis.huawei.impl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.vmware.osis.huawei.HwAdmin;
import com.vmware.osis.huawei.model.*;
import com.vmware.osis.huawei.repository.AccessKeyRepository;
import com.vmware.osis.huawei.repository.AccountRepository;
import com.vmware.osis.huawei.repository.AccountUserRepository;
import com.vmware.osis.huawei.repository.UserAccessKeyRepository;
import com.vmware.osis.huawei.util.HwClientUtil;
import com.vmware.osis.huawei.util.HwObsServiceUtil;
import com.vmware.osis.huawei.util.Result;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

/**
 * @author Administrator
 * @ClassName HwAdminImpl
 * @Description TODO
 **/
@Service
public class HwAdminImpl implements HwAdmin {
    private static final Logger log = LoggerFactory.getLogger(HwAdminImpl.class);

    private static final String INACTIVE_USERPOLICY_NAME = "inactive_user";

    private static final String USER_POLICY_DOCUMENT
        = "{\"Statement\": [{\"Sid\": \"vcd_readWrite\",\"Effect\": \"Allow\",\"Action\":[\"s3:*\"],\"Resource\": \"*\"}]}";

    @Value("${osis.huawei.rgw.endpoint}")
    private String endpoint;

    @Value("${osis.huawei.rgw.access-key}")
    private String adminAccessKey;

    @Value("${osis.huawei.rgw.secret-key}")
    private String adminSecretKey;

    @Value("${osis.huawei.s3.endpoint}")
    private String endpointS3;

    @Autowired
    private Gson gson;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccessKeyRepository accessKeyRepository;

    @Autowired
    private AccountUserRepository accountUserRepository;

    @Autowired
    private UserAccessKeyRepository userAccessKeyRepository;

    /**
     * @param account
     * @return
     */
    @Override
    public Account createAccount(Account account) {
        Map<String, String> businessParams = new HashMap<>();
        businessParams.put("AccountId", account.getAccountId());
        businessParams.put("AccountName", account.getAccountName());
        //businessParams.put("Email", "myEmail@huawei7.com");
        Result<String> createAccountResult = HwClientUtil.executeAdminGet("CreateAccountWithAll", businessParams,
            endpoint, adminAccessKey, adminSecretKey);
        Account reAccount = null;
        if (createAccountResult.is2xxSuccess()) {
            JsonObject responseObj = gson.fromJson(createAccountResult.getData(), JsonObject.class)
                .getAsJsonObject("CreateAccountWithAllResponse");
            JsonObject accountObj = responseObj.getAsJsonObject("CreateAccountResult").getAsJsonObject("Account");
            reAccount = gson.fromJson(accountObj.toString(), Account.class);
            reAccount.setCdTenantId(account.getCdTenantId());
            accountRepository.saveAndFlush(reAccount);

            JsonObject AccessKeyObj = responseObj.getAsJsonObject("CreateAccessKeyResult").getAsJsonObject("AccessKey");
            AccountAccessKey accountAccessKey = gson.fromJson(AccessKeyObj.toString(), AccountAccessKey.class);
            accessKeyRepository.saveAndFlush(accountAccessKey);
        } else {
            log.error("create account error!code={},errorMsg:{}", createAccountResult.getCode(),
                createAccountResult.getMsg());
            throw new HwAdminException(createAccountResult.getCode(), createAccountResult.getMsg());
        }
        return reAccount;
    }

    /**
     * @param id 帐户ID
     * @return
     */
    @Override
    public Optional<Account> getAccount(String id) {
        Optional<Account> account = accountRepository.findById(id);
        if (!account.isPresent()) {
            log.info("getAccount isPresent is false!try again by cdTenantId!id={}", id);
            account = accountRepository.findAccountByCdTenantId(id);
            log.info("getAccount findAccountByCdTenantId isPresent={}", account.isPresent());
        }
        return account;
    }

    /**
     * @param accountUser
     * @return
     */
    @Override
    public AccountUser createUser(AccountUser accountUser) {
        AccountAccessKey accountAccessKey = getAccountAccessKeyByAccountId(accountUser.getAccountId());
        Map<String, String> businessParams = new HashMap<>();
        businessParams.put("UserName", accountUser.getUserName());
        Result<String> createUserResult = HwClientUtil.executeGet("CreateUser", businessParams, endpoint,
            accountAccessKey.getAccessKeyId(), accountAccessKey.getSecretAccessKey());
        if (createUserResult.is2xxSuccess()) {
            String jsonResponse = createUserResult.getData();
            JsonObject userObj = gson.fromJson(jsonResponse, JsonObject.class)
                .getAsJsonObject("CreateUserResponse")
                .getAsJsonObject("CreateUserResult")
                .getAsJsonObject("User");
            String userId = userObj.get("UserId").getAsString();
            accountUser.setUserId(userId);
            accountUser.setArn(userObj.get("Arn").getAsString());
            accountUser.setPath(userObj.get("Path").getAsString());
            accountUser.setCreateDate(userObj.get("CreateDate").getAsString());
            accountUser.setCanonicalUserId(accountUser.getCanonicalUserId());
            accountUser.setStatus(true);

            // 设置用户策略
            businessParams.put("PolicyName", "vcd_readWrite");
            businessParams.put("PolicyDocument", USER_POLICY_DOCUMENT);
            Result<String> putUserPolicyResult = HwClientUtil.executeGet("PutUserPolicy", businessParams, endpoint,
                accountAccessKey.getAccessKeyId(), accountAccessKey.getSecretAccessKey());
            if (!putUserPolicyResult.is2xxSuccess()) {
                log.info("failed to set user policy!userName={}", accountUser.getUserName());
            }

            // 用户保存入库
            accountUserRepository.saveAndFlush(accountUser);

            return accountUser;
        } else {
            log.error("create user error!errorMsg={}", createUserResult.getMsg());
        }

        return null;
    }

    @Override
    public Optional<AccountUser> getUser(String userId) {
        if (StringUtils.isBlank(userId)) {
            return Optional.empty();
        }
        return accountUserRepository.findById(userId);
    }

    @Override
    public Optional<AccountUser> getUserWithCanonicalUserId(String canonicalUserId) {
        List<AccountUser> users = accountUserRepository.findAccountUsersByCanonicalUserId(canonicalUserId);
        AccountUser user = null;
        if (users != null && !users.isEmpty()) {
            user = users.get(0);
        }
        return Optional.ofNullable(user);
    }

    @Override
    public void deleteUser(String accountId, String userId) {
        AccountAccessKey accountAccessKey = getAccountAccessKeyByAccountId(accountId);
        if (accountAccessKey != null) {
            String accountAk = accountAccessKey.getAccessKeyId();
            String accountSk = accountAccessKey.getSecretAccessKey();
            Optional<AccountUser> userOptional = getUser(userId);
            if (userOptional.isPresent()) {
                AccountUser accountUser = userOptional.get();
                String userName = accountUser.getUserName();
                Map<String, String> businessParams = new HashMap<>();
                businessParams.put("UserName", userName);

                //删除用户前需要先删除用户认证、策略信息
                deleteUserAccessKey(accountId, userId, null);
                boolean deleteUserPolicies = deleteUserPolicies(accountAk, accountSk, userName);
                if (deleteUserPolicies) {
                    Result<String> deleteUserResult = HwClientUtil.executeGet("DeleteUser", businessParams, endpoint,
                        accountAk, accountSk);
                    if (deleteUserResult.is2xxSuccess()) {
                        accountUserRepository.deleteById(userId);
                    }
                }
            }
        }
    }

    private boolean deleteUserPolicies(String accountAk, String accountSk, String userName) {
        Map<String, String> businessParams = new HashMap<>();
        businessParams.put("UserName", userName);
        Result<String> result = HwClientUtil.executeGet("ListUserPolicies", businessParams, endpoint, accountAk,
            accountSk);
        if (result.is2xxSuccess()) {
            List<String> policyNames = new ArrayList<>();
            JsonObject listUserPoliciesResult = gson.fromJson(result.getData(), JsonObject.class)
                .getAsJsonObject("ListUserPoliciesResponse")
                .getAsJsonObject("ListUserPoliciesResult");
            JsonElement policyNamesEle = listUserPoliciesResult.get("PolicyNames");
            if (policyNamesEle.isJsonObject()) {
                JsonElement memberEle = policyNamesEle.getAsJsonObject().get("member");
                if (memberEle.isJsonPrimitive()) {
                    policyNames.add(memberEle.getAsString());
                } else {
                    memberEle.getAsJsonArray().forEach(jsonElement -> policyNames.add(jsonElement.getAsString()));
                }
            }

            // 删除用户策略
            policyNames.forEach(policyName -> {
                Map<String, String> deletePolicyParams = new HashMap<>();
                deletePolicyParams.put("UserName", userName);
                deletePolicyParams.put("PolicyName", policyName);
                HwClientUtil.executeGet("DeleteUserPolicy", deletePolicyParams, endpoint, accountAk, accountSk);
            });
            log.error("deleteUserPolicies success!userName={}", userName);
            return true;
        }

        log.error("deleteUserPolicies failed!userName={}", userName);
        return false;
    }

    @Override
    public AccountUser updateUserStatus(String userId, boolean isActive) {
        Optional<AccountUser> accountUserOpl = getUser(userId);
        if (accountUserOpl.isPresent()) {
            AccountUser accountUser = accountUserOpl.get();
            AccountAccessKey accountAccessKey = getAccountAccessKeyByAccountId(accountUser.getAccountId());
            String userName = accountUserOpl.get().getUserName();
            Map<String, String> businessParams = new HashMap<>();
            businessParams.put("UserName", userName);
            businessParams.put("PolicyName", INACTIVE_USERPOLICY_NAME);
            Result<String> policyResult = HwClientUtil.executeGet("GetUserPolicy", businessParams, endpoint,
                accountAccessKey.getAccessKeyId(), accountAccessKey.getSecretAccessKey());
            boolean hasInactivePolicy = policyResult.is2xxSuccess();
            if (isActive && hasInactivePolicy) {
                // 删除用户策略
                HwClientUtil.executeGet("DeleteUserPolicy", businessParams, endpoint, accountAccessKey.getAccessKeyId(),
                    accountAccessKey.getSecretAccessKey());
            } else if (!isActive && !hasInactivePolicy) {
                // 添加用户策略
                businessParams.put("PolicyDocument",
                    "{\"Statement\":[{\"Effect\":\"Deny\",\"Action\":\"*\",\"Resource\":\"*\"}]}");
                HwClientUtil.executeGet("PutUserPolicy", businessParams, endpoint, accountAccessKey.getAccessKeyId(),
                    accountAccessKey.getSecretAccessKey());
            } else {
                log.info("do not user policy action!isActive={},hasInactivePolicy={}", isActive, hasInactivePolicy);
            }
            accountUser.setStatus(isActive);

            return accountUser;
        } else {
            log.error("update user error,because the user not exists!");
        }
        return null;
    }

    @Override
    public Page<AccountUser> listUser(long offset, long limit, AccountUser matchUser) {
        return pageQuery(offset, limit, matchUser, accountUserRepository);
    }

    @Override
    public Page<Account> listTenants(long offset, long limit, Account matchAccount) {
        return pageQuery(offset, limit, matchAccount, accountRepository);
    }

    @Override
    public Account modAccount(String accountId, Map<String, String> paramMap) {
        log.info("modAccount,accountId={},request={}", accountId, gson.toJson(paramMap));
        Optional<Account> accountDbOpl = getAccount(accountId);
        Account accountDb = accountDbOpl.get();
        Map<String, String> businessParams = new HashMap<>();
        businessParams.put("AccountId", accountId);
        if (StringUtils.isNotBlank(paramMap.get("name"))) {
            businessParams.put("NewAccountName", paramMap.get("name"));
        }
        if (StringUtils.isNotBlank(paramMap.get("status"))) {
            businessParams.put("Status", paramMap.get("status"));
        }
        if (StringUtils.isNotBlank(paramMap.get("email"))) {
            businessParams.put("NewEmail", paramMap.get("email"));
        }
        Result<String> result = HwClientUtil.executeAdminGet("UpdateAccount", businessParams, endpoint, adminAccessKey,
            adminSecretKey);
        if (result.is2xxSuccess()) {
            JsonObject newAccountObj = gson.fromJson(result.getData(), JsonObject.class)
                .getAsJsonObject("UpdateAccountResponse")
                .getAsJsonObject("UpdateAccountResult")
                .getAsJsonObject("Account");
            accountDb.setAccountName(newAccountObj.get("AccountName").getAsString());
            accountDb.setStatus(newAccountObj.get("Status").getAsString());
            accountDb.setEmail(newAccountObj.get("Email").getAsString());
            if (StringUtils.isNotBlank(paramMap.get("cd_tenant_id"))) {
                accountDb.setCdTenantId(paramMap.get("cd_tenant_id"));
            }
            accountRepository.saveAndFlush(accountDb);
            return accountDb;
        }

        return null;
    }

    @Transactional
    @Override
    public void deleteAccount(String accountId) {
        // 删除账户前需要先删除账户下的所有用户
        List<AccountUser> users = accountUserRepository.findAccountUsersByAccountId(accountId);
        if (users != null && !users.isEmpty()) {
            users.forEach(user -> deleteUser(accountId, user.getUserId()));
        }

        Map<String, String> businessParams = new HashMap<>();
        businessParams.put("AccountId", accountId);
        Result<String> result = HwClientUtil.executeAdminGet("DeleteAccount", businessParams, endpoint, adminAccessKey,
            adminSecretKey);
        if (result.is2xxSuccess()) {
            accessKeyRepository.deleteById(accountId);
            accountRepository.deleteById(accountId);
        }
    }

    @Override
    public UserAccessKey createUserAccessKey(String accountId, String userId) {
        AccountUser user = this.getUser(userId).get();

        AccountAccessKey accountAccessKey = getAccountAccessKeyByAccountId(accountId);
        String accountAk = accountAccessKey.getAccessKeyId();
        String accountSk = accountAccessKey.getSecretAccessKey();

        Map<String, String> businessParams = new HashMap<>();
        businessParams.put("UserName", user.getUserName());
        Result<String> result = HwClientUtil.executeGet("CreateAccessKey", businessParams, endpoint, accountAk,
            accountSk);
        UserAccessKey userAccessKey = null;
        if (result.is2xxSuccess()) {
            JsonObject userAccessKeyObj = gson.fromJson(result.getData(), JsonObject.class)
                .getAsJsonObject("CreateAccessKeyResponse")
                .getAsJsonObject("CreateAccessKeyResult")
                .getAsJsonObject("AccessKey");
            userAccessKey = gson.fromJson(userAccessKeyObj.toString(), new TypeToken<UserAccessKey>() { }.getType());
            userAccessKey.setUserId(userId);
            userAccessKey.setCdUserId(user.getCdUserId());
            userAccessKey.setCdTenantId(user.getCdTenantId());
            userAccessKey.setAccountId(accountId);

            userAccessKeyRepository.saveAndFlush(userAccessKey);
        }

        return userAccessKey;
    }

    @Override
    public Page<UserAccessKey> listUserAccessKey(long offset, long limit, UserAccessKey matcher) {
        return pageQuery(offset, limit, matcher, userAccessKeyRepository);
    }

    @Override
    public void deleteUserAccessKey(String tenantId, String userId, String accessKey) {
        UserAccessKey matcher = new UserAccessKey();
        matcher.setAccountId(tenantId);
        matcher.setUserId(userId);
        matcher.setAccessKeyId(accessKey);
        Page<UserAccessKey> page = listUserAccessKey(0L, 100L, matcher);
        if (!page.isEmpty()) {
            page.getContent().forEach(userAccessKey -> delUserAccessKey(userAccessKey));
        }
    }

    private void delUserAccessKey(UserAccessKey userAccessKey) {
        String id = userAccessKey.getId();
        String userName = userAccessKey.getUserName();
        String userAk = userAccessKey.getAccessKeyId();

        AccountAccessKey accountAccessKey = getAccountAccessKeyByAccountId(userAccessKey.getAccountId());
        String accountAk = accountAccessKey.getAccessKeyId();
        String accountSk = accountAccessKey.getSecretAccessKey();

        Map<String, String> businessParams = new HashMap<>();
        businessParams.put("UserName", userName);
        businessParams.put("AccessKeyId", userAk);
        Result<String> result = HwClientUtil.executeGet("DeleteAccessKey", businessParams, endpoint, accountAk,
            accountSk);
        if (result.is2xxSuccess()) {
            userAccessKeyRepository.deleteById(id);
        }
    }

    @Override
    public UserAccessKey updateUserAccessKey(String tenantId, String userId, String accessKey, boolean active) {
        UserAccessKey matcher = new UserAccessKey();
        matcher.setAccountId(tenantId);
        matcher.setUserId(userId);
        matcher.setAccessKeyId(accessKey);
        Page<UserAccessKey> page = listUserAccessKey(0L, 1L, matcher);
        UserAccessKey reUserAccessKey = null;
        if (!page.isEmpty()) {
            UserAccessKey userAccessKey = page.getContent().get(0);
            String id = userAccessKey.getId();
            String userName = userAccessKey.getUserName();
            String userAk = userAccessKey.getAccessKeyId();

            AccountAccessKey accountAccessKey = getAccountAccessKeyByAccountId(tenantId);
            String accountAk = accountAccessKey.getAccessKeyId();
            String accountSk = accountAccessKey.getSecretAccessKey();

            Map<String, String> businessParams = new HashMap<>();
            businessParams.put("UserName", userName);
            businessParams.put("AccessKeyId", userAk);
            String status = active ? "Active" : "Inactive";
            businessParams.put("Status", status);
            Result<String> result = HwClientUtil.executeGet("UpdateAccessKey", businessParams, endpoint, accountAk,
                accountSk);
            if (result.is2xxSuccess()) {
                userAccessKey.setStatus(status);
                userAccessKeyRepository.saveAndFlush(userAccessKey);
                reUserAccessKey = userAccessKey;
            }
        }
        return reUserAccessKey;
    }

    @Override
    public List<BucketBean> listBucket(String tenantId, long offset, long limit) {
        // 底层全量查询无需进行分页查询
        List<BucketBeanInfo> list = listBucketInfoFromStorage(tenantId, false);
        if (!list.isEmpty()) {
            return list.stream().map(bucketBeanInfo -> {
                BucketBean bucketBean = new BucketBean();
                bucketBean.setName(bucketBeanInfo.getName());
                bucketBean.setCreationDate(bucketBeanInfo.getCreationDate());
                bucketBean.setOwnerId(bucketBeanInfo.getOwnerId());
                return bucketBean;
            }).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    @Override
    public boolean checkStatus() {
        Result<String> result = HwClientUtil.executeAdminGet("GetSummary", null, endpoint, adminAccessKey,
            adminSecretKey);
        if (result.is2xxSuccess()) {
            return true;
        }
        return false;
    }

    public List<BucketBeanInfo> listBucketInfo(String tenantId, long offset, long limit) {
        return listBucketInfoFromStorage(tenantId, true);
    }

    private List<BucketBeanInfo> listBucketInfoFromStorage(String tenantId, boolean queryCapacity) {
        // 获取帐户的ak、sk
        AccountAccessKey accountAccessKey = this.getAccountAccessKeyByAccountId(tenantId);
        if (accountAccessKey != null) {
            Result<String> result = HwObsServiceUtil.listAllMyBuckets(endpointS3, accountAccessKey.getAccessKeyId(),
                accountAccessKey.getSecretAccessKey());
            if (result.is2xxSuccess()) {
                JsonObject bucketsResult = gson.fromJson(result.getData(), JsonObject.class)
                    .getAsJsonObject("ListAllMyBucketsResult");
                JsonObject ownerObj = bucketsResult.getAsJsonObject("Owner");
                String ownerId = ownerObj.get("ID").getAsString();
                JsonElement bucketsEle = bucketsResult.get("Buckets");
                if (!bucketsEle.isJsonPrimitive()) {
                    List<BucketBeanInfo> list = new ArrayList<>();
                    JsonElement bucketDataEle = bucketsEle.getAsJsonObject().get("Bucket");
                    if (bucketDataEle.isJsonObject()) {
                        // 只有一个桶
                        list.add(
                            buildBucketBeanInfo(tenantId, bucketDataEle.getAsJsonObject(), ownerId, queryCapacity));
                    } else {
                        // 桶列表
                        bucketDataEle.getAsJsonArray()
                            .forEach(obj -> list.add(
                                buildBucketBeanInfo(tenantId, (JsonObject) obj, ownerId, queryCapacity)));
                    }
                    return list;
                }
            }
        }
        return Collections.emptyList();
    }

    private BucketBeanInfo buildBucketBeanInfo(String tenantId, JsonObject bucketObj, String ownerId,
        boolean queryCapacity) {
        BucketBeanInfo bucketBean = new BucketBeanInfo();
        bucketBean.setName(bucketObj.get("Name").getAsString());
        bucketBean.setCreationDate(bucketObj.get("CreationDate").getAsString());
        bucketBean.setOwnerId(ownerId);

        if (queryCapacity) {
            //获取桶的使用量
            AccountAccessKey accountAccessKey = this.getAccountAccessKeyByAccountId(tenantId);
            if (accountAccessKey != null) {
                Result<String> result = HwObsServiceUtil.getBucketStorageInfo(endpointS3,
                    accountAccessKey.getAccessKeyId(), accountAccessKey.getSecretAccessKey(),
                    bucketObj.get("Name").getAsString());
                if (result.is2xxSuccess()) {
                    JsonObject bucketsResult = gson.fromJson(result.getData(), JsonObject.class)
                        .getAsJsonObject("GetBucketStorageInfoResult");
                    Long size = bucketsResult.get("Size").getAsLong();
                    Long objectnumber = bucketsResult.get("ObjectNumber").getAsLong();
                    bucketBean.setSize(size);
                    bucketBean.setObjectNumber(objectnumber);
                }
            }
        }

        return bucketBean;
    }

    public Page pageQuery(long offset, long limit, Object matcher, JpaRepository repository) {
        int pageNo = (int) (offset / limit);
        PageRequest pageRequest = PageRequest.of(pageNo, (int) limit);
        Example<Object> example = null;
        if (matcher != null) {
            JsonObject queryObj = new JsonObject();
            ExampleMatcher matching = null;
            Field[] fields = matcher.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    Object val = field.get(matcher);
                    if (val != null) {
                        if (matching == null) {
                            matching = ExampleMatcher.matching();
                        }
                        matching = matching.withMatcher(field.getName(),
                            ExampleMatcher.GenericPropertyMatchers.contains());
                        if (val instanceof Boolean) {
                            queryObj.addProperty(field.getName(), (boolean) val);
                        } else if (val instanceof String) {
                            queryObj.addProperty(field.getName(), (String) val);
                        } else if (val instanceof Number) {
                            queryObj.addProperty(field.getName(), (Number) val);
                        } else {
                            queryObj.addProperty(field.getName(), String.valueOf(val));
                        }
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            if (matching != null) {
                Object query = gson.fromJson(queryObj.toString(), matcher.getClass());
                example = Example.of(query, matching);
            }
        }

        Page<Object> page;
        if (example != null) {
            page = repository.findAll(example, pageRequest);
        } else {
            page = repository.findAll(pageRequest);
        }
        return page;
    }

    private AccountAccessKey getAccountAccessKeyByAccountId(String accountId) {
        Optional<Account> accountOptional = this.getAccount(accountId);
        return accessKeyRepository.findById(accountOptional.get().getAccountId()).get();
    }
}
