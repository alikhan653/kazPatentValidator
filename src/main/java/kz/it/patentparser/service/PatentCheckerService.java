package kz.it.patentparser.service;

import kz.it.patentparser.repository.DocNumberRepository;
import org.asynchttpclient.*;
import org.asynchttpclient.exception.TooManyConnectionsException;
import org.asynchttpclient.exception.TooManyConnectionsPerHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.asynchttpclient.Dsl.*;

@Service
public class PatentCheckerService {
    private static final Logger logger = LoggerFactory.getLogger(PatentCheckerService.class);
    private static final String BASE_URL = "https://gosreestr.kazpatent.kz";

//    private final AsyncHttpClient client;
//    private final ExecutorService executor;
//    private final ScheduledExecutorService scheduler;
//
//    private static final int MAX_CONNECTIONS = 50;
//    private static final int MAX_CONNECTIONS_PER_HOST = 10;
//    private static final int REQUEST_DELAY_MS = 200;
//    private static final int MAX_RETRIES = 5;
//    private final AtomicInteger failedCount = new AtomicInteger(0);
//
//    private final PatentStorageService patentStorageService;

//    public PatentCheckerService(PatentStorageService patentStorageService) {
//        this.client = asyncHttpClient(config()
//                .setMaxConnections(MAX_CONNECTIONS)
//                .setMaxConnectionsPerHost(MAX_CONNECTIONS_PER_HOST)
//                .setConnectTimeout(Duration.ofDays(20000))
//                .setRequestTimeout(Duration.ofDays(20000))
//                .setFollowRedirect(true)
//        );
//
//        this.executor = Executors.newFixedThreadPool(10); // Limits parallel requests
//        this.scheduler = Executors.newScheduledThreadPool(1);
//
//        this.patentStorageService = patentStorageService;
//    }
//
//    public void startProcessing(String category, int from, int to) {
//        Instant startTime = Instant.now();
//        logger.info("Starting patent check from {} to {}", from, to);
//        for (int docNumber = from; docNumber <= to; docNumber++) {
//            final int docNum = docNumber;
//            scheduler.schedule(() -> processPatent(category, docNum, 0, Instant.now()),
//                    (docNum - from) * REQUEST_DELAY_MS, TimeUnit.MILLISECONDS);
//        }
//
//        scheduler.schedule(() -> {
//            Duration totalDuration = Duration.between(startTime, Instant.now());
//            logger.info("Finished processing patents {} to {} in {} seconds",
//                    from, to, totalDuration.toSeconds());
//        }, (to - from + 1) * REQUEST_DELAY_MS, TimeUnit.MILLISECONDS);
//
//        shutdown();
//
//        try {
//            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
//            logger.info("✅ Processing completed!");
//            logger.info("❌ Total failed requests: " + failedCount.get());
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            logger.error("Error waiting for executor shutdown: " + e.getMessage());
//        }
//    }
//
//    private void processPatent(String category, int docNumber, int retryCount, Instant requestStartTime) {
//        String url = BASE_URL + "/" + category + "/Details?docNumber=" + docNumber;
//        client.prepareGet(url).execute(new AsyncCompletionHandler<>() {
//            @Override
//            public Response onCompleted(Response response) {
//                Instant requestEndTime = Instant.now();
//                Duration duration = Duration.between(requestStartTime, requestEndTime);
//
//                int statusCode = response.getStatusCode();
//                if (statusCode == 200) {
//                    if (response.getResponseBody().contains("divHolder")) {
//                        logger.info("[Request-{}] ✅ Patent found: {} (Time: {} ms)",
//                                docNumber, url, duration.toMillis());
//                        patentStorageService.saveDocNumber(category, String.valueOf(docNumber), false);
//                    } else {
//                        logger.warn("[Request-{}] Patent {} does not contain valid data (Time: {} ms)",
//                                docNumber, url, duration.toMillis());
//                    }
//                } else if (statusCode == 429) { // Rate limit hit
//                    int retryAfter = (int) Math.pow(2, retryCount); // Exponential backoff
//                    logger.warn("[Request-{}] ⚠ Rate limited (429), retrying in {}s...", docNumber, retryAfter);
//                    scheduler.schedule(() -> processPatent(category, docNumber, retryCount + 1, Instant.now()),
//                            retryAfter, TimeUnit.SECONDS);
//                } else if (statusCode == 500) { // Stop further processing
//                    logger.warn("[Request-{}] Patent {} not found (500) (Time: {} ms)",
//                            docNumber, url, duration.toMillis());
//                } else {
//                    logger.error("[Request-{}] Unexpected response {}: {} (Time: {} ms)",
//                            docNumber, statusCode, response.getStatusText(), duration.toMillis());
//                    failedCount.incrementAndGet(); // Track failed attempts
//                }
//                return response;
//            }
//
//            @Override
//            public void onThrowable(Throwable t) {
//                if (t instanceof TooManyConnectionsException || t instanceof TooManyConnectionsPerHostException) {
//                    int retryAfter = (int) Math.pow(2, retryCount); // Exponential backoff
//                    if (retryCount < MAX_RETRIES) {
//                        logger.error("[Request-{}] Too many connections, retrying in {}s... (Attempt {}/{})",
//                                docNumber, retryAfter, retryCount + 1, MAX_RETRIES);
//                        scheduler.schedule(() -> processPatent(category, docNumber, retryCount + 1, Instant.now()),
//                                retryAfter, TimeUnit.SECONDS);
//                    } else {
//                        logger.error("[Request-{}] Too many connections, max retries reached. Skipping.", docNumber);
//                        failedCount.incrementAndGet(); // Track failed attempts
//                    }
//                } else {
//                    logger.error("[Request-{}] Request failed: {}", docNumber, t.getMessage());
//                    failedCount.incrementAndGet(); // Track failed attempts
//                }
//            }
//        });
//    }
//
//    public void shutdown() {
//        try {
//            client.close();
//            executor.shutdown();
//            scheduler.shutdown();
//        } catch (Exception e) {
//            logger.error("Error shutting down services", e);
//        }
//    }
}
