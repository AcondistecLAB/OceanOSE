package com.vmware.osis.huawei.repository;

import com.vmware.osis.huawei.model.AccountAccessKey;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Administrator
 * @ClassName AccessKeyRepository
 * @Description TODO
 **/
@Repository
public interface AccessKeyRepository extends JpaRepository<AccountAccessKey, String> {
    
}
