package de.kaliburg.morefair.api;

import de.kaliburg.morefair.account.AccountDetailsDto;
import de.kaliburg.morefair.account.AccountEntity;
import de.kaliburg.morefair.account.AccountService;
import de.kaliburg.morefair.account.AchievementsEntity;
import de.kaliburg.morefair.api.utils.HttpUtils;
import de.kaliburg.morefair.api.utils.WsUtils;
import de.kaliburg.morefair.events.Event;
import de.kaliburg.morefair.events.types.AccountEventTypes;
import de.kaliburg.morefair.game.round.RankerService;
import de.kaliburg.morefair.game.round.RoundEntity;
import de.kaliburg.morefair.game.round.RoundService;
import de.kaliburg.morefair.security.SecurityUtils;
import de.kaliburg.morefair.statistics.StatisticsService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Controller
@Slf4j
@RequestMapping("/api/account")
@RestController
@RequiredArgsConstructor
public class AccountController {

  public static final String TOPIC_EVENTS_DESTINATION = "/account/events";
  public static final String PRIVATE_EVENTS_DESTINATION = "/account/events";
  private static final String APP_RENAME_DESTINATION = "/account/name";
  private final AccountService accountService;
  private final WsUtils wsUtils;
  private final RoundService roundService;
  private final RankerService rankerService;
  private final StatisticsService statisticsService;


  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> getAccount(Authentication authentication) {
    try {
      AccountEntity account = accountService.find(SecurityUtils.getUuid(authentication));

      if (account == null) {
        return ResponseEntity.notFound().build();
      }

      if (account.getAchievements() == null) {
        account.setAchievements(new AchievementsEntity(account));
        accountService.save(account);
      }

      statisticsService.recordLogin(account);
      RoundEntity currentRound = roundService.getCurrentRound();
      int highestLadder = rankerService.findCurrentRankersOfAccount(account, currentRound).stream()
          .mapToInt(r -> r.getLadder().getNumber()).max().orElse(1);

      return ResponseEntity.ok(new AccountDetailsDto(account, highestLadder));
    } catch (Exception e) {
      log.error(e.getMessage());
      e.printStackTrace();
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }


  @PatchMapping("/name")
  public ResponseEntity<?> updateDisplayName(Authentication authentication,
      @RequestParam("displayName") String displayName) {
    try {
      while (displayName.contains("[BANNED]") || displayName.contains("[MUTED]")) {
        displayName = displayName.replace("[BANNED]", "");
        displayName = displayName.replace("[MUTED]", "");
      }

      displayName = displayName.trim();
      if (displayName.length() > 32) {
        displayName = displayName.substring(0, 32);
      }

      if (displayName.isBlank()) {
        return HttpUtils.buildErrorMessage(HttpStatus.BAD_REQUEST, "Display name cannot be blank");
      }

      AccountEntity account = accountService.find(SecurityUtils.getUuid(authentication));

      if (displayName.equals(account.getDisplayName())) {
        Map<String, String> result = new HashMap<>();
        result.put("displayName", displayName);
        return ResponseEntity.ok(result);
      }

      log.info("[G] RENAME: {} (#{}) -> {}", account.getDisplayName(), account.getId(),
          displayName);

      account.setDisplayName(displayName);
      accountService.save(account);

      wsUtils.convertAndSendToTopic(AccountController.TOPIC_EVENTS_DESTINATION,
          new Event<>(AccountEventTypes.NAME_CHANGE, account.getId(),
              account.getDisplayName()));

      Map<String, String> result = new HashMap<>();
      result.put("displayName", displayName);
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error(e.getMessage());
      e.printStackTrace();
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }


}
