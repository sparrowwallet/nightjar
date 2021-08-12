package com.samourai.whirlpool.client.wallet.orchestrator;

import com.google.common.eventbus.Subscribe;
import com.samourai.wallet.util.AbstractOrchestrator;
import com.samourai.whirlpool.client.event.*;
import com.samourai.whirlpool.client.exception.AutoTx0InsufficientBalanceException;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.tx0.Tx0Config;
import com.samourai.whirlpool.client.tx0.Tx0ParamService;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.beans.*;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Collection;
import java.util.LinkedList;
import java8.util.function.Predicate;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoTx0Orchestrator extends AbstractOrchestrator {
  private static final Logger log = LoggerFactory.getLogger(AutoTx0Orchestrator.class);
  private static final int START_DELAY = 10000;

  private WhirlpoolWallet whirlpoolWallet;
  private WhirlpoolWalletConfig config;
  private Tx0ParamService tx0ParamService;

  public AutoTx0Orchestrator(
      WhirlpoolWallet whirlpoolWallet,
      WhirlpoolWalletConfig config,
      Tx0ParamService tx0ParamService) {
    super(config.getAutoTx0Delay() * 1000, START_DELAY, config.getAutoTx0Delay());
    this.whirlpoolWallet = whirlpoolWallet;
    this.config = config;
    this.tx0ParamService = tx0ParamService;

    WhirlpoolEventService.getInstance().register(this);
  }

  @Subscribe
  public void onWalletClose(WalletCloseEvent walletCloseEvent) {
    WhirlpoolEventService.getInstance().unregister(this);
  }

  @Override
  protected void runOrchestrator() {
    // try tx0 with automatic selection of best available utxo
    try {
      if (log.isDebugEnabled()) {
        log.debug("AutoTx0: looking for Tx0...");
      }
      autoTx0(); // throws AutoMixInsufficientBalanceException
      setLastRun();
      log.info(" • AutoTx0: Tx0 SUCCESS");
    } catch (AutoTx0InsufficientBalanceException e) {
      // no tx0 possible yet
      onAutoTx0InsufficientBalance(e);
    } catch (Exception e) {
      log.error("", e);
    }
  }

  public Tx0 autoTx0() throws Exception { // throws AutoMixInsufficientBalanceException
    String poolId = config.getAutoTx0PoolId();
    Pool pool = whirlpoolWallet.getPoolSupplier().findPoolById(poolId);
    if (pool == null) {
      throw new NotifiableException(
          "No pool found for autoTx0 (autoTx0 = " + (poolId != null ? poolId : "null") + ")");
    }
    Tx0FeeTarget tx0FeeTarget = config.getAutoTx0FeeTarget();
    Tx0FeeTarget mixFeeTarget = Tx0FeeTarget.BLOCKS_12;
    Collection<WhirlpoolUtxo> spendFroms =
        findAutoTx0SpendFrom(
            pool, tx0FeeTarget, mixFeeTarget); // throws AutoMixInsufficientBalanceException

    Tx0Config tx0Config = whirlpoolWallet.getTx0Config();
    return whirlpoolWallet.tx0(spendFroms, pool, tx0FeeTarget, mixFeeTarget, tx0Config);
  }

  private Collection<WhirlpoolUtxo> findAutoTx0SpendFrom(
      Pool pool, Tx0FeeTarget tx0FeeTarget, Tx0FeeTarget mixFeeTarget)
      throws Exception { // throws AutoMixInsufficientBalanceException

    // spend TX0 from all non-PREMIX accounts when --auto-tx0-aggregate
    WhirlpoolAccount[] accounts =
        config.isAutoTx0Aggregate()
            ? new WhirlpoolAccount[] {
              WhirlpoolAccount.DEPOSIT, WhirlpoolAccount.POSTMIX, WhirlpoolAccount.BADBANK
            }
            : new WhirlpoolAccount[] {WhirlpoolAccount.DEPOSIT};
    Collection<WhirlpoolUtxo> spendFroms = whirlpoolWallet.getUtxoSupplier().findUtxos(accounts);

    // find ready DEPOSITS
    Collection<WhirlpoolUtxo> readyUtxos = new LinkedList<WhirlpoolUtxo>();
    for (WhirlpoolUtxo whirlpoolUtxo : spendFroms) {
      // check confirmation
      int latestBlockHeight = whirlpoolWallet.getChainSupplier().getLatestBlockHeight();
      int confirmations = whirlpoolUtxo.computeConfirmations(latestBlockHeight);
      if (confirmations >= config.getTx0MinConfirmations()) {
        WhirlpoolUtxoStatus utxoStatus = whirlpoolUtxo.getUtxoState().getStatus();
        if (utxoStatus != WhirlpoolUtxoStatus.TX0
            && utxoStatus != WhirlpoolUtxoStatus.TX0_SUCCESS
            && utxoStatus != WhirlpoolUtxoStatus.MIX_STARTED
            && utxoStatus != WhirlpoolUtxoStatus.MIX_SUCCESS) {
          // spend from ready utxos
          readyUtxos.add(whirlpoolUtxo);
        }
      }
    }

    // check tx0 possible
    if (tx0ParamService.isTx0Possible(
        pool, tx0FeeTarget, mixFeeTarget, WhirlpoolUtxo.sumValue(readyUtxos))) {
      return readyUtxos;
    } else {
      throw new AutoTx0InsufficientBalanceException();
    }
  }

  private long computeTotalUnconfirmedDeposits() {
    final int latestBlockHeight = whirlpoolWallet.getChainSupplier().getLatestBlockHeight();
    return WhirlpoolUtxo.sumValue(
        StreamSupport.stream(whirlpoolWallet.getUtxoSupplier().findUtxos(WhirlpoolAccount.DEPOSIT))
            .filter(
                new Predicate<WhirlpoolUtxo>() {
                  @Override
                  public boolean test(WhirlpoolUtxo whirlpoolUtxo) {
                    // find unconfirmed utxos
                    return (whirlpoolUtxo.computeConfirmations(latestBlockHeight) == 0);
                  }
                })
            .collect(Collectors.<WhirlpoolUtxo>toList()));
  }

  private void onAutoTx0InsufficientBalance(AutoTx0InsufficientBalanceException e) {
    // wait tx0Delay before retry
    setLastRun();

    Pool autoMixPool = whirlpoolWallet.getPoolSupplier().findPoolById(config.getAutoTx0PoolId());
    long autoMixDenomination = autoMixPool.getDenomination();

    // do we have enough unconfirmed deposit for a tx0?
    long minUnconfirmedDeposit = 2 * autoMixDenomination;
    long totalUnconfirmedDeposit = computeTotalUnconfirmedDeposits();
    if (log.isDebugEnabled()) {
      log.debug(
          "totalUnconfirmedDeposit="
              + totalUnconfirmedDeposit
              + ", minUnconfirmedDeposit="
              + minUnconfirmedDeposit);
    }
    if (totalUnconfirmedDeposit >= minUnconfirmedDeposit) {
      if (log.isDebugEnabled()) {
        log.debug(
            "AutoTx0: no tx0 possible yet, awaiting for "
                + ClientUtils.satToBtc(totalUnconfirmedDeposit)
                + "btc UNCONFIRMED DEPOSIT to confirm");
      }
      return;
    }

    // do we have enough premix to mix at full speed?
    int maxClients = Math.min(config.getMaxClients(), config.getMaxClientsPerPool());
    long minQueueBalance = autoMixDenomination * maxClients;
    long totalPremix = whirlpoolWallet.getUtxoSupplier().getBalance(WhirlpoolAccount.PREMIX);
    if (log.isDebugEnabled()) {
      log.debug("totalPremix=" + totalPremix + ", minQueueBalance=" + minQueueBalance);
    }
    if (totalPremix >= minQueueBalance) {
      if (log.isDebugEnabled()) {
        log.debug(
            "AutoTx0: no tx0 possible yet, awaiting for "
                + ClientUtils.satToBtc(totalPremix)
                + "btc PREMIX to mix");
      }
      return;
    }

    // notify
    if (config.isAutoTx0Aggregate()) {
      long minAggregateBalance = minQueueBalance * 4; // at least 4 mixs
      long totalBalance = whirlpoolWallet.getUtxoSupplier().getBalanceTotal();
      if (log.isDebugEnabled()) {
        log.debug("totalBalance=" + totalBalance + ", minAggregateBalance=" + minAggregateBalance);
      }
      if (totalBalance >= minAggregateBalance) {
        // aggregate wallet
        try {
          whirlpoolWallet.aggregate();
        } catch (Exception ee) {
          log.error("aggregate failed", ee);
        }
      } else {
        // not enough funds to continue
        String depositAddress = whirlpoolWallet.getDepositAddress(false);
        String message =
            "insufficient balance for AutoTx0. Please make a deposit to "
                + depositAddress
                + " of at least "
                + ClientUtils.satToBtc(minAggregateBalance)
                + "btc\n";
        whirlpoolWallet.notifyError(message);
      }
    }
  }

  @Subscribe
  public void onWalletStart(WalletStartEvent walletStartEvent) {
    // start orchestrator
    start(true);
  }

  @Subscribe
  public void onWalletStop(WalletStopEvent walletStopEvent) {
    // stop orchestrator
    stop();
  }

  @Subscribe
  public void onUtxosChange(UtxosChangeEvent utxosChangeEvent) {
    if (!isStarted()) {
      return;
    }

    WhirlpoolUtxoChanges whirlpoolUtxoChanges = utxosChangeEvent.getUtxoData().getUtxoChanges();
    boolean notify = false;

    // DETECTED
    for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxoChanges.getUtxosAdded()) {
      int latestBlockHeight = whirlpoolWallet.getChainSupplier().getLatestBlockHeight();
      if (whirlpoolUtxo.computeConfirmations(latestBlockHeight)
          >= config.getTx0MinConfirmations()) {
        notify = true;
      }
    }

    // UPDATED
    for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxoChanges.getUtxosAdded()) {
      int latestBlockHeight = whirlpoolWallet.getChainSupplier().getLatestBlockHeight();
      if (whirlpoolUtxo.computeConfirmations(latestBlockHeight)
          >= config.getTx0MinConfirmations()) {
        notify = true;
      }
    }

    if (notify) {
      if (log.isDebugEnabled()) {
        log.debug(" o AutoTx0: checking for tx0...");
      }
      notifyOrchestrator();
    }
  }
}
