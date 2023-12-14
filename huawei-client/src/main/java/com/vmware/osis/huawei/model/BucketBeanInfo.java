package com.vmware.osis.huawei.model;

public class BucketBeanInfo extends BucketBean{
    private long size;
    private long objectNumber;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getObjectNumber() {
        return objectNumber;
    }

    public void setObjectNumber(long objectNumber) {
        this.objectNumber = objectNumber;
    }
}
