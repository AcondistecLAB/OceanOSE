package com.vmware.osis.huawei;

import com.vmware.osis.huawei.model.*;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface HwAdmin {
    Account createAccount(Account account);

    Optional<Account> getAccount(String id);

    AccountUser createUser(AccountUser accountUser);

    Optional<AccountUser> getUser(String userId);

    Optional<AccountUser> getUserWithCanonicalUserId(String canonicalUserId);

    void deleteUser(String accountId, String userId);

    AccountUser updateUserStatus(String userId, boolean isActive);

    Page<AccountUser> listUser(long offset, long limit, AccountUser matchUser);

    Page<Account>listTenants(long offset, long limit, Account matchAccount);

    Account modAccount(String accountId, Map<String, String> paramMap);

    void deleteAccount(String accountId);

    UserAccessKey createUserAccessKey(String accountId, String userId);

    Page<UserAccessKey> listUserAccessKey(long offset, long limit, UserAccessKey matcher);

    void deleteUserAccessKey(String tenantId, String userId, String accessKey);

    UserAccessKey updateUserAccessKey(String tenantId, String userId, String accessKey, boolean active);

    List<BucketBean> listBucket(String tenantId, long offset, long limit);

    List<BucketBeanInfo> listBucketInfo(String tenantId, long offset, long limit);

    boolean checkStatus();
}
