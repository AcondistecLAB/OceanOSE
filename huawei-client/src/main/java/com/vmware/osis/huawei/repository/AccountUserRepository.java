package com.vmware.osis.huawei.repository;

import com.vmware.osis.huawei.model.AccountUser;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Administrator
 * @ClassName AccountRepository
 * @Description TODO
 **/
@Repository
public interface AccountUserRepository extends JpaRepository<AccountUser, String> {
    AccountUser findByCanonicalUserId(String canonicalUserId);

    List<AccountUser> findAccountUsersByCanonicalUserId(String canonicalUserId);

    List<AccountUser> findAccountUsersByAccountId(String accountId);
}
