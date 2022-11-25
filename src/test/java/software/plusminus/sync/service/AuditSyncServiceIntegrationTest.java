package software.plusminus.sync.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import software.plusminus.data.repository.DataRepository;
import software.plusminus.sync.InnerEntity;
import software.plusminus.sync.TestEntity;
import software.plusminus.sync.dto.Sync;
import software.plusminus.sync.dto.SyncType;
import software.plusminus.sync.models.Product;
import software.plusminus.sync.models.ProductOutcome;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class AuditSyncServiceIntegrationTest {

    @Autowired
    private AuditSyncService syncService;
    @Autowired
    private DataRepository dataRepository;
    
    @Test
    public void createObjectWithInnerEntity() {
        Product product = new Product();
        ProductOutcome productOutcome = new ProductOutcome();
        productOutcome.setProduct(product);

        List<?> result = syncService.write(Arrays.asList(
                Sync.of(productOutcome, SyncType.CREATE, null),
                Sync.of(product, SyncType.CREATE, null)));
        
        assertThat(result).asList().containsExactly(productOutcome, product);
        assertThat(productOutcome.getId()).isEqualTo(1L);
        assertThat(productOutcome.getProduct().getId()).isEqualTo(1L);
        assertThat(product.getId()).isEqualTo(1L);
    }
    
    @Test
    public void turnBackObjectWithInnerEntityOnUpdate() {
        Product productIndDb = new Product();
        dataRepository.save(productIndDb);
        ProductOutcome productOutcomeInDb = new ProductOutcome();
        productOutcomeInDb.setProduct(productIndDb);
        dataRepository.save(productOutcomeInDb);
        
        Product product = new Product();
        product.setId(1L);
        product.setVersion(0L);
        product.setEntries(productIndDb.getEntries());
        ProductOutcome productOutcome = new ProductOutcome();
        productOutcome.setId(1L);
        productOutcome.setProduct(product);

        List<?> result = syncService.write(Arrays.asList(
                Sync.of(productOutcome, SyncType.UPDATE, null),
                Sync.of(product, SyncType.UPDATE, null)));
        
        assertThat(result).hasSize(2);
        assertThat(productOutcome.getId()).isEqualTo(1L);
        assertThat(productOutcome.getProduct().getId()).isEqualTo(1L);
        assertThat(product.getId()).isEqualTo(1L);
    }
    
    @Test
    public void updateWithInnerDependency() {
        Product productIndDb = new Product();
        dataRepository.save(productIndDb);
        ProductOutcome productOutcomeInDb = new ProductOutcome();
        productOutcomeInDb.setProduct(productIndDb);
        dataRepository.save(productOutcomeInDb);

        Product product = new Product();
        product.setId(1L);
        product.setVersion(0L);
        product.setEntries(Collections.emptyList());

        List<?> result = syncService.write(Collections.singletonList(
                Sync.of(product, SyncType.UPDATE, null)));

        assertThat(result).hasSize(1);
        assertThat(product.getId()).isEqualTo(1L);
    }
    
    @Test
    public void updateTwoObjectsWithSameInnerEntity() {
        InnerEntity innerEntity = new InnerEntity();
        innerEntity = dataRepository.save(innerEntity);
        TestEntity entityOne = new TestEntity();
        entityOne.setInnerEntity(innerEntity);
        entityOne = dataRepository.save(entityOne);
        TestEntity entityTwo = new TestEntity();
        entityTwo.setInnerEntity(innerEntity);
        entityTwo = dataRepository.save(entityTwo);

        List<TestEntity> result = (List<TestEntity>) syncService.write(Arrays.asList(
                Sync.of(entityOne, SyncType.UPDATE, null),
                Sync.of(entityTwo, SyncType.UPDATE, null)));

        assertThat(result.get(0).getInnerEntity()).isSameAs(result.get(1).getInnerEntity());
    }
}