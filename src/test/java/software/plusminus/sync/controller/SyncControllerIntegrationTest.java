package software.plusminus.sync.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import software.plusminus.check.util.JsonUtils;
import software.plusminus.jwt.service.IssuerService;
import software.plusminus.jwt.service.JwtGenerator;
import software.plusminus.security.Security;
import software.plusminus.security.context.SecurityContext;
import software.plusminus.sync.TestEntity;
import software.plusminus.sync.TransactionalService;
import software.plusminus.sync.dto.Sync;
import software.plusminus.sync.dto.SyncType;
import software.plusminus.sync.models.Product;
import software.plusminus.sync.models.ProductOutcome;
import software.plusminus.tenant.util.TenantUtils;
import software.plusminus.test.IntegrationTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.persistence.EntityManager;
import javax.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static software.plusminus.check.Checks.check;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SyncControllerIntegrationTest extends IntegrationTest {

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
    private TransactionalService transactionalService;
    @Autowired
    private JwtGenerator generator;
    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private SecurityContext securityContext;
    @SpyBean
    private IssuerService issuerService;

    private TestEntity entity1;
    private TestEntity entity2;
    @SuppressWarnings("squid:S1450")
    private TestEntity entityWithUnknownTenant;
    @SuppressWarnings("squid:S1450")
    private TestEntity entityWithoutTenant;
    private TestEntity entitySoftlyDeleted;

    @Before
    public void before() {
        entity1 = readTestEntity();
        entity1.setId(null);
        entity1.setVersion(null);
        entity1.setTenant(TENANT);
        persist(OTHER_DEVICE, entity1);

        entity2 = readTestEntity();
        entity2.setId(null);
        entity2.setVersion(null);
        entity2.setTenant(TENANT);
        persist(OTHER_DEVICE, entity2);

        entityWithUnknownTenant = readTestEntity();
        entityWithUnknownTenant.setId(null);
        entityWithUnknownTenant.setVersion(null);
        entityWithUnknownTenant.setTenant("Unknown tenant");
        persist(OTHER_DEVICE, entityWithUnknownTenant);

        entityWithoutTenant = readTestEntity();
        entityWithoutTenant.setId(null);
        entityWithoutTenant.setVersion(null);
        entityWithoutTenant.setTenant("");
        persist(OTHER_DEVICE, entityWithoutTenant);

        entitySoftlyDeleted = readTestEntity();
        entitySoftlyDeleted.setId(null);
        entitySoftlyDeleted.setVersion(null);
        entitySoftlyDeleted.setTenant(TENANT);
        entitySoftlyDeleted.setDeleted(Boolean.TRUE);
        persist(OTHER_DEVICE, entitySoftlyDeleted);
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
        entity2.setMyField("updated");
        entity2 = merge(OTHER_DEVICE, entity2);

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
        remove(OTHER_DEVICE, entity2);

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
        String json = mapper.writerFor(new TypeReference<List<Sync<TestEntity>>>() {
        })
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

    @Test
    public void turnBackExistingObjectWithInnerEntityOnCreate() throws Exception {
        UUID uuid = UUID.randomUUID();
        Product productIndDb = new Product();
        productIndDb.setUuid(uuid);
        run(TENANT, CURRENT_DEVICE, () -> entityManager.persist(productIndDb));
        ProductOutcome productOutcomeInDb = new ProductOutcome();
        productOutcomeInDb.setUuid(uuid);
        productOutcomeInDb.setProduct(productIndDb);
        run(TENANT, CURRENT_DEVICE, () -> entityManager.persist(productOutcomeInDb));

        Product product = new Product();
        product.setUuid(uuid);
        product.setTenant(TENANT);

        String json = objectMapper.writeValueAsString(
                Collections.singletonList(Sync.of(product, SyncType.CREATE, null)));
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
    }

    private TestEntity readTestEntity() {
        return JsonUtils.fromJson("/json/entity.json", TestEntity.class);
    }

    private Cookie authenticationCookie() {
        doReturn("localhost")
                .when(issuerService).currentIssuer();
        String token = generator.generateAccessToken(security(TENANT, CURRENT_DEVICE));
        doCallRealMethod()
                .when(issuerService).currentIssuer();
        return new Cookie("JWT-TOKEN", token);
    }

    private Security security(String tenant, String device) {
        return Security.builder()
                .username("TestUser")
                .others(ImmutableMap.of("tenant", tenant, "device", device))
                .build();
    }

    private void persist(String device, TestEntity entity) {
        run(entity.getTenant(), device, () -> entityManager.persist(entity));
    }

    private TestEntity merge(String device, TestEntity entity) {
        AtomicReference<TestEntity> container = new AtomicReference<>();
        run(entity.getTenant(), device, () -> {
            TestEntity merged = entityManager.merge(entity);
            container.set(merged);
        });
        return container.get();
    }

    private void remove(String device, TestEntity entity) {
        run(entity.getTenant(), device, () -> entityManager.remove(entityManager.merge(entity)));
    }

    private void run(String tenant, String device, Runnable runnable) {
        when(securityContext.get("tenant")).thenReturn(tenant);
        when(securityContext.get("device")).thenReturn(device);
        transactionalService.inTransaction(() ->
                TenantUtils.runWithTenant(entityManager, tenant, runnable));
        reset(securityContext);
    }
}