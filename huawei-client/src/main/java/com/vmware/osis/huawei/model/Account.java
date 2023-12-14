package com.vmware.osis.huawei.model;

import com.google.gson.annotations.SerializedName;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author xiangyong.wang
 * @ClassName Account
 * @Description 帐户
 **/
@Entity
@Table(name = "account")
public class Account {
    @SerializedName(value = "AccountId", alternate = "accountId")
    @Id
    @Column(name = "account_id")
    private String accountId;

    @SerializedName(value = "AccountName")
    @Column(name = "account_name")
    private String accountName;

    @SerializedName(value = "CanonicalUserId")
    @Column(name = "canonical_user_Id")
    private String canonicalUserId;

    @SerializedName(value = "Status")
    @Column(name = "status")
    private String status;

    @SerializedName(value = "Email")
    @Column(name = "email")
    private String email;

    @SerializedName(value = "Arn")
    @Column(name = "arn")
    private String arn;

    @SerializedName(value = "CreateDate")
    @Column(name = "create_date")
    private String createDate;

    @SerializedName(value = "EncryptOption")
    @Column(name = "encrypt_option")
    private Integer encryptOption;

    @SerializedName(value = "KmsType")
    @Column(name = "kms_type")
    private Integer kmsType;

    @Column(name = "cd_tenant_id")
    private String cdTenantId;

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getCanonicalUserId() {
        return canonicalUserId;
    }

    public void setCanonicalUserId(String canonicalUserId) {
        this.canonicalUserId = canonicalUserId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getArn() {
        return arn;
    }

    public void setArn(String arn) {
        this.arn = arn;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public Integer getEncryptOption() {
        return encryptOption;
    }

    public void setEncryptOption(Integer encryptOption) {
        this.encryptOption = encryptOption;
    }

    public Integer getKmsType() {
        return kmsType;
    }

    public void setKmsType(Integer kmsType) {
        this.kmsType = kmsType;
    }

    public String getCdTenantId() {
        return cdTenantId;
    }

    public void setCdTenantId(String cdTenantId) {
        this.cdTenantId = cdTenantId;
    }
}
