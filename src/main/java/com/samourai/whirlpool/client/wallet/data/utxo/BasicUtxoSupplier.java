package com.samourai.whirlpool.client.wallet.data.utxo;

import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.hd.AddressType;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.send.provider.UtxoProvider;
import com.samourai.whirlpool.client.event.UtxosChangeEvent;
import com.samourai.whirlpool.client.event.UtxosResponseEvent;
import com.samourai.whirlpool.client.tx0.Tx0ParamService;
import com.samourai.whirlpool.client.wallet.WhirlpoolEventService;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.data.chain.ChainSupplier;
import com.samourai.whirlpool.client.wallet.data.pool.PoolSupplier;
import com.samourai.whirlpool.client.wallet.data.supplier.BasicSupplier;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigManager;
import com.samourai.whirlpool.client.wallet.data.utxoConfig.UtxoConfigSupplier;
import com.samourai.whirlpool.client.wallet.data.wallet.WalletSupplierImpl;
import java8.util.function.Predicate;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class BasicUtxoSupplier extends BasicSupplier<UtxoData>
    implements UtxoProvider, UtxoSupplier {
  private static final Logger log = LoggerFactory.getLogger(BasicUtxoSupplier.class);

  private final WhirlpoolEventService eventService = WhirlpoolEventService.getInstance();
  private final WalletSupplierImpl walletSupplier;
  private final UtxoConfigSupplier utxoConfigSupplier;
  private final ChainSupplier chainSupplier;
  private final PoolSupplier poolSupplier;
  private final Tx0ParamService tx0ParamService;
  private NetworkParameters params;
  private UtxoConfigManager utxoConfigManager; // updates utxoConfig automatically

  private Map<String, WhirlpoolUtxo> previousUtxos;

  public BasicUtxoSupplier(
      WalletSupplierImpl walletSupplier,
      UtxoConfigSupplier utxoConfigSupplier,
      ChainSupplier chainSupplier,
      PoolSupplier poolSupplier,
      Tx0ParamService tx0ParamService,
      NetworkParameters params)
      throws Exception {
    super(log, null);
    this.previousUtxos = null;
    this.walletSupplier = walletSupplier;
    this.utxoConfigSupplier = utxoConfigSupplier;
    this.chainSupplier = chainSupplier;
    this.poolSupplier = poolSupplier;
    this.tx0ParamService = tx0ParamService;
    this.params = params;
    this.utxoConfigManager = new UtxoConfigManager(utxoConfigSupplier);
  }

  public abstract void refresh() throws Exception;

  @Override
  public void setValue(UtxoData utxoData) throws Exception {
    utxoData.init(
        walletSupplier,
        utxoConfigSupplier,
        poolSupplier,
        tx0ParamService,
        previousUtxos,
        chainSupplier.getLatestBlock().height);

    // update previousUtxos
    Map<String, WhirlpoolUtxo> newPreviousUtxos = new LinkedHashMap<String, WhirlpoolUtxo>();
    newPreviousUtxos.putAll(utxoData.getUtxos());
    previousUtxos = newPreviousUtxos;

    // set new value
    super.setValue(utxoData);

    // notify
    eventService.post(new UtxosResponseEvent());
    if (!utxoData.getUtxoChanges().isEmpty()) {
      eventService.post(new UtxosChangeEvent(utxoData));
    }
  }

  @Override
  public Collection<WhirlpoolUtxo> findUtxos(final WhirlpoolAccount... whirlpoolAccounts) {
    return getValue().findUtxos(whirlpoolAccounts);
  }

  @Override
  public Collection<WhirlpoolUtxo> findUtxos(
          final AddressType addressType, final WhirlpoolAccount... whirlpoolAccounts) {
    return StreamSupport.stream(findUtxos(whirlpoolAccounts))
        .filter(
            new Predicate<WhirlpoolUtxo>() {
              @Override
              public boolean test(WhirlpoolUtxo whirlpoolUtxo) {
                return whirlpoolUtxo.getAddressType() == addressType;
              }
            })
        .collect(Collectors.<WhirlpoolUtxo>toList());
  }

  @Override
  public Collection<WhirlpoolUtxo> findUtxosByAddress(String address) {
    return getValue().findUtxosByAddress(address);
  }

  @Override
  public Collection<WalletResponse.Tx> findTxs(WhirlpoolAccount whirlpoolAccount) {
    return getValue().findTxs(whirlpoolAccount);
  }

  @Override
  public long getBalance(WhirlpoolAccount whirlpoolAccount) {
    return getValue().getBalance(whirlpoolAccount);
  }

  @Override
  public long getBalanceTotal() {
    return getValue().getBalanceTotal();
  }

  @Override
  public WhirlpoolUtxo findUtxo(String utxoHash, int utxoIndex) {
    // find by key
    WhirlpoolUtxo whirlpoolUtxo = getValue().findByUtxoKey(utxoHash, utxoIndex);
    if (whirlpoolUtxo != null) {
      return whirlpoolUtxo;
    }
    log.warn("findUtxo(" + utxoHash + ":" + utxoIndex + "): not found");
    return null;
  }

  // UtxoSupplier

  @Override
  public String getChangeAddress(WhirlpoolAccount account, AddressType addressType) {
    // TODO zeroleak revert change index
    return walletSupplier
        .getWallet(account, addressType)
        .getNextChangeAddress()
        .getAddressString(addressType);
  }

  @Override
  public Collection<UTXO> getUtxos(WhirlpoolAccount account, AddressType addressType) {
    return toUTXOs(findUtxos(addressType, account));
  }

  @Override
  public Collection<UTXO> getUtxos(WhirlpoolAccount account) {
    return toUTXOs(findUtxos(account));
  }

  @Override
  public ECKey _getPrivKey(String utxoHash, int utxoIndex) throws Exception {
    WhirlpoolUtxo whirlpoolUtxo = findUtxo(utxoHash, utxoIndex);
    if (whirlpoolUtxo == null) {
      throw new Exception("Utxo not found: " + utxoHash + ":" + utxoIndex);
    }
    HD_Address premixAddress = getAddress(whirlpoolUtxo);
    return premixAddress.getECKey();
  }

  private HD_Address getAddress(WhirlpoolUtxo whirlpoolUtxo) {
    UnspentOutput utxo = whirlpoolUtxo.getUtxo();
    AddressType addressType = AddressType.findByAddress(utxo.addr, params);
    return walletSupplier.getWallet(whirlpoolUtxo.getAccount(), addressType).getAddressAt(utxo);
  }

  private Collection<UTXO> toUTXOs(Collection<WhirlpoolUtxo> whirlpoolUtxos) {
    // group utxos by script = same address
    Map<String, UTXO> utxoByScript = new LinkedHashMap<String, UTXO>();
    for (WhirlpoolUtxo whirlpoolUtxo : whirlpoolUtxos) {
      MyTransactionOutPoint outPoint = whirlpoolUtxo.getUtxo().computeOutpoint(params);
      String script = whirlpoolUtxo.getUtxo().script;

      UTXO utxo = utxoByScript.get(script);
      if (utxo == null) {
        utxo = new UTXO();
        utxo.setPath(whirlpoolUtxo.getUtxo().getPath());
        utxoByScript.put(script, utxo);
      }
      utxo.getOutpoints().add(outPoint);
      if (utxo.getOutpoints().size() > 1) {
        if (log.isDebugEnabled()) {
          log.debug(
              "Found "
                  + utxo.getOutpoints().size()
                  + " UTXO with same address: "
                  + utxo.getOutpoints());
        }
      }
    }
    return utxoByScript.values();
  }
}
