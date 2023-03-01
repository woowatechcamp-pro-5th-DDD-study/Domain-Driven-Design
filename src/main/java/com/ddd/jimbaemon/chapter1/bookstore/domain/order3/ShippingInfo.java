package com.ddd.jimbaemon.chapter1.bookstore.domain.order3;

import java.util.Objects;

public class ShippingInfo {

    public Receiver receiver; //받는 사람
    public Address address; //주소

    public ShippingInfo(Receiver receiver,
        Address address) {
        this.receiver = receiver;
        this.address = address;
    }

    //두 밸류 객체를 비교할 때는 모든 속성이 같은지 비교한다.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ShippingInfo that = (ShippingInfo) o;
        return Objects.equals(receiver, that.receiver) && Objects
            .equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(receiver, address);
    }

    public class Receiver {

        private String name;
        private String phoneNumber;

        public Receiver(String name, String phoneNumber) {
            this.name = name;
            this.phoneNumber = phoneNumber;
        }

        public String getName() {
            return name;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Receiver receiver = (Receiver) o;
            return Objects.equals(name, receiver.name) && Objects
                .equals(phoneNumber, receiver.phoneNumber);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, phoneNumber);
        }
    }

    public class Address {

        private String address1;
        private String address2;
        private String zipcode;

        public Address(String address1, String address2, String zipcode) {
            this.address1 = address1;
            this.address2 = address2;
            this.zipcode = zipcode;
        }

        public String getAddress1() {
            return address1;
        }

        public String getAddress2() {
            return address2;
        }

        public String getZipcode() {
            return zipcode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Address address = (Address) o;
            return Objects.equals(address1, address.address1) && Objects
                .equals(address2, address.address2) && Objects.equals(zipcode, address.zipcode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(address1, address2, zipcode);
        }
    }
}
