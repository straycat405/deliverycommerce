package com.babjo.deliverycommerce.user.entity;

public enum UserEnumRole {

    CUSTOMER(Authority.CUSTOMER),
    OWNER(Authority.OWNER),
    MANAGER(Authority.MANAGER),
    MASTER(Authority.MASTER);

    private final String authority;

    // 생성자
    UserEnumRole(String authority) {
        this.authority = authority;
    }

    // Getter
    public String getAuthority() {
        return authority;
    }

    public static class Authority {
        public static final String CUSTOMER = "ROLE_CUSTOMER";
        public static final String OWNER = "ROLE_OWNER";
        public static final String MANAGER = "ROLE_MANAGER";
        public static final String MASTER = "ROLE_MASTER";
    }
}
