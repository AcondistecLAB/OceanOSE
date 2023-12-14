package com.vmware.osis.huawei.model;

import com.google.gson.annotations.SerializedName;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Administrator
 * @ClassName AccessKey
 * @Description 证书
 **/
@Entity
@Table(name = "account_access_key")
public class AccountAccessKey {
    @Id
    @Column(name = "account_id")
    @SerializedName(value = "AccountId")
    private String accountId;

    @Column(name = "access_key_id")
    @SerializedName(value = "AccessKeyId")
    private String accessKeyId;

    @Column(name = "secret_access_key")
    @SerializedName(value = "SecretAccessKey")
    private String secretAccessKey;

    @Column(name = "status")
    @SerializedName(value = "Status")
    private String status;

    @Column(name = "create_date")
    @SerializedName(value = "CreateDate")
    private String createDate;

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
