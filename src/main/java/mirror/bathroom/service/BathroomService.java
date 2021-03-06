package mirror.bathroom.service;

import mirror.bathroom.client.generic.LightBulbClient;
import mirror.bathroom.dao.BathroomSittingDto;
import mirror.bathroom.dao.BathroomSittingDao;
import mirror.bathroom.model.response.BathroomResponse;
import mirror.bathroom.service.generic.BathroomResponseConverter;
import mirror.bathroom.service.sitting.BathroomSittingStatsResponse;
import mirror.telegram.service.TelegramBotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

public class BathroomService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BathroomService.class);
    private final LightBulbClient client;
    private final BathroomResponseConverter responseConverter;
    private final TelegramBotService telegramBotService;
    private final BathroomSittingDao bathroomSittingDao;
    private boolean registeredForOneOffAlert;

    public BathroomService(LightBulbClient client, BathroomResponseConverter responseConverter,
                           TelegramBotService botService, BathroomSittingDao bathroomSittingDao) {
        this.client = client;
        this.responseConverter = responseConverter;
        this.telegramBotService = botService;
        this.bathroomSittingDao = bathroomSittingDao;
        this.registeredForOneOffAlert = false;
    }

    public BathroomResponse getBathroomState() {
        LOGGER.debug("Getting bathroom state");
        return responseConverter.convertResponse(client.getLightState());
    }

    public void alertIfRegistered(String state) {
        botSendMessage(state);
    }

    private void botSendMessage(String state) {
        if (registeredForOneOffAlert) {
            String message = "Congratulations!!! The bathroom is now " + state + "=)))))";
            telegramBotService.broadcastMessageBathroomBot(message);

            registeredForOneOffAlert = false;
        }
    }

    public void signalUrgent() {
        LOGGER.info("Signalling urgent");
        registerForOneOffAlert();
        client.breatheEffectRed();
    }

    private void registerForOneOffAlert() {
        LOGGER.info("Registering for one off alert");
        registeredForOneOffAlert = true;
    }

    public void saveSitting(BathroomSittingDto sitting) {
        LOGGER.info("Logging sitting: {} to {}", sitting.getStartTime(), sitting.getEndTime());
        bathroomSittingDao.insertBathroomSitting(sitting);
    }

    public BathroomSittingStatsResponse getSittingStatsLastWeek() {
        LocalDateTime endTime = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime startTime = endTime.minusDays(7);
        List<BathroomSittingDto> sittings = bathroomSittingDao.getSittingsInRange(startTime, endTime);

        return new BathroomSittingStatsResponse(sittings);
    }
}
