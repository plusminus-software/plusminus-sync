package software.plusminus.sync.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import software.plusminus.data.repository.DataRepository;
import software.plusminus.data.service.data.DataService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class AuditSyncServiceIntegrationTest {

    @Autowired
    private AuditSyncService syncService;
    @Autowired
    private DataRepository dataRepository;
    @SpyBean
    private DataService dataService;
    
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
        verify(dataService).create(product);
        verify(dataService).create(productOutcome);
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
        ProductOutcome productOutcome = new ProductOutcome();
        productOutcome.setId(1L);
        productOutcome.setProduct(product);
        product.setEntries(Collections.singletonList(productOutcome));

        List<?> result = syncService.write(Arrays.asList(
                Sync.of(productOutcome, SyncType.UPDATE, null),
                Sync.of(product, SyncType.UPDATE, null)));
        
        assertThat(result).hasSize(2);
        assertThat(productOutcome.getId()).isEqualTo(1L);
        assertThat(productOutcome.getProduct().getId()).isEqualTo(1L);
        assertThat(product.getId()).isEqualTo(1L);
        verify(dataService, never()).update(any());
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
//        product.setVersion(0L);
        product.setEntries(Collections.emptyList());

        List<?> result = syncService.write(Collections.singletonList(
                Sync.of(product, SyncType.UPDATE, null)));

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isOfAnyClassIn(Product.class);
        Product resultProduct = (Product) result.get(0); 
        assertThat(resultProduct.getId()).isEqualTo(1L);
        assertThat(resultProduct.getVersion()).isEqualTo(1L);
        verify(dataService).update(product);
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