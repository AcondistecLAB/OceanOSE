package com.vmware.osis.huawei.repository;

import com.vmware.osis.huawei.model.Account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author Administrator
 * @ClassName AccountRepository
 * @Description TODO
 **/
@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    Optional<Account> findAccountByCdTenantId(String cdTenantId);
}
