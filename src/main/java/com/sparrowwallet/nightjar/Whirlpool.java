package com.sparrowwallet.nightjar;

import com.google.common.net.HostAndPort;
import com.samourai.http.client.HttpUsage;
import com.samourai.tor.client.TorClientService;
import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.BackendServer;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.java.HD_WalletFactoryJava;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;
import com.samourai.whirlpool.client.whirlpool.ServerApi;
import com.sparrowwallet.nightjar.http.JavaHttpClient;
import com.sparrowwallet.nightjar.http.JavaHttpClientService;
import com.sparrowwallet.nightjar.stomp.JavaStompClientService;
import com.sparrowwallet.nightjar.tor.WhirlpoolTorClientService;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class Whirlpool {
    private static final Logger log = LoggerFactory.getLogger(Whirlpool.class);

    private HostAndPort torProxy;
    private final WhirlpoolServer whirlpoolServer;
    private final JavaHttpClientService httpClientService;
    private final JavaStompClientService stompClientService;
    private final TorClientService torClientService;
    private final WhirlpoolWalletService whirlpoolWalletService;
    private final WhirlpoolWalletConfig config;

    public Whirlpool(String network, String torHost, int torPort, String sCode, int maxClients, int clientDelay) {
        torProxy = HostAndPort.fromParts(torHost, torPort);

        whirlpoolServer = WhirlpoolServer.valueOf(network);
        httpClientService = new JavaHttpClientService(this);
        stompClientService = new JavaStompClientService(httpClientService);
        torClientService = new WhirlpoolTorClientService();
        whirlpoolWalletService = new WhirlpoolWalletService();
        config = computeWhirlpoolWalletConfig(sCode, maxClients, clientDelay);
    }

    private WhirlpoolWalletConfig computeWhirlpoolWalletConfig(String sCode, int maxClients, int clientDelay) {
        boolean onion = true;
        String serverUrl = whirlpoolServer.getServerUrl(onion);
        String backendUrl = BackendServer.TESTNET.getBackendUrl(onion);

        ServerApi serverApi = new ServerApi(serverUrl, httpClientService);
        JavaHttpClient httpClientBackend = httpClientService.getHttpClient(HttpUsage.BACKEND);
        BackendApi backendApi = new BackendApi(httpClientBackend, backendUrl, null);

        NetworkParameters params = whirlpoolServer.getParams();
        boolean isAndroid = false;

        WhirlpoolWalletConfig whirlpoolWalletConfig = new WhirlpoolWalletConfig(httpClientService, stompClientService, torClientService, serverApi, params, isAndroid, backendApi);

        whirlpoolWalletConfig.setAutoTx0PoolId(null); // disable auto-tx0
        whirlpoolWalletConfig.setAutoMix(false); // disable auto-mix

        // configure optional settings (or don't set anything for using default values)
        whirlpoolWalletConfig.setScode(sCode);
        whirlpoolWalletConfig.setMaxClients(maxClients);
        whirlpoolWalletConfig.setClientDelay(clientDelay);

        return whirlpoolWalletConfig;
    }

    public WhirlpoolWallet getWhirlpoolWallet(String walletId, List<String> words, String passphrase, int purpose) throws WhirlpoolException {
        try {
            HD_WalletFactoryJava hdWalletFactory = HD_WalletFactoryJava.getInstance();
            MnemonicCode mnemonicCode = MnemonicCode.INSTANCE;
            byte[] seed = hdWalletFactory.computeSeedFromWords(words);
            NetworkParameters networkParameters = (whirlpoolServer == WhirlpoolServer.TESTNET ? TestNet3Params.get() : MainNetParams.get());
            HD_Wallet hdWallet = new HD_Wallet(purpose, mnemonicCode, networkParameters, seed, passphrase, 1);
            String walletIdentifier = whirlpoolServer.toString().toLowerCase() + "-" + walletId;
            String walletStateFileName = computeIndexFile(walletIdentifier).getAbsolutePath();
            String utxoConfigFileName = computeUtxosFile(walletIdentifier).getAbsolutePath();
            return whirlpoolWalletService.openWallet(config, hdWallet, walletStateFileName, utxoConfigFileName);
        } catch(MnemonicException e) {
            throw new WhirlpoolException("Invalid mnemonic " + e.getMessage());
        } catch(Exception e) {
            throw new WhirlpoolException("Could not create whirlpool wallet ", e);
        }
    }

    private File computeIndexFile(String walletIdentifier) throws WhirlpoolException {
        String path = "nightjar-state-" + walletIdentifier.replace(':', '-') + ".json";
        return computeFile(path);
    }

    private File computeUtxosFile(String walletIdentifier) throws WhirlpoolException {
        String path = "nightjar-utxos-" + walletIdentifier.replace(':', '-') + ".json";
        return computeFile(path);
    }

    public static File computeFile(String path) throws WhirlpoolException {
        File f = new File(path);
        if (!f.exists()) {
            if(log.isDebugEnabled()) {
                log.debug("Creating file " + path);
            }
            try {
                f.createNewFile();
            } catch (Exception e) {
                throw new WhirlpoolException("Unable to write file " + path);
            }
        }

        return f;
    }

    public HostAndPort getTorProxy() {
        return torProxy;
    }

    public static void main(String[] args) {
        Whirlpool whirlpool = new Whirlpool("TESTNET", "localhost", 9050, "SPARROW", 1, 15);
    }
}
