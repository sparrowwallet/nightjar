package com.sparrowwallet.nightjar;

import com.samourai.http.client.HttpUsage;
import com.samourai.http.client.IHttpClient;
import com.samourai.soroban.cahoots.CahootsContext;
import com.samourai.soroban.client.OnlineSorobanInteraction;
import com.samourai.soroban.client.SorobanMessage;
import com.samourai.soroban.client.cahoots.OnlineCahootsMessage;
import com.samourai.soroban.client.cahoots.SorobanCahootsService;
import com.samourai.soroban.client.meeting.SorobanMeetingService;
import com.samourai.soroban.client.meeting.SorobanRequestMessage;
import com.samourai.soroban.client.meeting.SorobanResponseMessage;
import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bip47.rpc.java.Bip47UtilJava;
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.TestCahootsWallet;
import com.samourai.wallet.cahoots.psbt.PSBT;
import com.samourai.wallet.cahoots.stonewallx2.STONEWALLx2;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactoryGeneric;
import com.sparrowwallet.nightjar.http.JavaHttpClientService;
import io.reactivex.functions.Consumer;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.TestNet3Params;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Provider;
import java.util.Objects;

public class SorobanServiceTest {
    private static final Logger log = LoggerFactory.getLogger(SorobanServiceTest.class);

    private static final String SEED_WORDS = "all all all all all all all all all all all all";
    private static final String SEED_PASSPHRASE_INITIATOR = "initiator";
    private static final String SEED_PASSPHRASE_COUNTERPARTY = "counterparty";

    protected static final int TIMEOUT_MS = 20000;
    protected static final Provider PROVIDER_JAVA = new BouncyCastleProvider();

    protected static final NetworkParameters params = TestNet3Params.get();
    protected static final Bip47UtilJava bip47Util = Bip47UtilJava.getInstance();
    protected static final HD_WalletFactoryGeneric hdWalletFactory = HD_WalletFactoryGeneric.getInstance();

    public static void main(String[] args) throws Exception {
        SorobanServiceTest sorobanServiceTest = new SorobanServiceTest();
        sorobanServiceTest.meet();
    }

