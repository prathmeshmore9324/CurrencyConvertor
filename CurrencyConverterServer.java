import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CurrencyConverterServer {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(9090), 0); // Change port to 9090
        server.createContext("/convert", new CurrencyConversionHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port 9090");
    }
    

    static class CurrencyConversionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("🔹 Request received: " + exchange.getRequestURI());

            // Enable CORS
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            // Handle OPTIONS preflight requests
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\": \"Method Not Allowed\"}");
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            if (query == null || !query.contains("from") || !query.contains("to") || !query.contains("amount")) {
                sendResponse(exchange, 400, "{\"error\": \"Invalid request parameters\"}");
                return;
            }

            Map<String, String> params = getQueryParams(query);
            String from = params.get("from");
            String to = params.get("to");
            double amount;

            try {
                amount = Double.parseDouble(params.get("amount"));
            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, "{\"error\": \"Invalid amount\"}");
                return;
            }

            double conversionRate = getConversionRate(from, to);
            double result = amount * conversionRate;

            String response = String.format("{\"result\": %.2f}", result);
            sendResponse(exchange, 200, response);
        }

        private double getConversionRate(String from, String to) {
            Map<String, Double> rates = new HashMap<>();
            rates.put("USD_EUR", 0.85);
            rates.put("EUR_USD", 1.18);
            rates.put("USD_GBP", 0.75);
            rates.put("GBP_USD", 1.33);
            rates.put("USD_INR", 83.0);
            rates.put("INR_USD", 0.012);
            rates.put("EUR_GBP", 0.88);
            rates.put("GBP_EUR", 1.14);

            return rates.getOrDefault(from + "_" + to, 1.0);
        }

        private Map<String, String> getQueryParams(String query) {
            Map<String, String> params = new HashMap<>();
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2) {
                    params.put(pair[0], pair[1]);
                }
            }
            return params;
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            System.out.println("🔹 Response: " + response);
            exchange.sendResponseHeaders(statusCode, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}
