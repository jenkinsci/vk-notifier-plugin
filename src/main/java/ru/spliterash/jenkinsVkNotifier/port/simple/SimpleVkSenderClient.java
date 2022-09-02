package ru.spliterash.jenkinsVkNotifier.port.simple;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import ru.spliterash.jenkinsVkNotifier.port.VkSender;
import ru.spliterash.jenkinsVkNotifier.port.exception.VkException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Log4j
@RequiredArgsConstructor
public class SimpleVkSenderClient implements VkSender {
    private static final int TIMEOUT = 1000 * 10;
    private final String token;


    private CloseableHttpClient createClient() {
        return HttpClients.custom()
                .disableCookieManagement()
                .setDefaultConnectionConfig(ConnectionConfig.DEFAULT)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(TIMEOUT)
                        .setConnectTimeout(TIMEOUT)
                        .setSocketTimeout(TIMEOUT)
                        .build())
                .build();
    }

    @Override
    public void sendMessage(String peerId, String txt) {
        List<NameValuePair> data = Arrays.asList(
                new BasicNameValuePair("access_token", token),
                new BasicNameValuePair("v", "5.131"),

                new BasicNameValuePair("peer_id", peerId),
                new BasicNameValuePair("message", txt),
                new BasicNameValuePair("random_id", String.valueOf(ThreadLocalRandom.current().nextLong()))
        );


        try (CloseableHttpClient client = createClient()) {
            HttpPost post = new HttpPost("https://api.vk.com/method/messages.send");
            post.setEntity(new UrlEncodedFormEntity(data, StandardCharsets.UTF_8));

            HttpEntity response = client.execute(post).getEntity();

            String body = IOUtils.toString(response.getContent(), StandardCharsets.UTF_8);
            if (body == null)
                throw new VkException("Response null");

            if (body.contains("\"error_code\":"))
                throw new VkException("Response have error: " + body);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
