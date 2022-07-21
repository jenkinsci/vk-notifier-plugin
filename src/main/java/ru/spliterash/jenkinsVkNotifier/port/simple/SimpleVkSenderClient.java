package ru.spliterash.jenkinsVkNotifier.port.simple;

import lombok.RequiredArgsConstructor;
import ru.spliterash.jenkinsVkNotifier.port.VkSender;
import ru.spliterash.jenkinsVkNotifier.port.exception.VkException;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class SimpleVkSenderClient implements VkSender {
    private static final Random RANDOM = new Random();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .cookieHandler(new CookieHandler() {
                @Override
                public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) throws IOException {
                    return Collections.emptyMap();
                }

                @Override
                public void put(URI uri, Map<String, List<String>> responseHeaders) throws IOException {

                }
            })
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.of(30, ChronoUnit.SECONDS))
            .build();

    private final String token;


    @Override
    public void sendMessage(String peerId, String txt) {
        Map<String, String> parameters = new HashMap<>();

        parameters.put("access_token", token);
        parameters.put("v", "5.131");

        parameters.put("peer_id", peerId);
        parameters.put("message", txt);
        parameters.put("random_id", String.valueOf(RANDOM.nextLong()));

        String form = parameters.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        try {
            HttpResponse<String> result = CLIENT.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create("https://api.vk.com/method/messages.send"))
                            .POST(HttpRequest.BodyPublishers.ofString(form))
                            .build(), HttpResponse.BodyHandlers.ofString()
            );

            String body = result.body();
            if (body == null)
                throw new VkException("Response null");

            if (body.contains("\"error_code\":"))
                throw new VkException("Response have error: " + body);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
