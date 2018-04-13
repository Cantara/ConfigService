package no.cantara.cs;

import no.cantara.cs.config.ConstrettoConfig;
import no.cantara.cs.config.SpringConfigMapDb;
import no.cantara.cs.config.SpringConfigPostgres;
import no.cantara.cs.health.HealthResource;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.util.logging.Level;
import java.util.logging.LogManager;

import static java.util.Optional.ofNullable;

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
    private String persistenceType;
    private Server server;


    public Main(String mapDbPath, String persistenceType) {
        this.mapDbPath = mapDbPath;
        this.persistenceType = persistenceType;
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

        Integer webappPort = ConstrettoConfig.getInt("service.port");
        String mapDbPath = ConstrettoConfig.getString("mapdb.path");
        String persistenceType = ConstrettoConfig.getString("persistence.type");

        try {

            final Main main = new Main(mapDbPath, persistenceType).withPort(webappPort);

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
        context.setInitParameter("contextClass", AnnotationConfigWebApplicationContext.class.getName());

        context.setInitParameter("mapdb.path", ofNullable(mapDbPath).orElseGet(() -> ConstrettoConfig.getString("mapdb.path")));

        String pType = ofNullable(persistenceType).orElseGet(() -> ConstrettoConfig.getString("persistence.type"));

        switch (pType) {
            case "postgres":
                log.info("Using PostgreSQL as persistence store.");
                context.setInitParameter("contextConfigLocation", SpringConfigPostgres.class.getName());
                break;
            case "mapdb":
                log.warn("Using MapDB as persistence store. This form of store is deprecated. PostgreSQL should be used instead.");
                context.setInitParameter("contextConfigLocation", SpringConfigMapDb.class.getName());
                break;
            default:
                log.warn("{} is not a valid persistence type. Falling back to using MapDB as persistence store. " +
                        "In addition - this form of store is deprecated. Please use PostgreSQL instead.", pType);
                context.setInitParameter("contextConfigLocation", SpringConfigMapDb.class.getName());
                break;
        }

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
        log.info("ConfigService started on http://localhost:{}{}{} with {} as persistence.",
                webappPort, CONTEXT_PATH, HealthResource.HEALTH_PATH, pType);
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
        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();

        // health - no authentication
        ConstraintMapping healthEndpointConstraintMapping = new ConstraintMapping();
        healthEndpointConstraintMapping.setConstraint(new Constraint(Constraint.NONE, Constraint.ANY_ROLE));
        healthEndpointConstraintMapping.setPathSpec(HealthResource.HEALTH_PATH);
        securityHandler.addConstraintMapping(healthEndpointConstraintMapping);

        //Note specific isAdmin checks in ClientResource for administration APIs for client resource
        securityHandler.addConstraintMapping(buildConstraintMapping("/client/*", new String[]{USER_ROLE, ADMIN_ROLE}));

        //all other paths require admin authentication
        securityHandler.addConstraintMapping(buildConstraintMapping("/*", new String[]{ADMIN_ROLE}));

        HashLoginService loginService = new HashLoginService("ConfigService");

        String clientUsername = ConstrettoConfig.getString("login.user");
        String clientPassword = ConstrettoConfig.getString("login.password");
        UserStore userStore = new UserStore();
        userStore.addUser(clientUsername, Credential.getCredential(clientPassword), new String[]{USER_ROLE});

        String adminUsername = ConstrettoConfig.getString("login.admin.user");
        String adminPassword = ConstrettoConfig.getString("login.admin.password");
        userStore.addUser(clientUsername, Credential.getCredential(adminPassword), new String[]{ADMIN_ROLE});
        loginService.setUserStore(userStore);

        securityHandler.setLoginService(loginService);

        log.debug("Main instantiated with basic auth clientuser={} and adminuser={}", clientUsername, adminUsername);
        return securityHandler;
    }
    private ConstraintMapping buildConstraintMapping(String pathSpec, String[] roles) {
        ConstraintMapping constraintMapping = new ConstraintMapping();
        constraintMapping.setPathSpec(pathSpec);
        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(roles);
        constraint.setAuthenticate(true);
        constraintMapping.setConstraint(constraint);
        return constraintMapping;
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