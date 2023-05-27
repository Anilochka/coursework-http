package db;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;

import java.util.ArrayList;
import java.util.List;

import utils.Utils;

public class UsersRepository {

    private final CqlSession session;

    public UsersRepository(CqlSession session) {
        this.session = session;
    }

    public List<User> selectAllUsers(String keyspace) {
        ResultSet resultSet = session.execute("select * from server.users");
        List<User> result = new ArrayList<>();

        resultSet.forEach(x -> result.add(new User(
                x.getString("username"),
                x.getString("realm"),
                x.getString("ha1")
        )));

        return result;
    }

    public User getUser(String username, String realm) {
        PreparedStatement ps = session.prepare("select * from server.users where username = ? and realm = ?");
        ResultSet rs = session.execute(ps.bind(username, realm));

        for (Row row : rs) {
            return new User(
                    row.getString("username"),
                    row.getString("realm"),
                    row.getString("ha1"));
        }
        return null;
    }

    public void createTable() {
        String query = "CREATE TABLE IF NOT EXISTS server.users (\n" +
                "    username       varchar,\n" +
                "    realm          varchar,\n" +
                "    ha1            varchar,\n" +
                "    PRIMARY KEY ((username), realm)\n" +
                ");";
        session.execute(query);
    }

    public void addUser(String username, String realm, String password) {
        PreparedStatement ps = session.prepare("insert into server.users (username, realm, ha1) values (?, ?, ?)");
        session.execute(ps.bind(username, realm, Utils.calculateHA1(username, realm, password)));
    }
}
