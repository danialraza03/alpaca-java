package net.jacobpeterson.alpaca.websocket.marketdata.news;

import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.realtime.news.StockNewsMessage;
import net.jacobpeterson.alpaca.model.properties.DataAPIType;
import net.jacobpeterson.alpaca.websocket.marketdata.MarketNewsWebsocket;
import okhttp3.OkHttpClient;

/**
 * {@link StockMarketNewsWebsocket} is a {@link MarketNewsWebsocket} for
 * <a href="https://alpaca.markets/docs/api-documentation/api-v2/market-data/alpaca-data-api-v2/real-time/">Realtime
 * Stock Market Data</a>
 */
public class StockMarketNewsWebsocket extends MarketNewsWebsocket {

    /**
     * Instantiates a new {@link StockMarketNewsWebsocket}.
     *
     * @param okHttpClient the {@link OkHttpClient}
     * @param dataAPIType  the {@link DataAPIType}
     * @param keyID        the key ID
     * @param secretKey    the secret key
     */
    public StockMarketNewsWebsocket(OkHttpClient okHttpClient, DataAPIType dataAPIType,
                                    String keyID, String secretKey) {
        super(okHttpClient, "v1beta1/news", "News", keyID, secretKey, StockNewsMessage.class);
    }
}
