package net.jacobpeterson.alpaca.websocket.marketdata;

import com.google.common.collect.Iterables;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.realtime.MarketDataMessage;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.realtime.news.NewsMessage;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.realtime.control.ErrorMessage;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.realtime.control.SubscriptionsMessage;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.realtime.control.SuccessMessage;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.realtime.enums.MarketDataMessageType;
import net.jacobpeterson.alpaca.websocket.AlpacaWebsocket;
import net.jacobpeterson.alpaca.websocket.AlpacaWebsocketInterface;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.not;
import static net.jacobpeterson.alpaca.util.gson.GsonUtil.GSON;

/**
 * {@link MarketNewsWebsocket} is an {@link AlpacaWebsocket} implementation and provides the {@link
 * MarketDataWebsocketInterface} interface for
 * <a href="https://alpaca.markets/docs/api-documentation/api-v2/market-data/">Realtime Market Data</a> for both crypto
 * and stocks.
 */
public abstract class MarketNewsWebsocket
        extends AlpacaWebsocket<MarketDataMessageType, MarketDataMessage, MarketDataListener>
        implements MarketNewsWebsocketInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketNewsWebsocket.class);
    private static final String MESSAGE_TYPE_ELEMENT_KEY = "T";
    private static final List<String> AUTH_FAILURE_MESSAGES = Arrays.asList(
            "auth failed",
            "auth timeout",
            "not authenticated");
    private static final List<MarketDataMessageType> SUBSCRIBABLE_MARKET_DATA_MESSAGE_TYPES = Arrays.asList(
            MarketDataMessageType.NEWS);

    /**
     * Creates a {@link HttpUrl} for {@link MarketNewsWebsocket} with the given <code>websocketURLPathSegments</code>.
     *
     * @param websocketURLPathSegments the websocket URL path segments
     *
     * @return a {@link HttpUrl}
     */
    private static HttpUrl createWebsocketURL(String websocketURLPathSegments) {
        return new HttpUrl.Builder()
                .scheme("https") // HttpUrl.Builder doesn't recognize "wss" scheme, but "https" works fine
                .host("stream.data.alpaca.markets")
                .addPathSegments(websocketURLPathSegments)
                .build();
    }

    private final Set<MarketDataMessageType> listenedMarketDataMessageTypes;
    private final Set<String> subscribedNews;
    private final Type newsClassType;

    /**
     * Instantiates a new {@link MarketNewsWebsocket}.
     *
     * @param okHttpClient                the {@link OkHttpClient}
     * @param websocketURLPathSegments    the websocket URL path segments
     * @param websocketMarketDataTypeName the websocket market data type name {@link String}
     * @param keyID                       the key ID
     * @param secretKey                   the secret key
     * @param newsClass                  the {@link NewsMessage} {@link Class} to deserialize data into
     */
    public MarketNewsWebsocket(OkHttpClient okHttpClient, String websocketURLPathSegments,
                               String websocketMarketDataTypeName, String keyID, String secretKey,
                               Class<? extends NewsMessage> newsClass) {
        super(okHttpClient, createWebsocketURL(websocketURLPathSegments), websocketMarketDataTypeName + " Market Data",
                keyID, secretKey, null);

        listenedMarketDataMessageTypes = new HashSet<>();
        subscribedNews = new HashSet<>();

        this.newsClassType = newsClass;
    }

    @Override
    protected void cleanupState() {
        super.cleanupState();

        listenedMarketDataMessageTypes.clear();
    }

    @Override
    protected void onConnection() {
        sendAuthenticationMessage();
    }

    @Override
    protected void onReconnection() {
        sendAuthenticationMessage();
        if (waitForAuthorization(5, TimeUnit.SECONDS)) {
            subscribeToControl(Iterables.toArray(listenedMarketDataMessageTypes, MarketDataMessageType.class));
            subscribe(subscribedNews);
        }
    }

    @Override
    protected void sendAuthenticationMessage() {
        // Ensures that 'authenticationMessageFuture' exists
        getAuthorizationFuture();

        /* Format of message is:
         * {
         *   "action": "auth",
         *   "key": "{APCA-API-KEY-ID}",
         *   "secret": "{APCA-API-SECRET-KEY}"
         * }
         */

        JsonObject authObject = new JsonObject();
        authObject.addProperty("action", "auth");
        authObject.addProperty("key", keyID);
        authObject.addProperty("secret", secretKey);

        LOGGER.info("{} websocket sending authentication message...", websocketName);
        websocket.send(authObject.toString());
    }

    // This websocket uses string frames and not binary frames.
    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String message) {
        LOGGER.trace("{}", message);

        JsonElement messageElement = JsonParser.parseString(message);
        checkState(messageElement instanceof JsonArray, "Message must be a JsonArray! Received: %s", messageElement);

        JsonArray messageArray = messageElement.getAsJsonArray();
        for (JsonElement arrayElement : messageArray) {
            checkState(arrayElement instanceof JsonObject,
                    "'arrayElement' must be a JsonObject! Received: %s", arrayElement);

            JsonObject messageObject = arrayElement.getAsJsonObject();
            MarketDataMessageType marketDataMessageType = GSON.fromJson(
                    messageObject.get(MESSAGE_TYPE_ELEMENT_KEY), MarketDataMessageType.class);
            checkNotNull(marketDataMessageType, "MarketDataMessageType not found in message: %s", messageObject);

            MarketDataMessage marketDataMessage;
            switch (marketDataMessageType) {
                case SUCCESS:
                    marketDataMessage = GSON.fromJson(messageObject, SuccessMessage.class);

                    if (isSuccessMessageAuthenticated((SuccessMessage) marketDataMessage)) {
                        LOGGER.info("{} websocket authenticated.", websocketName);

                        authenticated = true;

                        if (authenticationMessageFuture != null) {
                            authenticationMessageFuture.complete(true);
                        }
                    }
                    break;
                case ERROR:
                    marketDataMessage = GSON.fromJson(messageObject, ErrorMessage.class);

                    if (isErrorMessageAuthFailure((ErrorMessage) marketDataMessage) &&
                            authenticationMessageFuture != null) {
                        LOGGER.error("{} websocket not authenticated! Received: {}.", websocketName, marketDataMessage);

                        authenticated = false;

                        if (authenticationMessageFuture != null) {
                            authenticationMessageFuture.complete(false);
                        }
                    } else {
                        LOGGER.error("{} websocket error message: {}", websocketName, marketDataMessage);
                    }
                    break;
                case SUBSCRIPTION:
                    marketDataMessage = GSON.fromJson(messageObject, SubscriptionsMessage.class);

                    // Update 'listenedMarketDataMessageTypes' and the associated subscribed symbols lists
                    SubscriptionsMessage subscriptionsMessage = (SubscriptionsMessage) marketDataMessage;
                    handleSubscriptionMessageList(MarketDataMessageType.NEWS, subscriptionsMessage.getNews(),
                            subscribedNews);
                    break;
                case NEWS:
                    marketDataMessage = GSON.fromJson(messageObject, newsClassType);
                    break;
                default:
                    LOGGER.error("Message type {} not implemented!", marketDataMessageType);
                    continue;
            }

            if (listenedMarketDataMessageTypes.contains(marketDataMessageType)) {
                callListener(marketDataMessageType, marketDataMessage);
            }
        }
    }

    /**
     * Returns true if {@link SuccessMessage#getMessage()} equals "authenticated".
     *
     * @param successMessage the {@link SuccessMessage}
     *
     * @return a boolean
     */
    private boolean isSuccessMessageAuthenticated(SuccessMessage successMessage) {
        return successMessage.getMessage().equalsIgnoreCase("authenticated");
    }

    /**
     * Handles a {@link SubscriptionsMessage} for updating {@link #listenedMarketDataMessageTypes} and returns a {@link
     * List} of currently subscribe symbols or <code>null</code>.
     *
     * @param marketDataMessageType   the {@link MarketDataMessageType}
     * @param newSubscribedSymbols    the new subscribed symbols from {@link SubscriptionsMessage}
     * @param currentSubscribeSymbols the currently subscribe symbols {@link Set}
     */
    private void handleSubscriptionMessageList(MarketDataMessageType marketDataMessageType,
            Collection<String> newSubscribedSymbols, Set<String> currentSubscribeSymbols) {
        if (newSubscribedSymbols != null && !newSubscribedSymbols.isEmpty()) {
            listenedMarketDataMessageTypes.add(marketDataMessageType);
            currentSubscribeSymbols.clear();
            currentSubscribeSymbols.addAll(newSubscribedSymbols);
        } else {
            listenedMarketDataMessageTypes.remove(marketDataMessageType);
            currentSubscribeSymbols.clear();
        }
    }

    /**
     * Returns true if {@link ErrorMessage#getMessage()} is any of {@link #AUTH_FAILURE_MESSAGES}.
     *
     * @param errorMessage the {@link ErrorMessage}
     *
     * @return a boolean
     */
    private boolean isErrorMessageAuthFailure(ErrorMessage errorMessage) {
        return AUTH_FAILURE_MESSAGES.contains(errorMessage.getMessage().toLowerCase());
    }

    @Override
    public void subscribeToControl(MarketDataMessageType... marketDataMessageTypes) {
        if (marketDataMessageTypes == null) {
            return;
        }

        Arrays.stream(marketDataMessageTypes)
                .filter(not(SUBSCRIBABLE_MARKET_DATA_MESSAGE_TYPES::contains))
                .forEach(listenedMarketDataMessageTypes::add);
    }

    @Override
    public void subscribe(Collection<String> newsSymbols) {
        sendSubscriptionUpdate(newsSymbols, true);
    }

    @Override
    public void unsubscribe(Collection<String> newsSymbols) {
        sendSubscriptionUpdate(newsSymbols, false);
    }

    /**
     * Sends a subscription update request.
     *
     * @param newsSymbols a {@link Collection} of symbols to update for news or <code>null</code> for no change
     * @param subscribe    true to subscribe, false to unsubscribe
     */
    private void sendSubscriptionUpdate(Collection<String> newsSymbols, boolean subscribe) {
        if (!isConnected()) {
            throw new IllegalStateException("This websocket must be connected before sending subscription updates!");
        }

        /* Format of message is:
         * {
         *   "action": "subscribe",
         *   "trades": ["AAPL"],
         *   "quotes": ["AMD", "CLDR"],
         *   "bars": ["*"]
         * }
         */

        JsonObject subscriptionUpdateObject = new JsonObject();
        subscriptionUpdateObject.addProperty("action", subscribe ? "subscribe" : "unsubscribe");

        addSubscriptionUpdateList(subscriptionUpdateObject, "news", newsSymbols);

        boolean updateExists = subscriptionUpdateObject.size() > 1;
        if (updateExists) {
            websocket.send(subscriptionUpdateObject.toString());
            LOGGER.info("Requested subscriptions update: {}.", subscriptionUpdateObject);
        }
    }

    /**
     * Adds <code>symbols</code> to <code>subscriptionUpdateObject</code> with <code>elementKey</code> if not empty.
     *
     * @param subscriptionUpdateObject the subscription update {@link JsonObject}
     * @param elementKey               the element key
     * @param symbols                  the symbols {@link Collection}
     */
    private void addSubscriptionUpdateList(JsonObject subscriptionUpdateObject, String elementKey,
            Collection<String> symbols) {
        if (symbols != null && !symbols.isEmpty()) {
            JsonArray symbolArray = new JsonArray();
            symbols.forEach(symbolArray::add);
            subscriptionUpdateObject.add(elementKey, symbolArray);
        }
    }

    @Override
    public Collection<MarketDataMessageType> subscribedControls() {
        return new HashSet<>(listenedMarketDataMessageTypes);
    }

    @Override
    public Collection<String> subscribedNews() {
        return new HashSet<>(subscribedNews);
    }
}
