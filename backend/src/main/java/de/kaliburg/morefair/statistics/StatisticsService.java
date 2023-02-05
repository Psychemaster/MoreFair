package de.kaliburg.morefair.statistics;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.kaliburg.morefair.MoreFairApplication;
import de.kaliburg.morefair.account.AccountEntity;
import de.kaliburg.morefair.game.round.LadderEntity;
import de.kaliburg.morefair.game.round.RankerEntity;
import de.kaliburg.morefair.game.round.RoundEntity;
import de.kaliburg.morefair.game.round.RoundService;
import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
@Service
@Slf4j
@Transactional
public class StatisticsService {

  private final MongoTemplate mongoTemplate;
  private final LoginRecordRepository loginRecordRepository;
  private final BiasRecordRepository biasRecordRepository;
  private final MultiRecordRepository multiRecordRepository;
  private final AutoPromoteRecordRepository autoPromoteRecordRepository;
  private final PromoteRecordRepository promoteRecordRepository;
  private final ThrowVinegarRecordRepository throwVinegarRecordRepository;

  @Lazy
  @Autowired
  private RoundService roundService;
  @Value("${spring.profiles.active}")
  private String activeProfile;
  @Value("${spring.datasource.password}")
  private String sqlPassword;
  @Value("${spring.datasource.username}")
  private String sqlUsername;

  @PostConstruct
  public void prepareCollections() {
    createCollection(LoginRecordEntity.class);
    createCollection(BiasRecordEntity.class);
    createCollection(MultiRecordEntity.class);
    createCollection(AutoPromoteRecordEntity.class);
    createCollection(PromoteRecordEntity.class);
    createCollection(ThrowVinegarRecordEntity.class);
  }

  private <T> void createCollection(Class<T> clazz) {
    if (!mongoTemplate.collectionExists(clazz)) {
      mongoTemplate.createCollection(clazz);
    }
  }

  public void recordLogin(AccountEntity account) {
    loginRecordRepository.save(new LoginRecordEntity(account));
  }

  /**
   * Records the moment before a user biases.
   *
   * @param ranker the Ranker of the user before they bias
   * @param ladder the ladder with all rankers before they multiply
   * @param round  the round with all ladders before they multiply
   */
  public void recordBias(RankerEntity ranker, LadderEntity ladder, RoundEntity round) {
    RankerRecord rankerRecord = new RankerRecord(ranker);
    LadderRecord ladderRecord = new LadderRecord(ladder);
    RoundRecord roundRecord = new RoundRecord(round);
    biasRecordRepository.save(new BiasRecordEntity(rankerRecord, ladderRecord, roundRecord));
  }

  /**
   * Records the moment before a user multiplies.
   *
   * @param ranker the Ranker of the user before they multiply
   * @param ladder the ladder with all rankers before they multiply
   * @param round  the round with all ladders before they multiply
   */
  public void recordMulti(RankerEntity ranker, LadderEntity ladder, RoundEntity round) {
    RankerRecord rankerRecord = new RankerRecord(ranker);
    LadderRecord ladderRecord = new LadderRecord(ladder);
    RoundRecord roundRecord = new RoundRecord(round);
    multiRecordRepository.save(new MultiRecordEntity(rankerRecord, ladderRecord, roundRecord));
  }

  /**
   * Records the moment before a user buys auto-promote.
   *
   * @param ranker the Ranker of the user before they buy auto-promote
   * @param ladder the ladder with all rankers before they buy auto-promote
   * @param round  the round with all ladders before they buy auto-promote
   */
  public void recordAutoPromote(RankerEntity ranker, LadderEntity ladder, RoundEntity round) {
    RankerRecord rankerRecord = new RankerRecord(ranker);
    LadderRecord ladderRecord = new LadderRecord(ladder);
    RoundRecord roundRecord = new RoundRecord(round);
    autoPromoteRecordRepository.save(
        new AutoPromoteRecordEntity(rankerRecord, ladderRecord, roundRecord));
  }

  /**
   * Records the moment before a user promotes to the next ladder.
   *
   * @param ranker the Ranker of the user before they promote to the next ladder
   * @param ladder the ladder with all rankers before they promote to the next ladder
   * @param round  the round with all ladders before they promote to the next ladder
   */
  public void recordPromote(RankerEntity ranker, LadderEntity ladder, RoundEntity round) {
    RankerRecord rankerRecord = new RankerRecord(ranker);
    LadderRecord ladderRecord = new LadderRecord(ladder);
    RoundRecord roundRecord = new RoundRecord(round);
    promoteRecordRepository.save(
        new PromoteRecordEntity(rankerRecord, ladderRecord, roundRecord));
  }

