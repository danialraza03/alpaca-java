package net.jacobpeterson.alpaca.websocket.marketdata;

import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.realtime.MarketDataMessage;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.realtime.control.ErrorMessage;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.realtime.control.SubscriptionsMessage;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.realtime.control.SuccessMessage;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.realtime.enums.MarketDataMessageType;
import net.jacobpeterson.alpaca.websocket.AlpacaWebsocketInterface;

import java.util.Collection;

/**
 * {@link MarketNewsWebsocketInterface} is an {@link AlpacaWebsocketInterface} for a {@link MarketNewsWebsocket}.
 */
public interface MarketNewsWebsocketInterface extends AlpacaWebsocketInterface<MarketDataListener> {

    /**
     * Subscribe to a specific control {@link MarketDataMessage} which contain information about the stream's current
     * state. That is, an {@link ErrorMessage}, {@link SubscriptionsMessage}, or {@link SuccessMessage}.
     *
     * @param marketDataMessageTypes array containing any of the following: {@link MarketDataMessageType#SUCCESS},
     *                               {@link MarketDataMessageType#ERROR}, or {@link MarketDataMessageType#SUBSCRIPTION}
     */
    void subscribeToControl(MarketDataMessageType... marketDataMessageTypes);

    /**
     * Subscribes to news according to the given {@link Collection} of symbols.
     * <br>
     * Note that the given {@link Collection} can contain the wildcard character e.g. "*" to subscribe to
     * ALL available symbols.
     *
     * @param newsSymbols a {@link Collection} of symbols to subscribe to news or <code>null</code> for no change
     *
     * @see #unsubscribe(Collection)
     */
    void subscribe(Collection<String> newsSymbols);

    /**
     * Unsubscribes from news according to the given {@link Collection} of symbols.
     * <br>
     * Note that the given {@link Collection} can contain the wildcard character (e.g. "*") to unsubscribe
     * from a previously subscribed wildcard.
     *
     * @param newsSymbols a {@link Collection} of symbols to unsubscribe from news or <code>null</code> for no
     *                     change
     *
     * @see #subscribe(Collection)
     */
    void unsubscribe(Collection<String> newsSymbols);

    /**
     * Gets all the currently subscribed control {@link MarketDataMessageType}s.
     *
     * @return a {@link Collection} of {@link MarketDataMessageType}s
     */
    Collection<MarketDataMessageType> subscribedControls();

    /**
     * Gets all the currently subscribed symbols for news updates.
     *
     * @return a {@link Collection} of {@link String}s
     */
    Collection<String> subscribedNews();
}
