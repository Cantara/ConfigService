package no.cantara.cs;

import no.cantara.cs.health.HealthResource;
import no.cantara.cs.util.Configuration;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.web.context.ContextLoaderListener;

import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09
 */
public class Main {
    public static final String CONTEXT_PATH = "/jau";
    public static final String ADMIN_ROLE = "admin";
    public static final String USER_ROLE = "user";

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private Integer webappPort;
    private String mapDbPath;
    private Server server;


    public Main(String mapDbPath) {
        this.mapDbPath = mapDbPath;
        this.server = new Server();
    }

    public Main withPort(Integer webappPort) {
        this.webappPort = webappPort;
        return this;
    }

    public static void main(String[] args) {
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        LogManager.getLogManager().getLogger("").setLevel(Level.INFO);

        Integer webappPort = Configuration.getInt("service.port");
        String mapDbPath = Configuration.getString("mapdb.path");

        try {

            final Main main = new Main(mapDbPath).withPort(webappPort);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    log.debug("ShutdownHook triggered. Exiting application");
                    main.stop();
                }
            });

            main.start();
            log.debug("Finished waiting for Thread.currentThread().join()");
            main.stop();
        } catch (RuntimeException e) {
            log.error("Error during startup. Shutting down ConfigService.", e);
            System.exit(1);
        }
    }

    // https://github.com/psamsotha/jersey-spring-jetty/blob/master/src/main/java/com/underdog/jersey/spring/jetty/JettyServerMain.java
    public void start() {
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath(CONTEXT_PATH);


        ConstraintSecurityHandler securityHandler = buildSecurityHandler();
        context.setSecurityHandler(securityHandler);

        ResourceConfig jerseyResourceConfig = new ResourceConfig();
        jerseyResourceConfig.packages("no.cantara.cs");
        ServletHolder jerseyServlet = new ServletHolder(new ServletContainer(jerseyResourceConfig));
        context.addServlet(jerseyServlet, "/*");

        context.addEventListener(new ContextLoaderListener());

        context.setInitParameter("contextConfigLocation", "classpath:context.xml");
        context.setInitParameter("mapdb.path", mapDbPath);

        ServerConnector connector = new ServerConnector(server);
        if (webappPort != null) {
            connector.setPort(webappPort);
        }
        NCSARequestLog requestLog = buildRequestLog();
        server.setRequestLog(requestLog);
        server.addConnector(connector);
        server.setHandler(context);

        try {
            server.start();
        } catch (Exception e) {
            log.error("Error during Jetty startup. Exiting", e);
            System.exit(2);
        }
        webappPort = connector.getLocalPort();
        log.info("ConfigService started on http://localhost:{}{}, mapdb.path: {}", webappPort, CONTEXT_PATH, mapDbPath);
        try {
            server.join();
        } catch (InterruptedException e) {
            log.error("Jetty server thread when join. Pretend everything is OK.", e);
        }
    }

    private NCSARequestLog buildRequestLog() {
        NCSARequestLog requestLog = new NCSARequestLog("logs/jetty-yyyy_mm_dd.request.log");
        requestLog.setAppend(true);
        requestLog.setExtended(true);
        requestLog.setLogTimeZone("GMT");

        return requestLog;
    }

    private ConstraintSecurityHandler buildSecurityHandler() {
        Constraint userRoleConstraint = new Constraint();
        userRoleConstraint.setName(Constraint.__BASIC_AUTH);
        userRoleConstraint.setRoles(new String[]{USER_ROLE, ADMIN_ROLE});
        userRoleConstraint.setAuthenticate(true);

        Constraint adminRoleConstraint = new Constraint();
        adminRoleConstraint.setName(Constraint.__BASIC_AUTH);
        adminRoleConstraint.setRoles(new String[]{ADMIN_ROLE});
        adminRoleConstraint.setAuthenticate(true);

        ConstraintMapping clientConstraintMapping = new ConstraintMapping();
        clientConstraintMapping.setConstraint(userRoleConstraint);
        clientConstraintMapping.setPathSpec("/client/*");

        ConstraintMapping adminRoleConstraintMapping = new ConstraintMapping();
        adminRoleConstraintMapping.setConstraint(adminRoleConstraint);
        adminRoleConstraintMapping.setPathSpec("/*");

        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        securityHandler.addConstraintMapping(clientConstraintMapping);
        securityHandler.addConstraintMapping(adminRoleConstraintMapping);

        // Allow healthresource to be accessed without authentication
        ConstraintMapping healthEndpointConstraintMapping = new ConstraintMapping();
        healthEndpointConstraintMapping.setConstraint(new Constraint(Constraint.NONE, Constraint.ANY_ROLE));
        healthEndpointConstraintMapping.setPathSpec(HealthResource.HEALTH_PATH);
        securityHandler.addConstraintMapping(healthEndpointConstraintMapping);

        HashLoginService loginService = new HashLoginService("ConfigService");

        String clientUsername = Configuration.getString("login.user");
        String clientPassword = Configuration.getString("login.password");
        loginService.putUser(clientUsername, new Password(clientPassword), new String[]{USER_ROLE});

        String adminUsername = Configuration.getString("login.admin.user");
        String adminPassword = Configuration.getString("login.admin.password");
        loginService.putUser(adminUsername, new Password(adminPassword), new String[]{ADMIN_ROLE});

        log.debug("Main instantiated with basic auth clientuser={} and adminuser={}", clientUsername, adminUsername);
        securityHandler.setLoginService(loginService);
        return securityHandler;
    }


    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            log.warn("Error when stopping Jetty server", e);
        }
    }

    public int getPort() {
        return webappPort;
    }

    public boolean isStarted() {
        return server.isStarted();
    }
}