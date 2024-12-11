package anurag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class TradingViewApi {

    public static class ApiResponse {
        private int totalCount;
        private List<Entry> data;

        public ApiResponse(int totalCount, List<Entry> data) {
            this.totalCount = totalCount;
            this.data = data;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public List<Entry> getData() {
            return data;
        }
    }

    public static class Entry {
        private String symbol;
        private String name;
        private String description;
        private double closePrice;
        private long marketCap;
        private String sector;

        public Entry(String symbol, String name, String description, double closePrice, long marketCap, String sector) {
            this.symbol = symbol;
            this.name = name;
            this.description = description;
            this.closePrice = closePrice;
            this.marketCap = marketCap;
            this.sector = sector;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "symbol='" + symbol + '\'' +
                    ", name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    ", closePrice=" + closePrice +
                    ", marketCap=" + marketCap +
                    ", sector='" + sector + '\'' +
                    '}';
        }
    }

    public static ApiResponse fetchData() throws IOException, InterruptedException {
        String apiUrl = "https://scanner.tradingview.com/india/scan?label-product=popup-screener-stock";
        String payload = """
            {"columns":["name","description","logoid","update_mode","type","typespecs","close","pricescale","minmov","fractional","minmove2","currency","change","volume","relative_volume_10d_calc","market_cap_basic","fundamental_currency_code","price_earnings_ttm","earnings_per_share_diluted_ttm","earnings_per_share_diluted_yoy_growth_ttm","dividends_yield_current","sector.tr","market","sector","recommendation_mark","exchange"],
            "filter":[
            {"left":"CCI20|1W","operation":"crosses_above","right":-100},
            {"left":"CCI20","operation":"in_range","right":[-100,0]},
            {"left":"CCI20","operation":"crosses_above","right":-100}
            ],
            "ignore_unknown_fields":false,"options":{"lang":"en"},"range":[0,800],"sort":{"sortBy":"volume","sortOrder":"desc"},"symbols":{},"markets":["india"],"filter2":{"operator":"and","operands":[{"operation":{"operator":"or","operands":[{"operation":{"operator":"and","operands":[{"expression":{"left":"type","operation":"equal","right":"stock"}},{"expression":{"left":"typespecs","operation":"has","right":["common"]}}]}},{"operation":{"operator":"and","operands":[{"expression":{"left":"type","operation":"equal","right":"stock"}},{"expression":{"left":"typespecs","operation":"has","right":["preferred"]}}]}},{"operation":{"operator":"and","operands":[{"expression":{"left":"type","operation":"equal","right":"dr"}}]}},{"operation":{"operator":"and","operands":[{"expression":{"left":"type","operation":"equal","right":"fund"}},{"expression":{"left":"typespecs","operation":"has_none_of","right":["etf"]}}]}}]}}]}}
        """;

        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();

        boolean success = false;
        ApiResponse apiResponse = null;

        while (!success) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode jsonNode = objectMapper.readTree(response.body());
                int totalCount = jsonNode.get("totalCount").asInt();

                List<Entry> entries = new ArrayList<>();
                for (JsonNode node : jsonNode.get("data")) {
                    JsonNode details = node.get("d");
                    entries.add(new Entry(
                            node.get("s").asText(),
                            details.get(0).asText(),
                            details.get(1).asText(),
                            details.get(6).asDouble(),
                            details.get(15).asLong(),
                            details.get(21).asText()
                    ));
                }
                apiResponse = new ApiResponse(totalCount, entries);
                success = true;
            } else {
                Thread.sleep(1000);
            }
        }
        return apiResponse;
    }

    public static void main(String[] args) {
        try {
            ApiResponse response = fetchData();
            System.out.println("Total Count: " + response.getTotalCount());
            for (Entry entry : response.getData()) {
                System.out.println(entry.symbol);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}