  /**
   * Records the moment before a user throws vinegar.
   *
   * @param ranker the Ranker of the user before they throw vinegar
   * @param ladder the ladder with all rankers before they throw vinegar
   * @param round  the round with all ladders before they throw vinegar
   */
  public void recordVinegarThrow(RankerEntity ranker, RankerEntity target, LadderEntity ladder,
      RoundEntity round) {
    RankerRecord rankerRecord = new RankerRecord(ranker);
    RankerRecord targetRecord = new RankerRecord(target);
    List<RankerEntity> rankers = ladder.getRankers();
    RankerRecord secondRecord = new RankerRecord(rankers.get(1));
    LadderRecord ladderRecord = new LadderRecord(ladder);
    RoundRecord roundRecord = new RoundRecord(round);
    throwVinegarRecordRepository.save(
        new ThrowVinegarRecordEntity(rankerRecord, targetRecord, secondRecord, ladderRecord,
            roundRecord));
  }

  /**
   * Sends a request to start the General Analytics for the Server.
   */
  @PostConstruct
  @Scheduled(cron = "0 */30 * * * *")
  public void startGeneralAnalytics() {
    startAnalytics("GeneralAnalytics", null);
  }

  /**
   * Sends a request to start the Round Statistics for the Server.
   */
  public void startRoundStatistics(long roundId) {
    startAnalytics("RoundStatistics", roundId);
  }

  // TODO: Delete this function once testing is through
  @PostConstruct
  public void debugStartup() {
    startRoundStatistics(1);
  }

  /**
   * Sends a Request to the local spark-master to start a new job. Sends the html request as a
   * completable future, so it doesn't block the thread.
   *
   * @param mainClass the main Class of the Spark Application that should be run on the cluster
   */
  public void startAnalytics(String mainClass, @Nullable Long currentRoundId) {
    try {
      // Create a JSON object for the request body
      JsonObject jsonBody = new JsonObject();
      jsonBody.addProperty("action", "CreateSubmissionRequest");

      // Add the "sparkProperties" property
      JsonObject sparkProperties = new JsonObject();
      sparkProperties.addProperty("spark.master", "spark://85.214.71.14:7077");
      sparkProperties.addProperty("spark.app.name", "Spark Live Test");
      sparkProperties.addProperty("spark.executor.memory", "8g");
      sparkProperties.addProperty("spark.driver.memory", "8g");
      sparkProperties.addProperty("spark.driver.cores", "2");
      jsonBody.add("sparkProperties", sparkProperties);

      // Add the other properties
      // TODO: make the path based on the current jar
      File jarFile =
          new File(
              MoreFairApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI()
                  .getPath());
      String path = jarFile.getAbsolutePath();

      jsonBody.addProperty("appResource", path + "/spark.jar");
      jsonBody.addProperty("clientSparkVersion", "3.3.1");
      jsonBody.addProperty("mainClass", mainClass);

      // Add the "environmentVariables" property
      JsonObject environmentVariables = new JsonObject();
      environmentVariables.addProperty("SPARK_ENV_LOADED", "1");
      environmentVariables.addProperty("SQL_USERNAME", sqlUsername);
      environmentVariables.addProperty("SQL_PASSWORD", sqlPassword);
      environmentVariables.addProperty("PROFILE", activeProfile);
      jsonBody.add("environmentVariables", environmentVariables);

      // Add the "appArgs" property
      JsonArray appArgs = new JsonArray();
      if (currentRoundId != null) {
        appArgs.add(currentRoundId);
      }
      jsonBody.add("appArgs", appArgs);

      // Convert the JSON object to a string
      Gson gson = new Gson();
      String jsonString = gson.toJson(jsonBody);

      RestTemplate restTemplate = new RestTemplate();
      String url = "http://85.214.71.14:6066/v1/submissions/create";

      // Send the request and get the response
      CompletableFuture.runAsync(() -> {
        try {
          restTemplate.postForObject(url, jsonString, String.class);
        } catch (Exception e) {
          log.error(e.getMessage());
        }
      });
    } catch (Exception e) {
      log.error(e.getMessage());
      e.printStackTrace();
    }
  }
}
