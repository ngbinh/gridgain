// @java.file.header

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.client.router;

import java.util.*;

import org.gridgain.client.*;

/**
 * HTTP router interface.
 * <p>
 * Router allows remote clients to connect to grid without direct access to
 * the network where grid is running. It accepts requests on the same protocol
 * as grid rest server and establish required connection to grid nodes
 * to serve them.
 * <p>
 * This router works only with HTTP rest protocol, for routing of binary protocol
 * use {@link GridTcpRouter}.
 * <p>
 * Below is an example on how to start TCP router with non-default configuration.
 * <pre name="code" class="java">
 * GridHttpRouterConfiguration cfg =
 *     new GridHttpRouterConfiguration();
 *
 * cfg.setJettyConfigurationPath("config/custom-router-jetty.xml");
 *
 * cfg.setServers(Arrays.asList(
 *     "node1.appdomain.com:11211",
 *     "node2.appdomain.com:11211"));
 *
 * GridRouterFactory.startHttpRouter(cfg);
 * </pre>
 * <p>
 * Note that clients should be specifically configured in order to use router.
 * Please refer to {@link GridClientConfiguration#getServers()} and
 * {@link GridClientConfiguration#getRouters()} documentation for more details.
 * <p>
 * Instances of this interface are managed through {@link GridRouterFactory}.
 *
 * @see GridHttpRouterConfiguration
 * @author @java.author
 * @version @java.version
 */
public interface GridHttpRouter {
    /**
     * Returns router Id.
     * <p>
     * Unique router Ids are automatically generated on router startup.
     * They are used to control router's lifecycle via {@link GridRouterFactory}.
     *
     * @see GridRouterFactory#httpRouter(java.util.UUID)
     * @see GridRouterFactory#stopHttpRouter(java.util.UUID)
     * @return Router Id.
     */
    public UUID id();

    /**
     * Returns configuration used to start router.
     *
     * @see GridRouterFactory#startHttpRouter(GridHttpRouterConfiguration)
     * @return Router configuration.
     */
    public GridHttpRouterConfiguration configuration();
}