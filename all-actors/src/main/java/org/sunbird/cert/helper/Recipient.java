package org.sunbird.cert.helper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * this is a Related class will be used to for a Related object needs to be saved in ES.
 * @author anmolgupta
 * "name": "name of the student, teacher or institution",
 *     "email": "email of the student, teacher or institution, optional (encrypted)",
 *     "phone": "phone number of the student, teacher or institution, optional (encrypted)",
 *     "id": "user or org external identifier in Sunbird, optional. Could also be a URI preferably or UUID",
 *     "type" : "one of individual, entity"
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Recipient {

    private String name;
    private String email;
    private String phone;
    private String id;
    private String type;

    public Recipient() {
    }

    private Recipient(Builder builder) {
        this.name = builder.name;
        this.email = builder.email;
        this.phone = builder.phone;
        this.id = builder.id;
        this.type = builder.type;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public static class Builder{


        private String name;
        private String email;
        private String phone;
        private String id;
        private String type;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setEmail(String email) {
            this.email = email;
            return this;


        }

        public Builder setPhone(String phone) {
            this.phone = phone;
            return this;

        }

        public Builder setId(String id) {
            this.id = id;
            return this;

        }
        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public Recipient build(){
            Recipient recipient=new Recipient(this);
            return recipient;
        }
    }
}