    public void meet() throws Exception {
        JavaHttpClientService javaHttpClientService = new JavaHttpClientService(null);

        final BIP47Wallet bip47walletInitiator = bip47Wallet(SEED_WORDS, SEED_PASSPHRASE_INITIATOR);
        final BIP47Wallet bip47walletCounterparty = bip47Wallet(SEED_WORDS, SEED_PASSPHRASE_COUNTERPARTY);

        final PaymentCode paymentCodeInitiator = bip47Util.getPaymentCode(bip47walletInitiator);
        final PaymentCode paymentCodeCounterparty = bip47Util.getPaymentCode(bip47walletCounterparty);

        // run initiator
        Thread threadInitiator =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                // instanciate services
                                IHttpClient httpClient = javaHttpClientService.getHttpClient(HttpUsage.COORDINATOR_REST);
                                RpcClient rpcClient = new RpcClient(httpClient, false, params);
                                final SorobanMeetingService sorobanMeetingService = new SorobanMeetingService(bip47Util, params, PROVIDER_JAVA, bip47walletInitiator, rpcClient);

                                try {
                                    // request soroban meeeting
                                    SorobanRequestMessage request = sorobanMeetingService.sendMeetingRequest(paymentCodeCounterparty, CahootsType.STONEWALLX2).blockingSingle();
                                    SorobanResponseMessage response = sorobanMeetingService.receiveMeetingResponse(paymentCodeCounterparty, request, TIMEOUT_MS).blockingSingle();
                                    log.info("Accepted by contributor: " + response.isAccept());
                                } catch (Exception e) {
                                    log.error("Error with initiator", e);
                                }
                            }
                        });
        threadInitiator.start();

        // run contributor
        Thread threadContributor =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                // instanciate services
                                IHttpClient httpClient = javaHttpClientService.getHttpClient(HttpUsage.COORDINATOR_REST);
                                RpcClient rpcClient = new RpcClient(httpClient, false, params);
                                SorobanMeetingService sorobanMeetingService = new SorobanMeetingService(bip47Util, params, PROVIDER_JAVA, bip47walletCounterparty, rpcClient);

                                try {
                                    // listen for Soroban requests
                                    SorobanRequestMessage requestMessage = sorobanMeetingService.receiveMeetingRequest(TIMEOUT_MS).blockingSingle();
                                    log.info("" + requestMessage.getType());
                                    log.info("Contributor: " + paymentCodeInitiator.toString() + " " + requestMessage.getSender());

                                    // response accept
                                    sorobanMeetingService.sendMeetingResponse(paymentCodeInitiator, requestMessage, true).subscribe();
                                } catch (Exception e) {
                                    log.error("Error with contributor", e);
                                }
                            }
                        });
        threadContributor.start();

        threadInitiator.join();
        threadContributor.join();

        log.info("*** SOROBAN MEETING SUCCESS ***");
    }

    public void stonewallx2() throws Exception {
        JavaHttpClientService javaHttpClientService = new JavaHttpClientService(null);

        final BIP47Wallet bip47walletInitiator = bip47Wallet(SEED_WORDS, SEED_PASSPHRASE_INITIATOR);
        final BIP47Wallet bip47walletCounterparty = bip47Wallet(SEED_WORDS, SEED_PASSPHRASE_COUNTERPARTY);

        final PaymentCode paymentCodeInitiator = bip47Util.getPaymentCode(bip47walletInitiator);
        final PaymentCode paymentCodeCounterparty = bip47Util.getPaymentCode(bip47walletCounterparty);

        final int account = 0;
        final TestCahootsWallet cahootsWalletInitiator = computeCahootsWallet(SEED_WORDS, SEED_PASSPHRASE_INITIATOR);
        cahootsWalletInitiator.addUtxo(account, "senderTx1", 1, 10000, "tb1qkymumss6zj0rxy9l3v5vqxqwwffy8jjsyhrkrg");
        final TestCahootsWallet cahootsWalletCounterparty = computeCahootsWallet(SEED_WORDS, SEED_PASSPHRASE_COUNTERPARTY);
        cahootsWalletCounterparty.addUtxo(account, "counterpartyTx1", 1, 10000, "tb1qh287jqsh6mkpqmd8euumyfam00fkr78qhrdnde");

        // run initiator
        Thread threadInitiator =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                // instanciate services
                                IHttpClient httpClient = javaHttpClientService.getHttpClient(HttpUsage.BACKEND);
                                RpcClient rpcClient = new RpcClient(httpClient, false, params);
                                final SorobanCahootsService sorobanCahootsService = new SorobanCahootsService(bip47Util, PROVIDER_JAVA, cahootsWalletInitiator, rpcClient);

                                /*
                                 * #1 => accept
                                 */
                                runInitiator(true, sorobanCahootsService, account, paymentCodeCounterparty);

                                /*
                                 * #2 => reject
                                 */
                                runInitiator(false, sorobanCahootsService, account, paymentCodeCounterparty);
                            }
                        });
        threadInitiator.start();

        // run contributor
        Thread threadContributor =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                // instanciate services
                                IHttpClient httpClient = javaHttpClientService.getHttpClient(HttpUsage.BACKEND);
                                RpcClient rpcClient = new RpcClient(httpClient, false, params);
                                SorobanCahootsService sorobanCahootsService = new SorobanCahootsService(bip47Util, PROVIDER_JAVA, cahootsWalletCounterparty, rpcClient);

                                /** #1 => accept */
                                runContributor(true, sorobanCahootsService, account, paymentCodeInitiator);

                                /** #2 => reject */
                                runContributor(false, sorobanCahootsService, account, paymentCodeInitiator);
                            }
                        });
        threadContributor.start();

        threadInitiator.join();
        threadContributor.join();
    }

    private void runInitiator(
            final boolean ACCEPT,
            SorobanCahootsService sorobanCahootsService,
            int account,
            PaymentCode paymentCodeCounterparty) {
        // run soroban as initiator
        long amount = 5;
        String address = "tb1q9m8cc0jkjlc9zwvea5a2365u6px3yu646vgez4";

        try {
            CahootsContext cahootsContext = CahootsContext.newInitiatorStonewallx2(amount, address);

            sorobanCahootsService
                    .getSorobanService()
                    .getOnInteraction()
                    .subscribe(
                            new Consumer<OnlineSorobanInteraction>() {
                                @Override
                                public void accept(OnlineSorobanInteraction interaction) throws Exception {
                                    log.info("Type: " + interaction.getTypeInteraction());
                                    log.info("[INTERACTION] ==> TX_BROADCAST");
                                    if (ACCEPT) {
                                        interaction.sorobanAccept();
                                    } else {
                                        interaction.sorobanReject("TEST_REJECT");
                                    }
                                }
                            });
            SorobanMessage lastMessage = sorobanCahootsService
                            .initiator(account, cahootsContext, paymentCodeCounterparty, TIMEOUT_MS)
                            .blockingLast();

            if(lastMessage instanceof OnlineCahootsMessage onlineCahootsMessage) {
                Cahoots cahoots = onlineCahootsMessage.getCahoots();
                if(cahoots instanceof STONEWALLx2 stonewallx2) {
                    PSBT psbt = stonewallx2.getPSBT();
                    Transaction transaction = psbt.getTransaction();
                    log.info("Transaction: " + Hex.toHexString(transaction.bitcoinSerialize()));
                }
            }

            if (ACCEPT) {
                if(!Objects.equals(
                        "{\"cahoots\":\"{\\\"cahoots\\\":{\\\"fingerprint_collab\\\":\\\"f0d70870\\\",\\\"psbt\\\":\\\"\\\",\\\"cpty_account\\\":0,\\\"spend_amount\\\":5,\\\"outpoints\\\":[{\\\"value\\\":10000,\\\"outpoint\\\":\\\"14cf9c6be92efcfe628aabd32b02c85e763615ddd430861bc18f6d366e4c4fd5-1\\\"},{\\\"value\\\":10000,\\\"outpoint\\\":\\\"9407b31fd0159dc4dd3f5377e3b18e4b4aafef2977a52e76b95c3f899cbb05ad-1\\\"}],\\\"type\\\":0,\\\"dest\\\":\\\"tb1q9m8cc0jkjlc9zwvea5a2365u6px3yu646vgez4\\\",\\\"params\\\":\\\"testnet\\\",\\\"version\\\":2,\\\"fee_amount\\\":314,\\\"fingerprint\\\":\\\"eed8a1cd\\\",\\\"step\\\":4,\\\"collabChange\\\":\\\"tb1qv4ak4l0w76qflk4uulavu22kxtaajnltkzxyq5\\\",\\\"id\\\":\\\"testID\\\",\\\"account\\\":0,\\\"ts\\\":123456}}\"}",
                        lastMessage)) {
                    log.error("Unexpected message: " + lastMessage);
                }
            } else {
                log.error("Not accepted");
            }
        } catch (Exception e) {
            if (ACCEPT) {
                log.error("Error", e);
            } else {
                if(e.getMessage().contains("TEST_REJECT")) {
                    log.info("Rejected");
                }
            }
        }
    }

    private void runContributor(boolean ACCEPT, SorobanCahootsService sorobanCahootsService, int account, PaymentCode paymentCodeInitiator) {
        try {
            // run soroban as counterparty
            CahootsContext cahootsContext = CahootsContext.newCounterpartyStonewallx2();
            SorobanMessage lastMessage = sorobanCahootsService
                            .contributor(account, cahootsContext, paymentCodeInitiator, TIMEOUT_MS)
                            .blockingLast();
            if (ACCEPT) {
                if(!Objects.equals(
                        "{\"cahoots\":\"{\\\"cahoots\\\":{\\\"fingerprint_collab\\\":\\\"f0d70870\\\",\\\"psbt\\\":\\\"\\\",\\\"cpty_account\\\":0,\\\"spend_amount\\\":5,\\\"outpoints\\\":[{\\\"value\\\":10000,\\\"outpoint\\\":\\\"14cf9c6be92efcfe628aabd32b02c85e763615ddd430861bc18f6d366e4c4fd5-1\\\"},{\\\"value\\\":10000,\\\"outpoint\\\":\\\"9407b31fd0159dc4dd3f5377e3b18e4b4aafef2977a52e76b95c3f899cbb05ad-1\\\"}],\\\"type\\\":0,\\\"dest\\\":\\\"tb1q9m8cc0jkjlc9zwvea5a2365u6px3yu646vgez4\\\",\\\"params\\\":\\\"testnet\\\",\\\"version\\\":2,\\\"fee_amount\\\":314,\\\"fingerprint\\\":\\\"eed8a1cd\\\",\\\"step\\\":4,\\\"collabChange\\\":\\\"tb1qv4ak4l0w76qflk4uulavu22kxtaajnltkzxyq5\\\",\\\"id\\\":\\\"testID\\\",\\\"account\\\":0,\\\"ts\\\":123456}}\"}",
                        lastMessage)) {
                    log.error("Unexpected message: " + lastMessage);
                }
            } else {
                log.error("Not accepted");
            }
        } catch (Exception e) {
            if (ACCEPT) {
                log.error("Error", e);
            } else {
                if(e.getMessage().contains("TEST_REJECT")) {
                    log.info("Rejected");
                }
            }
        }
    }

    private TestCahootsWallet computeCahootsWallet(String seedWords, String passphrase)
            throws Exception {
        byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);
        HD_Wallet bip84w = hdWalletFactory.getBIP84(seed, passphrase, params);
        return new TestCahootsWallet(bip84w, params);
    }

    protected BIP47Wallet bip47Wallet(String seedWords, String passphrase) throws Exception {
        HD_Wallet hdWallet = hdWalletFactory.restoreWallet(seedWords, passphrase, params);
        BIP47Wallet bip47w = hdWalletFactory.getBIP47(hdWallet.getSeedHex(), hdWallet.getPassphrase(), params);
        return bip47w;
    }
}
