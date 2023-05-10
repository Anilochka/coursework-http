package main;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.auth.AuthProvider;
import com.datastax.oss.driver.internal.core.auth.ProgrammaticPlainTextAuthProvider;

import db.UsersRepository;
import servlets.AllRequestsServlet;

public class Main {
    public static CqlSession session;
    public static UsersRepository usersRepository;

    public static void main(String[] args) throws Exception {
        AllRequestsServlet allRequestsServlet = new AllRequestsServlet();

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.addServlet(new ServletHolder(allRequestsServlet), "/*");

        AuthProvider authProvider = new ProgrammaticPlainTextAuthProvider("cassandra", "cassandra");
        session =
                CqlSession.builder()
                        .withAuthProvider(authProvider)
                        .build();

        usersRepository = new UsersRepository(session);

        Server server = new Server(8092);
        server.setHandler(context);

        server.start();
        server.join();
    }
}
