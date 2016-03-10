package no.cantara.cs;

import no.cantara.cs.util.Configuration;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.Server;
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
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private int webappPort;
    private Server server;
    //private String resourceBase;


    public Main(Integer webappPort) {
        this.webappPort = webappPort;
        //log.info("Starting Jetty on port {}", webappPort);
        this.server = new Server(this.webappPort);

        //URL url = ClassLoader.getSystemResource("WEB-INF/web.xml");
        //this.resourceBase = url.toExternalForm().replace("WEB-INF/web.xml", "");
    }



    public static void main(String[] args) {
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        LogManager.getLogManager().getLogger("").setLevel(Level.INFO);

        log.debug("Starting ConfigService");
        Integer webappPort = Configuration.getInt("service.port");

        try {

            final Main main = new Main(webappPort);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    log.debug("ShutdownHook triggered. Exiting application");
                    main.stop();
                }
            });

            main.start();

            /*
            try {
                // wait forever...
                Thread.currentThread().join();
            } catch (InterruptedException ie) {
                log.warn("Thread was interrupted.", ie);
            }
            */
            log.debug("Finished waiting for Thread.currentThread().join()");
            main.stop();
        } catch (RuntimeException e) {
            log.error("Error during startup. Shutting down UserIdentityBackend.", e);
            System.exit(1);
        }
    }

    // https://github.com/psamsotha/jersey-spring-jetty/blob/master/src/main/java/com/underdog/jersey/spring/jetty/JettyServerMain.java
    public void start()  {
        //WebAppContext context = new WebAppContext();
        ServletContextHandler context = new ServletContextHandler();
        //log.debug("Start Jetty using resourcebase={}", resourceBase);
        //webAppContext.setDescriptor(resourceBase + "/WEB-INF/web.xml");
        //webAppContext.setResourceBase(resourceBase);
        context.setContextPath(CONTEXT_PATH);
        //webAppContext.setParentLoaderPriority(true);


        ConstraintSecurityHandler securityHandler = buildSecurityHandler();
        context.setSecurityHandler(securityHandler);

        ResourceConfig jerseyResourceConfig = new ResourceConfig();
        jerseyResourceConfig.packages("no.cantara.cs");
        ServletHolder jerseyServlet = new ServletHolder(new ServletContainer(jerseyResourceConfig));
        context.addServlet(jerseyServlet, "/*");

        context.addEventListener(new ContextLoaderListener());
        //context.addEventListener(new RequestContextListener());

        //context.setInitParameter("contextClass", AnnotationConfigWebApplicationContext.class.getName());
        context.setInitParameter("contextConfigLocation", "classpath:context.xml");


        server.setHandler(context);
        /*
        HandlerList handlers = new HandlerList();
        Handler[] handlerList = {webAppContext, new DefaultHandler()};
        handlers.setHandlers(handlerList);
        server.setHandler(handlers);
        */


        try {
            server.start();
        } catch (Exception e) {
            log.error("Error during Jetty startup. Exiting", e);
            System.exit(2);
        }
        int localPort = getPort();
        log.info("ConfigService started on http://localhost:{}{}", localPort, CONTEXT_PATH);
        try {
            server.join();
        } catch (InterruptedException e) {
            log.error("Jetty server thread when join. Pretend everything is OK.", e);
        }
    }

    private ConstraintSecurityHandler buildSecurityHandler() {
        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(new String[]{"user"});
        constraint.setAuthenticate(true);
        ConstraintMapping constraintMapping = new ConstraintMapping();
        constraintMapping.setConstraint(constraint);
        constraintMapping.setPathSpec("/*");
        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        securityHandler.addConstraintMapping(constraintMapping);
        HashLoginService loginService = new HashLoginService("ConfigService");
        String userName = Configuration.getString("login.user");
        String password = Configuration.getString("login.password");
        log.debug("Main instantiated with basic auth user={}", userName);
        loginService.putUser(userName, new Password(password), new String[]{"user"});
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
        //        return ((ServerConnector) server.getConnectors()[0]).getLocalPort();
    }

    public boolean isStarted() {
        return server.isStarted();
    }
}