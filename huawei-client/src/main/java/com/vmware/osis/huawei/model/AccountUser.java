package com.vmware.osis.huawei.model;

import com.google.gson.annotations.SerializedName;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Email;

/**
 * @author Administrator
 * @ClassName User
 * @Description 用户
 **/

@Entity
@Table(name = "account_user")
public class AccountUser {
    @Id
    @SerializedName(value = "UserId", alternate = "userId")
    @Column(name = "user_id")
    private String userId;

    @SerializedName(value = "Path")
    @Column(name = "path")
    private String path;

    @SerializedName(value = "UserName", alternate = "userName")
    @Column(name = "user_name")
    private String userName;

    @SerializedName(value = "Arn")
    @Column(name = "arn")
    private String arn;

    @SerializedName(value = "CreateDate")
    @Column(name = "create_date")
    private String createDate;

    @Column(name = "canonical_user_Id")
    private String canonicalUserId;

    @Column(name = "cd_tenant_id")
    private String cdTenantId;

    @Column(name = "cd_user_id")
    private String cdUserId;

    @Column(name = "account_id")
    private String accountId;

    @Column(name = "status")
    private Boolean status;

    @Column(name = "email")
    @Email
    private String email;

    @Column(name = "role")
    private String role;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public String getCanonicalUserId() {
        return canonicalUserId;
    }

    public void setCanonicalUserId(String canonicalUserId) {
        this.canonicalUserId = canonicalUserId;
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

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Boolean getStatus() {
        return status;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
