package software.plusminus.sync.dehydration;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import software.plusminus.jwt.service.IssuerService;
import software.plusminus.jwt.service.JwtGenerator;
import software.plusminus.security.Security;
import software.plusminus.sync.TransactionalService;
import software.plusminus.tenant.service.TenantService;
import software.plusminus.test.IntegrationTest;
import software.plusminus.util.ResourceUtils;

import java.util.Collections;
import java.util.UUID;
import java.util.stream.Stream;
import javax.persistence.EntityManager;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static software.plusminus.check.Checks.check;

public class DehydrationIntegrationTest extends IntegrationTest {

    @Autowired
    private TransactionalService transactionalService;
    @Autowired
    private JwtGenerator jwtGenerator;
    @Autowired
    private EntityManager entityManager;
    private RestTemplate restTemplate = new RestTemplate();

    @SpyBean
    private TenantService tenantService;
    @SpyBean
    private IssuerService issuerService;

    private A entityA;
    private B entityB;
    private C entityC;

    @Before
    public void before() {
        entityA = new A();
        entityA.setId(1L);
        entityA.setName("a");
        entityA.setUuid(UUID.randomUUID());
        entityB = new B();
        entityB.setId(2L);
        entityB.setName("b");
        entityB.setUuid(UUID.randomUUID());
        entityC = new C();
        entityC.setId(3L);
        entityC.setName("c");
        entityC.setUuid(UUID.randomUUID());

        entityA.setEntityC(entityC);
        entityB.setEntityA(entityA);

        doReturn("localhost")
                .when(tenantService).currentTenant();
        transactionalService.inTransaction(() -> Stream.of(entityA, entityB, entityC)
                .forEach(entityManager::persist));
        doCallRealMethod()
                .when(tenantService).currentTenant();
    }

    @Test
    public void dehydratedResponse() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + generateToken());

        String body = restTemplate.exchange(
                "http://localhost:" + port() + "/sync?types=A&types=B&types=C"
                        + "&excludeCurrentDevice=false"
                        + "&dehydrate=true", HttpMethod.GET, new HttpEntity<>(headers),
                String.class).getBody();

        String expected = String.format(
                ResourceUtils.toString("/dehydrated.json"),
                entityA.getUuid(),
                entityB.getUuid(),
                entityC.getUuid(),
                entityB.getUuid(),
                entityA.getUuid(),
                entityC.getUuid(),
                entityA.getUuid()
        );
        check(body).is(expected);
    }

    private String generateToken() {
        Security security = Security.builder()
                .others(Collections.singletonMap("tenant", "localhost"))
                .build();
        doReturn("localhost")
                .when(issuerService).currentIssuer();
        String token = jwtGenerator.generateAccessToken(security);
        doCallRealMethod()
                .when(issuerService).currentIssuer();
        return token;
    }
}
