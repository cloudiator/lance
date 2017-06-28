package de.uniulm.omi.cloudiator.lance.lca;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Frank on 23.11.2015.
 */
public class RestServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestServer.class);

    public RestServer(int restPort, String restHost, ExecutorService executorService, LifecycleAgentRestImpl lca) {
        checkArgument(restPort > 0);

        if (restPort <= 1024) {
            LOGGER.warn("You try to open a port below 1024. This is usually not a good idea...");
        }
        checkNotNull(restHost);
        checkArgument(!restHost.isEmpty());


        URI baseUri = UriBuilder.fromUri(restHost).port(restPort).build();
        ResourceConfig config = new ResourceConfig();
        config.register(new RestController(lca));
        config.register(JacksonFeature.class);
        executorService.execute(new GrizzlyServer(baseUri, config));
    }

    public static class GrizzlyServer implements Runnable {

        private final URI baseUri;
        private final ResourceConfig config;

        private GrizzlyServer(URI baseUri, ResourceConfig config) {
            this.baseUri = baseUri;
            this.config = config;
        }

        public void run() {
            try {
                final HttpServer httpServer = GrizzlyHttpServerFactory.createHttpServer(baseUri, config);
                httpServer.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}