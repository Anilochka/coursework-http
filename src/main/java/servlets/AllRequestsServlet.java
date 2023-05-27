package servlets;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;

import db.User;
import main.Main;
import templater.PageGenerator;

import org.apache.commons.codec.digest.DigestUtils;

public class AllRequestsServlet extends HttpServlet {
    private final String realm = "example.com";
    private final String privateKey = "ei4hj3k7nk2o3pb0pfofk1n78rzxftg";
    Map<String, String> nonces = new ConcurrentSkipListMap<>();
    Map<String, TryingCount> attemptsCount = new ConcurrentSkipListMap<>();
    private static final int MAX_ATTEMPTS_COUNT = 3;

    public AllRequestsServlet() {
    }

    protected void authenticate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");

        Enumeration<String> headers = request.getHeaderNames();
        headers.asIterator().forEachRemaining(h -> System.out.println(h + ": " + request.getHeader(h)));
        System.out.println("--------------------------------------");
        String clientIp = request.getRemoteAddr();

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            nonces.put(clientIp, calculateNonce(clientIp));
            response.addHeader("WWW-Authenticate", getAuthenticateHeader(nonces.get(clientIp)));
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } else if (!authHeader.startsWith("Digest")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, " This Servlet only supports Digest Authorization");
            return;
        }

        HashMap<String, String> headerValues = parseHeader(authHeader);
        String username = headerValues.get("username");
        String realm1 = headerValues.get("realm");
        if (!attemptsCount.containsKey(clientIp) || !Objects.equals(attemptsCount.get(clientIp).username, username)) {
            attemptsCount.put(clientIp, new TryingCount(username, 0));
        } else {
            int count = attemptsCount.get(clientIp).tryingCount + 1;
            attemptsCount.put(clientIp, new TryingCount(username, count));
        }
        if (username == null || username.isEmpty() || realm1 == null || realm1.isEmpty()) {
            refreshNonceAndCheckAttemptsCount(response, clientIp);
            return;
        }
        User user = Main.usersRepository.getUser(username, realm1);
        if (user == null) {
            refreshNonceAndCheckAttemptsCount(response, clientIp);
            return;
        }
        String ha1 = user.getHA1();

        String method = request.getMethod();
        String reqURI = headerValues.get("uri");
        String ha2 = DigestUtils.md5Hex(method + ":" + reqURI);
        String nonce = nonces.get(clientIp);
        nonces.remove(clientIp);

        String serverResponse = DigestUtils.md5Hex(ha1 + ":" + nonce + ":" + ha2);
        String clientResponse = headerValues.get("response");

        if (!serverResponse.equals(clientResponse)) {
            nonces.put(clientIp, calculateNonce(clientIp));
            int count = attemptsCount.get(clientIp).tryingCount;
            if (count < MAX_ATTEMPTS_COUNT) {
                response.addHeader("WWW-Authenticate", getAuthenticateHeader(nonces.get(clientIp)));
            }
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private void refreshNonceAndCheckAttemptsCount(HttpServletResponse response, String clientIp) throws IOException {
        nonces.put(clientIp, calculateNonce(clientIp));
        int count = attemptsCount.get(clientIp).tryingCount;
        if (count < MAX_ATTEMPTS_COUNT) {
            response.addHeader("WWW-Authenticate", getAuthenticateHeader(nonces.get(clientIp)));
        }
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private HashMap<String, String> parseHeader(String headerString) {
        String headerStringWithoutScheme = headerString.substring(headerString.indexOf(" ") + 1).trim();
        HashMap<String, String> values = new HashMap<>();
        String[] keyValueArray = headerStringWithoutScheme.split(",");
        for (String keyval : keyValueArray) {
            if (keyval.contains("=")) {
                String key = keyval.substring(0, keyval.indexOf("="));
                String value = keyval.substring(keyval.indexOf("=") + 1);
                values.put(key.trim(), value.replaceAll("\"", "").trim());
            }
        }
        return values;
    }

    private String getAuthenticateHeader(String nonce) {
        return "Digest realm=\"" + realm + "\","
                + "nonce=\"" + nonce + "\","
                + "opaque=\"" + getOpaque(realm, nonce) + "\"";
    }

    private String calculateNonce(String clientIp) {
        Date d = new Date();
        SimpleDateFormat f = new SimpleDateFormat("yyyy:MM:dd:hh:mm:ss");
        String timestamp = f.format(d);
        return DigestUtils.md5Hex(clientIp + ":" + timestamp + ":" + privateKey);
    }

    private String getOpaque(String domain, String nonce) {
        return DigestUtils.md5Hex(domain + nonce);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        authenticate(request, response);
        if (response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            return;
        }
        Map<String, Object> pageVariables = createPageVariablesMap(request);
        pageVariables.put("message", "");

        response.getWriter().println(PageGenerator.instance().getPage("page.html", pageVariables));

        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        authenticate(request, response);
        if (response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            return;
        }
        Map<String, Object> pageVariables = createPageVariablesMap(request);

        String message = request.getParameter("message");
        response.setContentType("text/html;charset=utf-8");

        if (message == null || message.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
        }
        pageVariables.put("message", message == null ? "" : message);

        response.getWriter().println(PageGenerator.instance().getPage("page.html", pageVariables));
    }

    private static Map<String, Object> createPageVariablesMap(HttpServletRequest request) {
        Map<String, Object> pageVariables = new HashMap<>();
        pageVariables.put("method", request.getMethod());
        pageVariables.put("URL", request.getRequestURL().toString());
        pageVariables.put("pathInfo", request.getPathInfo());
        pageVariables.put("sessionId", request.getSession().getId());
        pageVariables.put("parameters", request.getParameterMap().toString());
        return pageVariables;
    }

    private class TryingCount {
        String username;
        int tryingCount;

        public TryingCount(String username, int tryingCount) {
            this.username = username;
            this.tryingCount = tryingCount;
        }
    }
}
