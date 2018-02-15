package uk.gov.hmcts.reform.sendletter.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.sendletter.model.DbLetter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static java.sql.Timestamp.from;
import static java.util.Objects.nonNull;

@Repository
public class LetterRepository {

    private static final Logger log = LoggerFactory.getLogger(LetterRepository.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper;

    public LetterRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void save(DbLetter letter, Instant messageSendTime, String messageId) throws JsonProcessingException {
        jdbcTemplate.update(
            "INSERT INTO letters (id, message_id, service, created_at, sent_to_print_at, printed_at, additional_data)"
                + "VALUES (:id, :messageId, :service, :createdAt, :sentToPrintAt, :printedAt, :additionalData::JSON)",
            new MapSqlParameterSource()
                .addValue("id", letter.id)
                .addValue("messageId", messageId)
                .addValue("service", letter.service)
                .addValue("createdAt", from(messageSendTime))
                .addValue("additionalData", convertToJson(letter.additionalData))
                .addValue("sentToPrintAt", null)
                .addValue("printedAt", null)
        );
        log.info("Successfully saved letter data into database with id : {} and messageId :{}", letter.id, messageId);
    }

    /**
     * Updates the `sent_to_print_at` column on letter(s) with given id.
     *
     * @return number of updated rows.
     */
    public int updateSentToPrintAt(UUID id, LocalDateTime dateTime) {
        return jdbcTemplate.update(
            "UPDATE letters SET sent_to_print_at = :sentToPrintAt WHERE id = :id",
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("sentToPrintAt", dateTime)
        );
    }

    /**
     * Updates the `printed_at` column on letter(s) with given id.
     *
     * @return number of updated rows.
     */
    public int updatePrintedAt(UUID id, LocalDateTime dateTime) {
        return jdbcTemplate.update(
            "UPDATE letters SET printed_at = :printedAt WHERE id = :id",
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("printedAt", dateTime)
        );
    }

    private String convertToJson(Map<String, Object> additionalData) throws JsonProcessingException {
        if (nonNull(additionalData)) {
            return objectMapper.writeValueAsString(additionalData);
        }
        return null;
    }
}
