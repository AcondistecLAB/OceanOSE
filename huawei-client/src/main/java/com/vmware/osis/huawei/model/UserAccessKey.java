package com.vmware.osis.huawei.model;

import com.google.gson.annotations.SerializedName;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Administrator
 * @ClassName AccessKey
 * @Description 证书
 **/
@Entity
@Table(name = "user_access_key")
public class UserAccessKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private String id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "user_name")
    @SerializedName(value = "UserName", alternate = "userName")
    private String userName;

    @Column(name = "cd_tenant_id")
    private String cdTenantId;

    @Column(name = "cd_user_id")
    private String cdUserId;

    @Column(name = "account_id")
    private String accountId;

    @Column(name = "access_key_id")
    @SerializedName(value = "AccessKeyId", alternate = "accessKeyId")
    private String accessKeyId;

    @Column(name = "secret_access_key")
    @SerializedName(value = "SecretAccessKey")
    private String secretAccessKey;

    @Column(name = "status")
    @SerializedName(value = "Status", alternate = "status")
    private String status;

    @Column(name = "create_date")
    @SerializedName(value = "CreateDate")
    private String createDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getCdTenantId() {
        return cdTenantId;
    }

    public void setCdTenantId(String cdTenantId) {
        this.cdTenantId = cdTenantId;
    }

    public String getCdUserId() {
        return cdUserId;
    }

    public void setCdUserId(String cdUserId) {
        this.cdUserId = cdUserId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }
}
