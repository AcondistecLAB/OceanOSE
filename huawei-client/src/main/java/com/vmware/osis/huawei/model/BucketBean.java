package com.vmware.osis.huawei.model;

/**
 * @author Administrator
 * @ClassName BucketBean
 * @Description TODO
 **/
public class BucketBean {
    private String name;

    private String creationDate;

    private String ownerId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
}
