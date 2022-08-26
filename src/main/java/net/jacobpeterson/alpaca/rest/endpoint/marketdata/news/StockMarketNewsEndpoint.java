package net.jacobpeterson.alpaca.rest.endpoint.marketdata.news;

import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.news.StockNews;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.news.StockNewsResponse;
import net.jacobpeterson.alpaca.rest.AlpacaClient;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;
import net.jacobpeterson.alpaca.rest.endpoint.AlpacaEndpoint;
import net.jacobpeterson.alpaca.util.format.FormatUtil;
import okhttp3.HttpUrl;
import okhttp3.Request;

import java.time.ZonedDateTime;
import java.util.ArrayList;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link AlpacaEndpoint} for
 * <a href="https://alpaca.markets/docs/api-documentation/api-v2/market-data/alpaca-data-api-v2/historical/">Historical
 * Market Data API v2</a>.
 */
public class StockMarketNewsEndpoint extends AlpacaEndpoint {

    /**
     * Instantiates a new {@link StockMarketNewsEndpoint}.
     *
     * @param alpacaClient the {@link AlpacaClient}
     */
    public StockMarketNewsEndpoint(AlpacaClient alpacaClient) {
        super(alpacaClient, "news");
    }

    /**
     * Gets {@link StockNews} historical data for the requested security.
     *
     * @param symbols   the list of symbols to query for
     * @param start     filter data equal to or after this {@link ZonedDateTime}. Fractions of a second are not
     *                  accepted.
     * @param end       filter data equal to or before this {@link ZonedDateTime}. Fractions of a second are not
     *                  accepted.
     * @param limit     number of data points to return. Must be in range 1-10000, defaults to 1000 if <code>null</code>
     *                  is given
     * @param pageToken pagination token to continue from
     *
     * @return the {@link StockNewsResponse}
     *
     * @throws AlpacaClientException thrown for {@link AlpacaClientException}s
     */
    public StockNewsResponse getNews(ArrayList<String> symbols, ZonedDateTime start, ZonedDateTime end, Integer limit,
            String pageToken) throws AlpacaClientException {
        checkNotNull(symbols);
        checkNotNull(start);
        checkNotNull(end);

        HttpUrl.Builder urlBuilder = alpacaClient.urlBuilder()
                .addPathSegment(endpointPathSegment);

        urlBuilder.addQueryParameter("symbols", FormatUtil.toCommaSeperatedString(symbols));
        urlBuilder.addQueryParameter("start", FormatUtil.toRFC3339Format(start));
        urlBuilder.addQueryParameter("end", FormatUtil.toRFC3339Format(end));

        if (limit != null) {
            urlBuilder.addQueryParameter("limit", limit.toString());
        }

        if (pageToken != null) {
            urlBuilder.addQueryParameter("page_token", pageToken);
        }

        Request request = alpacaClient.requestBuilder(urlBuilder.build())
                .get()
                .build();
        return alpacaClient.requestObject(request, StockNewsResponse.class);
    }
}
