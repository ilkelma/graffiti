package graffiti

import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.server.session.HashSessionManager
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.ServletHolder

class Server {

    def jetty

    def webAppContext

    def dynamicServlets = [:]

    Server(config) {
        
        jetty = new org.eclipse.jetty.server.Server(config.port as Integer)
        webAppContext = new WebAppContext()

        // session setup
        def sessionManager = new HashSessionManager()
        webAppContext.sessionHandler = new SessionHandler(sessionManager)
        
        jetty.handler = webAppContext

        loadConfig(config)

        start()
    }

    void loadConfig(config) {

        webAppContext.extraClasspath = config.classpath.flatten().join(File.pathSeparator)
        webAppContext.welcomeFiles = config.welcomeFiles.flatten()

        setRoot(config.root)

        config.mappings.each { map it }

        webAppContext.configurations.each { println it.class }

//        config.datasources.each { name, value ->
////            register the datasource
//            def entry = new org.mortbay.jetty.plus.naming.Resource("jdbc/${name}", (javax.sql.DataSource)value)
//
//
//        }
    }

    void map(mapping) {

        def path = mapping.path

        // handle GET /path { doSomething() } mappings
        if( mapping.method && path && mapping.block ) {
            def servlet = dynamicServlets[path]
            if( !servlet ) {
                servlet = new GraffitiServlet()
                map(['path': path, 'servlet': servlet])
                dynamicServlets[path] = servlet
            }
            servlet[mapping.method] = mapping.block
            return
        }

        // handle /path -> SomeServlet class mappings
        if( mapping.path && mapping.servlet ) {

            def servlet = mapping.servlet
            if( servlet instanceof javax.servlet.Servlet ) {
                servlet = new ServletHolder(servlet)
                println mapping
                if( mapping.configBlock ) {
                    mapping.configBlock.delegate = servlet
                    mapping.configBlock()
                }
            }

            println 'adding servlet ' + servlet + ' to ' + path
            webAppContext.addServlet(servlet, path)
        }
        
    }

    def setRoot(String root) {
        webAppContext.resourceBase = root
    }

    void start() {
        jetty.start()
    }

    void stop() {
        jetty.stop()
    }
}