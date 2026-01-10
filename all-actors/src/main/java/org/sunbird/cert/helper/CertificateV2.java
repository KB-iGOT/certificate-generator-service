package org.sunbird.cert.helper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * this is a builder class to build a Certificate object needs to be inserted into ES
 * @author anmolgupta
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CertificateV2 {

    private String id;
    private Recipient recipient;
    private Map<String, Object> data;
    private Map<String, Object> related;
    private String createdBy;
    private String updatedBy;
    private boolean isRevoked;
    private String reason;
    private String accessCode;

    public CertificateV2(){}

    private CertificateV2(Builder builder) {
        this.id = builder.id;
        this.recipient = builder.recipient;
        this.data = builder.data;
        this.related = builder.related;
        this.createdBy = builder.createdBy;
        this.updatedBy = builder.updatedBy;
        this.isRevoked = builder.isRevoked;
        this.accessCode= builder.accessCode;
        this.reason=builder.reason;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public Recipient getRecipient() {
        return recipient;
    }

    public String getReason() {
        return reason;
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Map<String, Object> getRelated() {
        return related;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    @JsonProperty(value = "isRevoked")
    public boolean isRevoked() {
        return isRevoked;
    }

    @Override
    public String toString() {
        return "Certificate{" +
                "id='" + id + '\'' +
                ", recipient=" + recipient +
                ", data=" + data +
                ", related=" + related +
                ", createdBy='" + createdBy + '\'' +
                ", updatedBy='" + updatedBy + '\'' +
                ", isRevoked=" + isRevoked +
                ", reason='" + reason + '\'' +
                ", accessCode='" + accessCode + '\'' +
                '}';
    }

    public static class Builder {


        private String id;
        private Recipient recipient;
        private Map<String, Object> data;
        private Map<String, Object> related;
        private String createdBy;
        private String updatedBy;
        private boolean isRevoked;
        private String accessCode;
        private String reason;

        public Builder setReason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder setData(Map<String, Object> data) {
            this.data = data;
            return this;
        }

        public Builder setRecipient(Recipient recipient) {
            this.recipient = recipient;
            return this;
        }


        public Builder setAccessCode(String accessCode) {
            this.accessCode = accessCode;
            return this;
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setRelated(Map<String, Object> related) {
            this.related = related;
            return this;

        }

        public Builder setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder setUpdatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }


        public Builder setRevoked(boolean revoked) {
            isRevoked = revoked;
            return this;

        }
        public CertificateV2 build(){
            CertificateV2 certificate=new CertificateV2(this);
            return certificate;

        }
    }
}

