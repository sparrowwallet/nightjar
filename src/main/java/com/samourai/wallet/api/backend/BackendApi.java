package com.samourai.wallet.api.backend;

import com.samourai.wallet.api.backend.beans.*;
import com.samourai.wallet.util.oauth.OAuthApi;
import com.samourai.wallet.util.oauth.OAuthManager;
import java8.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class BackendApi implements OAuthApi {
  private Logger log = LoggerFactory.getLogger(BackendApi.class);

  private static final String URL_UNSPENT = "/unspent?active=";
  private static final String URL_MULTIADDR = "/multiaddr?active=";
  private static final String URL_WALLET = "/wallet?active=";
  private static final String URL_TXS = "/txs?active=";
  private static final String URL_INIT_BIP84 = "/xpub";
  private static final String URL_MINER_FEES = "/fees";
  private static final String URL_PUSHTX = "/pushtx/";
  private static final String URL_GET_AUTH_LOGIN = "/auth/login";
  private static final String URL_GET_AUTH_REFRESH = "/auth/refresh";
  private static final String ZPUB_SEPARATOR = "%7C";

  private IBackendClient httpClient;
  private String urlBackend;
  private Optional<OAuthManager> oAuthManager;

  public BackendApi(IBackendClient httpClient, String urlBackend, Optional<OAuthManager> oAuthManager) {
    this.httpClient = httpClient;
    this.urlBackend = urlBackend;

    if (oAuthManager == null) {
      oAuthManager = Optional.empty();
    }
    this.oAuthManager = oAuthManager;
    if (log.isDebugEnabled()) {
      String oAuthStr = oAuthManager.isPresent() ? "yes" : "no";
      log.debug("urlBackend=" + urlBackend + ", oAuth=" + oAuthStr);
    }
  }

  private String computeZpubStr(String[] zpubs) {
    String zpubStr = StringUtils.join(zpubs,ZPUB_SEPARATOR);
    return zpubStr;
  }

  /**
   * @deprecated use fetchWallet()
   */
  @Deprecated
  public List<UnspentOutput> fetchUtxos(String zpub) throws Exception {
    return fetchUtxos(new String[]{zpub});
  }

  /**
   * @deprecated use fetchWallet()
   */
  @Deprecated
  public List<UnspentOutput> fetchUtxos(String[] zpubs) throws Exception {
    String zpubStr = computeZpubStr(zpubs);
    String url = computeAuthUrl(urlBackend + URL_UNSPENT + zpubStr);
    if (log.isDebugEnabled()) {
      log.debug("fetchUtxos");
    }
    Map<String,String> headers = computeHeaders();
    UnspentResponse unspentResponse = httpClient.getJson(url, UnspentResponse.class, headers);
    List<UnspentOutput> unspentOutputs =
            new ArrayList<UnspentOutput>();
    if (unspentResponse.unspent_outputs != null) {
      unspentOutputs = Arrays.asList(unspentResponse.unspent_outputs);
    }
    return unspentOutputs;
  }

  /**
   * @deprecated use fetchWallet()
   */
  @Deprecated
  public Map<String,MultiAddrResponse.Address> fetchAddresses(String[] zpubs) throws Exception {
    String zpubStr = computeZpubStr(zpubs);
    String url = computeAuthUrl(urlBackend + URL_MULTIADDR + zpubStr);
    if (log.isDebugEnabled()) {
      log.debug("fetchAddress");
    }
    Map<String,String> headers = computeHeaders();
    MultiAddrResponse multiAddrResponse = httpClient.getJson(url, MultiAddrResponse.class, headers);
    Map<String,MultiAddrResponse.Address> addressesByZpub = new LinkedHashMap<String, MultiAddrResponse.Address>();
    if (multiAddrResponse.addresses != null) {
      for (MultiAddrResponse.Address address : multiAddrResponse.addresses) {
        addressesByZpub.put(address.address, address);
      }
    }
    return addressesByZpub;
  }

  /**
   * @deprecated use fetchWallet()
   */
  @Deprecated
  public MultiAddrResponse.Address fetchAddress(String zpub) throws Exception {
    Collection<MultiAddrResponse.Address> addresses = fetchAddresses(new String[]{zpub}).values();
    if (addresses.size() != 1) {
      throw new Exception("Address count=" + addresses.size());
    }
    MultiAddrResponse.Address address = addresses.iterator().next();

    if (log.isDebugEnabled()) {
      log.debug(
          "fetchAddress "
              + zpub
              + ": account_index="
              + address.account_index
              + ", change_index="
              + address.change_index);
    }
    return address;
  }

  /**
   * @deprecated use fetchWallet()
   */
  @Deprecated
  public TxsResponse fetchTxs(String[] zpubs, int page, int count) throws Exception {
    String zpubStr = computeZpubStr(zpubs);

    String url = computeAuthUrl(urlBackend + URL_TXS + zpubStr+"&page="+page+"&count="+count);
    if (log.isDebugEnabled()) {
      log.debug("fetchTxs");
    }
    Map<String,String> headers = computeHeaders();
    return httpClient.getJson(url, TxsResponse.class, headers);
  }

  public WalletResponse fetchWallet(String zpub) throws Exception {
    return fetchWallet(new String[]{zpub});
  }

  public WalletResponse fetchWallet(String[] zpubs) throws Exception {
    String zpubStr = computeZpubStr(zpubs);
    String url = computeAuthUrl(urlBackend + URL_WALLET + zpubStr);
    if (log.isDebugEnabled()) {
      log.debug("fetchWallet");
    }
    Map<String,String> headers = computeHeaders();
    WalletResponse walletResponse = httpClient.getJson(url, WalletResponse.class, headers);
    return walletResponse;
  }

  public void initBip84(String zpub) throws Exception {
    String url = computeAuthUrl(urlBackend + URL_INIT_BIP84);
    if (log.isDebugEnabled()) {
      log.debug("initBip84");
    }
    Map<String,String> headers = computeHeaders();
    Map<String, String> postBody = new HashMap<String, String>();
    postBody.put("xpub", zpub);
    postBody.put("type", "new");
    postBody.put("segwit", "bip84");
    httpClient.postUrlEncoded(url, Void.class, headers, postBody);
  }

  public MinerFee fetchMinerFee() throws Exception {
    String url = computeAuthUrl(urlBackend + URL_MINER_FEES);
    Map<String,String> headers = computeHeaders();
    Map<String, Integer> feeResponse = httpClient.getJson(url, Map.class, headers);
    if (feeResponse == null) {
      throw new Exception("Invalid miner fee response from server");
    }
    return new MinerFee(feeResponse);
  }

  public void pushTx(String txHex) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("pushTx... " + txHex);
    } else {
      log.info("pushTx...");
    }
    String url = computeAuthUrl(urlBackend + URL_PUSHTX);
    Map<String,String> headers = computeHeaders();
    Map<String, String> postBody = new HashMap<String, String>();
    postBody.put("tx", txHex);
    try {
      httpClient.postUrlEncoded(url, Void.class, headers, postBody);
    } catch (HttpException e) {
      if (log.isDebugEnabled()) {
        log.error("pushTx failed", e);
      }
      log.error(
          "PushTx failed: response="
              + e.getResponseBody()
              + ". error="
              + e.getMessage()
              + " for txHex="
              + txHex);
      throw new Exception(
          "PushTx failed (" + e.getResponseBody() + ") for txHex=" + txHex);
    }
  }

  public boolean testConnectivity() {
    try {
      fetchMinerFee();
      return true;
    } catch (Exception e) {
      log.error("", e);
      return false;
    }
  }

  protected Map<String,String> computeHeaders() throws Exception {
    Map<String,String> headers = new HashMap<String, String>();
    if (oAuthManager.isPresent()) {
      // add auth token
      headers.put("Authorization", "Bearer " + oAuthManager.get().getOAuthAccessToken(this));
    }
    return headers;
  }

  protected String computeAuthUrl(String  url) throws Exception {
    // override for auth support
    return url;
  }

  protected IBackendClient getHttpClient() {
    return httpClient;
  }

  public String getUrlBackend() {
    return urlBackend;
  }

  // OAuthAPI

  @Override
  public RefreshTokenResponse.Authorization oAuthAuthenticate(String apiKey) throws Exception {
    String url = getUrlBackend() + URL_GET_AUTH_LOGIN;
    if (log.isDebugEnabled()) {
      log.debug("tokenAuthenticate");
    }
    Map<String, String> postBody = new HashMap<String, String>();
    postBody.put("apikey", apiKey);
    RefreshTokenResponse response =
            getHttpClient().postUrlEncoded(url, RefreshTokenResponse.class, null, postBody);

    if (response.authorizations == null|| StringUtils.isEmpty(response.authorizations.access_token)) {
      throw new Exception("Authorization refused. Invalid apiKey?");
    }
    return response.authorizations;
  }

  @Override
  public String oAuthRefresh(String refreshTokenStr) throws Exception {
    String url = getUrlBackend() + URL_GET_AUTH_REFRESH;
    if (log.isDebugEnabled()) {
      log.debug("tokenRefresh");
    }
    Map<String, String> postBody = new HashMap<String, String>();
    postBody.put("rt", refreshTokenStr);
    RefreshTokenResponse response =
            getHttpClient().postUrlEncoded(url, RefreshTokenResponse.class, null, postBody);

    if (response.authorizations == null || StringUtils.isEmpty(response.authorizations.access_token)) {
      throw new Exception("Authorization refused. Invalid apiKey?");
    }
    return response.authorizations.access_token;
  }
}
