package software.plusminus.sync.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import software.plusminus.authentication.AuthenticationParameters;
import software.plusminus.authentication.AuthenticationService;
import software.plusminus.check.exception.JsonException;
import software.plusminus.check.util.JsonUtils;
import software.plusminus.context.Context;
import software.plusminus.security.properties.SecurityProperties;
import software.plusminus.sync.TestEntity;
import software.plusminus.sync.TransactionalService;
import software.plusminus.sync.dto.Sync;
import software.plusminus.sync.dto.SyncType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManager;
import javax.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static software.plusminus.check.Checks.check;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class SyncControllerIntegrationTest {

    private static final String TENANT = "localhost";
    private static final String CURRENT_DEVICE = "CurrentDevice";
    private static final String OTHER_DEVICE = "OtherDevice";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private SecurityProperties securityProperties;
    @Autowired
    private TransactionalService transactionalService;
    @Autowired
    private Context<AuthenticationParameters> securityContext;

    private TestEntity entity1;
    private TestEntity entity2;
    @SuppressWarnings("squid:S1450")
    private TestEntity entityWithUnknownTenant;
    @SuppressWarnings("squid:S1450")
    private TestEntity entityWithoutTenant;
    private TestEntity entitySoftlyDeleted;

    @Before
    public void before() {
        securityContext.set(authenticationParameters(TENANT, OTHER_DEVICE));
        
        entity1 = readTestEntity();
        entity1.setId(null);
        entity1.setVersion(null);
        entity1.setTenant(TENANT);
        transactionalService.inTransaction(() -> entityManager.persist(entity1));

        entity2 = readTestEntity();
        entity2.setId(null);
        entity2.setVersion(null);
        entity2.setTenant(TENANT);
        transactionalService.inTransaction(() -> entityManager.persist(entity2));

        entityWithUnknownTenant = readTestEntity();
        entityWithUnknownTenant.setId(null);
        entityWithUnknownTenant.setVersion(null);
        entityWithUnknownTenant.setTenant("Unknown tenant");
        securityContext.set(authenticationParameters("Unknown tenant", OTHER_DEVICE));
        transactionalService.inTransaction(() -> entityManager.persist(entityWithUnknownTenant));

        entityWithoutTenant = readTestEntity();
        entityWithoutTenant.setId(null);
        entityWithoutTenant.setVersion(null);
        entityWithoutTenant.setTenant("");
        securityContext.set(authenticationParameters("", OTHER_DEVICE));
        transactionalService.inTransaction(() -> entityManager.persist(entityWithoutTenant));

        entitySoftlyDeleted = readTestEntity();
        entitySoftlyDeleted.setId(null);
        entitySoftlyDeleted.setVersion(null);
        entitySoftlyDeleted.setTenant(TENANT);
        entitySoftlyDeleted.setDeleted(Boolean.TRUE);
        securityContext.set(authenticationParameters(TENANT, OTHER_DEVICE));
        transactionalService.inTransaction(() -> entityManager.persist(entitySoftlyDeleted));

        securityContext.set(null);
    }

    @Test
    public void read() throws Exception {
        List<Sync<TestEntity>> actions = Arrays.asList(
                Sync.of(entitySoftlyDeleted, SyncType.CREATE, 5L),
                Sync.of(entity2, SyncType.CREATE, 2L));

        String body = mvc
                .perform(get("/sync?types=TestEntity&excludeCurrentDevice=false&offset=1&size=10&direction=DESC")
                        .cookie(authenticationCookie()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        check(body).isJson().is(actions);
    }

    @Test
    public void readWithDefaultParameters() throws Exception {
        List<Sync<TestEntity>> actions = Arrays.asList(
                Sync.of(entity1, SyncType.CREATE, 1L),
                Sync.of(entity2, SyncType.CREATE, 2L),
                Sync.of(entitySoftlyDeleted, SyncType.CREATE, 5L));

        String body = mvc.perform(get("/sync?types=TestEntity")
                .cookie(authenticationCookie()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        check(body).isJson().is(actions);
    }

    @Test
    public void readUpdated() throws Exception {
        securityContext.set(authenticationParameters(TENANT, OTHER_DEVICE));
        entity2.setMyField("updated");
        transactionalService.inTransaction(() -> entity2 = entityManager.merge(entity2));
        securityContext.set(null);

        List<Sync<TestEntity>> actions = Collections.singletonList(
                Sync.of(entity2, SyncType.UPDATE, 6L));

        String body = mvc.perform(get("/sync?types=TestEntity&offset=5")
                .cookie(authenticationCookie()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        check(body).isJson().is(actions);
    }

    @Test
    public void readDeleted() throws Exception {
        securityContext.set(authenticationParameters(TENANT, OTHER_DEVICE));
        transactionalService.inTransaction(() ->
                entityManager.remove(entityManager.merge(entity2)));
        securityContext.set(null);

        String body = mvc.perform(get("/sync?types=TestEntity&offset=5")
                .cookie(authenticationCookie()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        check(body).is("/json/deleted.json");
    }

    @Test
    public void write() throws Exception {
        TestEntity entityOne = readTestEntity();
        entityOne.setId(1L);
        entityOne.setVersion(0L);
        entityOne.setTenant(TENANT);

        TestEntity entityTwo = readTestEntity();
        entityTwo.setId(2L);
        entityTwo.setVersion(0L);
        entityTwo.setTenant(TENANT);

        TestEntity newEntity = new TestEntity();
        newEntity.setMyField("new entity field");

        List<Sync<TestEntity>> items = Arrays.asList(
                Sync.of(entityOne, SyncType.UPDATE, null),
                Sync.of(entityTwo, SyncType.DELETE, null),
                Sync.of(newEntity, SyncType.CREATE, null));
        String json = mapper.writerFor(new TypeReference<List<Sync<TestEntity>>>() {})
                .writeValueAsString(items);

        String body = mvc.perform(post("/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .cookie(authenticationCookie()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).isNotEmpty();
        check(body).is("/json/write-response.json");
    }
    
    @Test
    public void writeWithDependencies() throws Exception {
        String json = JsonUtils.readJson("/json/write-request-with-inner-entity.json");

        String body = mvc.perform(post("/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .cookie(authenticationCookie()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).isNotEmpty();
        check(body).is("/json/write-response-with-dependencies.json");
    }

    private TestEntity readTestEntity() {
        return JsonUtils.fromJson("/json/entity.json", TestEntity.class);
    }

    private Cookie authenticationCookie() {
        String token = authenticationService.generateToken(authenticationParameters(TENANT, CURRENT_DEVICE));
        return new Cookie(securityProperties.getCookieName(), token);
    }

    private AuthenticationParameters authenticationParameters(String tenant, String device) {
        return AuthenticationParameters.builder()
                .username("TestUser")
                .otherParameters(ImmutableMap.of("tenant", tenant, "device", device))
                .build();
    } 

    public static <T> List<T> fromJsonList(String json, Class<T[]> type) {
        T[] array;
        try {
            json = JsonUtils.readJson(json);
            array = createObjectMapper().readValue(json, type);
        } catch (JsonProcessingException var4) {
            throw new JsonException(var4);
        }

        return Arrays.asList(array);
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setDateFormat(new ISO8601DateFormat());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}