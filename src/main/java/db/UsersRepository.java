package db;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;

import java.util.ArrayList;
import java.util.List;

public class UsersRepository {

    private final CqlSession session;

    public UsersRepository(CqlSession session) {
        this.session = session;
    }

    public List<User> selectAllUsers(String keyspace) {
        ResultSet resultSet = session.execute("select * from server.userd");
        List<User> result = new ArrayList<>();

        resultSet.forEach(x -> result.add(new User(
                x.getString("username"),
                x.getString("realm"),
                x.getString("HA1")
        )));

        return result;
    }

    public User getUser(String username, String realm) {
        PreparedStatement ps = session.prepare("select * from server.userd where username= ? and realm = ?");
        ResultSet rs = session.execute(ps.bind(username, realm));

        for (Row row : rs) {
            return new User(
                    row.getString("username"),
                    row.getString("realm"),
                    row.getString("ha1"));
        }
        return null;
    }
}
