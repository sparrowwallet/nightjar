package com.sparrowwallet.nightjar.http;

import com.google.common.net.HostAndPort;
import com.samourai.http.client.HttpUsage;
import com.samourai.http.client.IHttpClientService;
import com.samourai.whirlpool.client.utils.ClientUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.ProxyConfiguration;
import org.eclipse.jetty.client.Socks4Proxy;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("UnstableApiUsage")
public class JavaHttpClientService implements IHttpClientService {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String WHIRLPOOL_HTTPCLIENT = "Whirlpool-HttpClient";

    private final Map<HttpUsage, JavaHttpClient> httpClients;
    private HostAndPort torProxy;

    public JavaHttpClientService(HostAndPort torProxy) {
        this.httpClients = new ConcurrentHashMap<>();
        this.torProxy = torProxy;
    }

    public HostAndPort getTorProxy() {
        return torProxy;
    }

    public void setTorProxy(HostAndPort torProxy) {
        this.torProxy = torProxy;
    }

    @Override
    public JavaHttpClient getHttpClient(HttpUsage httpUsage) {
        JavaHttpClient httpClient = httpClients.get(httpUsage);
        if(httpClient == null) {
            if(log.isDebugEnabled()) {
                log.debug("+httpClient[" + httpUsage + "]");
            }
            httpClient = computeHttpClient(httpUsage);
            httpClients.put(httpUsage, httpClient);
        }
        return httpClient;
    }

    private JavaHttpClient computeHttpClient(HttpUsage httpUsage) {
        HttpClient httpClient = computeHttpClient(httpUsage, torProxy);
        return new JavaHttpClient(httpUsage, httpClient, 30000);
    }

    public void changeIdentityRest() {
        for(JavaHttpClient httpClient : httpClients.values()) {
            // restart REST clients
            if(httpClient.getHttpUsage().isRest()) {
                httpClient.restart();
            }
        }
        // don't break non-REST connexions, it will be renewed on next connexion
    }

    public static HttpClient computeHttpClient(HttpUsage httpUsage, HostAndPort proxy) {
        return computeHttpClient(httpUsage, proxy, ClientUtils.USER_AGENT);
    }

    public static HttpClient computeHttpClient(HttpUsage httpUsage, HostAndPort proxy, String userAgent) {
        String name = WHIRLPOOL_HTTPCLIENT + "-" + httpUsage.toString();

        // we use jetty for proxy SOCKS support
        HttpClient jettyHttpClient = new HttpClient(new SslContextFactory.Client.Client());
        jettyHttpClient.setName(name);

        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setName(name);
        threadPool.setDaemon(true);
        jettyHttpClient.setExecutor(threadPool);

        // prevent user-agent tracking
        jettyHttpClient.setUserAgentField(new HttpField(HttpHeader.USER_AGENT, userAgent));

        // proxy
        if(proxy != null) {
            ProxyConfiguration.Proxy jettyProxy = computeJettyProxy(proxy);
            jettyHttpClient.getProxyConfiguration().getProxies().add(jettyProxy);
        } else {
            if(log.isDebugEnabled()) {
                log.debug("+httpClient: no proxy");
            }
        }

        return jettyHttpClient;
    }

    public static ProxyConfiguration.Proxy computeJettyProxy(HostAndPort proxy) {
        return new Socks4Proxy(proxy.getHost(), proxy.getPort());
    }

    public void shutdown() {
        for(JavaHttpClient javaHttpClient : httpClients.values()) {
            javaHttpClient.stop();
        }

        httpClients.clear();
    }
}
