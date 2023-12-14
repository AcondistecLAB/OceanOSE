package com.vmware.osis.huawei.repository;

import com.vmware.osis.huawei.model.UserAccessKey;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Administrator
 * @ClassName UserAccessKeyRepository
 * @Description TODO
 **/
@Repository
public interface UserAccessKeyRepository extends JpaRepository<UserAccessKey, String> {
    
}
