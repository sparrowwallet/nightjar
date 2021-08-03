package com.sparrowwallet.nightjar.http;

import com.google.common.net.HostAndPort;
import com.samourai.http.client.HttpUsage;
import com.samourai.http.client.IHttpClientService;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.sparrowwallet.nightjar.Whirlpool;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.ProxyConfiguration;
import org.eclipse.jetty.client.Socks4Proxy;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JavaHttpClientService implements IHttpClientService {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Map<HttpUsage, JavaHttpClient> httpClients;
    private final Whirlpool whirlpool;

    public JavaHttpClientService(Whirlpool whirlpool) {
        this.httpClients = new ConcurrentHashMap<>();
        this.whirlpool = whirlpool;
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
        HttpClient httpClient = computeHttpClient(whirlpool.getTorProxy());
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

    public static HttpClient computeHttpClient(HostAndPort proxy) {
        return computeHttpClient(proxy, ClientUtils.USER_AGENT);
    }

    public static HttpClient computeHttpClient(HostAndPort proxy, String userAgent) {
        // we use jetty for proxy SOCKS support
        HttpClient jettyHttpClient = new HttpClient(new SslContextFactory.Client.Client());
        // jettyHttpClient.setSocketAddressResolver(new MySocketAddressResolver());

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
    }
}
