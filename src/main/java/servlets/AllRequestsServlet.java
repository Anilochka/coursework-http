package servlets;

import templater.PageGenerator;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.codec.digest.DigestUtils;

public class AllRequestsServlet extends HttpServlet {
    private final String authMethod = "auth";
    private final String userName = "usm";
    private final String password = "password";
    private final String realm = "example.com";

    public String nonce;
    public ScheduledExecutorService nonceRefreshExecutor = Executors.newScheduledThreadPool(1);

    public AllRequestsServlet() {
        nonce = calculateNonce();
        nonceRefreshExecutor.scheduleAtFixedRate(() -> {
            log("Refreshing Nonce....");
            nonce = calculateNonce();
        }, 1, 1, TimeUnit.MINUTES);
    }

    protected void authenticate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String requestBody = readRequestBody(request);

        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.isBlank()) {
                response.addHeader("WWW-Authenticate", getAuthenticateHeader());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                if (authHeader.startsWith("Digest")) {
                    // parse the values of the Authentication header into a hashmap
                    HashMap<String, String> headerValues = parseHeader(authHeader);
                    String method = request.getMethod();
                    String ha1 = DigestUtils.md5Hex(userName + ":" + realm + ":" + password);
                    String qop = headerValues.get("qop");
                    String ha2;
                    String reqURI = headerValues.get("uri");

                    if (qop != null && !qop.isBlank() && qop.equals("auth-int")) {
                        String entityBodyMd5 = DigestUtils.md5Hex(requestBody);
                        ha2 = DigestUtils.md5Hex(method + ":" + reqURI + ":" + entityBodyMd5);
                    } else {
                        ha2 = DigestUtils.md5Hex(method + ":" + reqURI);
                    }

                    String serverResponse;

                    if (qop == null || qop.isBlank()) {
                        serverResponse = DigestUtils.md5Hex(ha1 + ":" + nonce + ":" + ha2);

                    } else {
                        String domain = headerValues.get("realm");

                        String nonceCount = headerValues.get("nc");
                        String clientNonce = headerValues.get("cnonce");

                        serverResponse = DigestUtils.md5Hex(ha1 + ":" + nonce + ":"
                                + nonceCount + ":" + clientNonce + ":" + qop + ":" + ha2);

                    }
                    String clientResponse = headerValues.get("response");

                    if (!serverResponse.equals(clientResponse)) {
                        response.addHeader("WWW-Authenticate", getAuthenticateHeader());
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    }
                } else {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, " This Servlet only supports Digest Authorization");
                }
            }
            out.println("<head>");
            out.println("<title>Servlet HttpDigestAuth</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet HttpDigestAuth at"  + request.getContextPath() + "</h1>");
            out.println("</body>");
            out.println("</html>");
        } finally {
            out.close();
        }
    }

    private HashMap<String, String> parseHeader(String headerString) {
        // seperte out the part of the string which tells you which Auth scheme is it
        String headerStringWithoutScheme = headerString.substring(headerString.indexOf(" ") + 1).trim();
        HashMap<String, String> values = new HashMap<String, String>();
        String keyValueArray[] = headerStringWithoutScheme.split(",");
        for (String keyval : keyValueArray) {
            if (keyval.contains("=")) {
                String key = keyval.substring(0, keyval.indexOf("="));
                String value = keyval.substring(keyval.indexOf("=") + 1);
                values.put(key.trim(), value.replaceAll("\"", "").trim());
            }
        }
        return values;
    }

    private String getAuthenticateHeader() {
        String header = "";

        header += "Digest realm=\"" + realm + "\",";
        if (!authMethod.isBlank()) {
            header += "qop=" + authMethod + ",";
        }
        header += "nonce=\"" + nonce + "\",";
        header += "opaque=\"" + getOpaque(realm, nonce) + "\"";

        return header;
    }

    /**
     * Calculate the nonce based on current time-stamp upto the second, and a
     * random seed
     *
     * @return
     */
    public String calculateNonce() {
        Date d = new Date();
        SimpleDateFormat f = new SimpleDateFormat("yyyy:MM:dd:hh:mm:ss");
        String fmtDate = f.format(d);
        Random rand = new Random(100000);
        Integer randomInt = rand.nextInt();
        return DigestUtils.md5Hex(fmtDate + randomInt.toString());
    }

    private String getOpaque(String domain, String nonce) {
        return DigestUtils.md5Hex(domain + nonce);
    }

    /**
     * Returns the request body as String
     *
     * @param request
     * @return
     * @throws IOException
     */
    private String readRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;
        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(
                        inputStream));
                char[] charBuffer = new char[128];
                int bytesRead = -1;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            } else {
                stringBuilder.append("");
            }
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                    throw ex;
                }
            }
        }
        String body = stringBuilder.toString();
        return body;
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        authenticate(request, response);
        if (response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            return;
        }
//        Map<String, Object> pageVariables = createPageVariablesMap(request);
//        pageVariables.put("message", "");
//
//        response.getWriter().println(PageGenerator.instance().getPage("page.html", pageVariables));
//
//        response.setContentType("text/html;charset=utf-8");
//        response.setStatus(HttpServletResponse.SC_OK);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        authenticate(request, response);
        if (response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            return;
        }
//        Map<String, Object> pageVariables = createPageVariablesMap(request);
//
//        String message = request.getParameter("message");
//        response.setContentType("text/html;charset=utf-8");
//
//        if (message == null || message.isEmpty()) {
//            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
//        } else {
//            response.setStatus(HttpServletResponse.SC_OK);
//        }
//        pageVariables.put("message", message == null ? "" : message);
//
//        response.getWriter().println(PageGenerator.instance().getPage("page.html", pageVariables));
    }

    private static Map<String, Object> createPageVariablesMap(HttpServletRequest request) {
        Map<String, Object> pageVariables = new HashMap<>();
        pageVariables.put("method", request.getMethod());
        pageVariables.put("URL", request.getRequestURL().toString());
        pageVariables.put("pathInfo", request.getPathInfo());
        //pageVariables.put("sessionId", request.getSession().getId());
        pageVariables.put("parameters", request.getParameterMap().toString());
        return pageVariables;
    }
}
