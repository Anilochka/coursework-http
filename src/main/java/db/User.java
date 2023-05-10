package db;

public class User {
    private String username;
    private String realm;
    private String HA1;
    public User(String username, String realm, String HA1) {
        super();
        this.username = username;
        this.realm = realm;
        this.HA1 = HA1;
    }
    public String getUsername() {
        return username;
    }
    public String getRealm() {
        return realm;
    }
    public String getHA1() {
        return HA1;
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", realm='" + realm + '\'' +
                ", HA1='" + HA1 + '\'' +
                '}';
    }
}
