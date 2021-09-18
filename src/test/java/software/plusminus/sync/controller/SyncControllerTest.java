package software.plusminus.sync.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import company.plusminus.json.model.Classable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import software.plusminus.check.util.JsonUtils;
import software.plusminus.hibernate.HibernateFilterInterceptor;
import software.plusminus.sync.TestEntity;
import software.plusminus.sync.dto.Sync;
import software.plusminus.sync.dto.SyncType;
import software.plusminus.sync.service.AuditSyncService;
import software.plusminus.sync.service.SyncService;
import software.plusminus.util.ResourceUtils;

import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static software.plusminus.check.Checks.check;

@RunWith(SpringRunner.class)
// AuditSyncService must be registered because of @ConditionalOnBean(SyncService.class) on SyncController
@WebMvcTest(controllers = {SyncController.class, AuditSyncService.class})
@ActiveProfiles("test")
public class SyncControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private SyncService service;
    @MockBean
    private HibernateFilterInterceptor filterInterceptor;

    @Before
    public void setUp() {
        when(filterInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }
    
    @Test
    public void read() throws Exception {
        List<Sync<? extends Classable>> syncs = singletonList(Sync.of(
                JsonUtils.fromJson("/json/test-entity.json", TestEntity.class),
                SyncType.CREATE));
        when(service.read(singletonList("TestEntity"), "TestDevice", 20L, 30, Sort.Direction.DESC))
                .thenReturn(syncs);

        String body = mvc
                .perform(get("/sync?types=TestEntity"
                        + "&ignoreDevice=TestDevice&fromAuditNumber=20&size=30&direction=DESC"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        check(body).isJson().is(syncs);
    }

    @Test
    public void readWithDefaultParameters() throws Exception {
        when(service.read(singletonList("TestEntity"), null, 0L, 20, Sort.Direction.ASC))
                .thenReturn(Collections.emptyList());

        String body = mvc.perform(get("/sync?types=TestEntity"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        check(body).is("[]");
    }

    @Test
    public void write() throws Exception {
        String json = ResourceUtils.toString("/json/test-entity.json");
        TestEntity entity = JsonUtils.fromJson(json, TestEntity.class);
        List<Sync<? extends Classable>> items = singletonList(toSyncItem(entity));
        List<TestEntity> entities = singletonList(entity);
        doReturn(entities).when(service).write(items);

        String body = mvc.perform(post("/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(items)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        check(body).is('[' + json + ']');
    }

    private <T extends Classable> Sync<T> toSyncItem(T object) {
        Sync<T> sync = new Sync<>();
        sync.setType(SyncType.CREATE);
        sync.setObject(object);
        return sync;
    }
}