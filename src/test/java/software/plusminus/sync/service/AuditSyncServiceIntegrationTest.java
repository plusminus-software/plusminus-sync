package software.plusminus.sync.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import software.plusminus.data.repository.DataRepository;
import software.plusminus.data.service.data.DataService;
import software.plusminus.sync.InnerEntity;
import software.plusminus.sync.TestEntity;
import software.plusminus.sync.TransactionalService;
import software.plusminus.sync.dto.Sync;
import software.plusminus.sync.dto.SyncType;
import software.plusminus.sync.models.Product;
import software.plusminus.sync.models.ProductOutcome;
import software.plusminus.test.IntegrationTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static software.plusminus.check.Checks.check;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AuditSyncServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private AuditSyncService syncService;
    @Autowired
    private DataRepository dataRepository;
    @Autowired
    private TransactionalService transactionalService;
    @SpyBean
    private DataService dataService;
    
    @Test
    public void createObjectWithInnerEntity() {
        Product product = new Product();
        ProductOutcome productOutcome = new ProductOutcome();
        productOutcome.setProduct(product);
        product.setEntries(Collections.singletonList(productOutcome));

        List<?> result = syncService.write(Arrays.asList(
                Sync.of(productOutcome, SyncType.CREATE, null, null),
                Sync.of(product, SyncType.CREATE, null, null)));
        
        assertThat(result).asList().containsExactly(productOutcome, product);
        verify(dataService).create(product);
        verify(dataService).create(productOutcome);
        transactionalService.inTransaction(() -> {
            check(result.get(0))
                    .as(ProductOutcome.class)
                    .is(dataRepository.findById(ProductOutcome.class, productOutcome.getId()));
            check(result.get(1))
                    .as(Product.class)
                    .is(dataRepository.findById(Product.class, product.getId()));
        });
    }
    
    @Test
    public void replaceVersionIfObjectsEqualIgnoringVersionField() {
        Product productInDb = new Product();
        productInDb.setName("my name");
        dataRepository.save(productInDb);
        ProductOutcome productOutcomeInDb = new ProductOutcome();
        productOutcomeInDb.setProduct(productInDb);
        dataRepository.save(productOutcomeInDb);
        productInDb.setEntries(Collections.singletonList(productOutcomeInDb));
        
        Product product = new Product();
        product.setName("my name");
        product.setId(productInDb.getId());
        product.setVersion(22L);
        ProductOutcome productOutcome = new ProductOutcome();
        productOutcome.setId(productOutcomeInDb.getId());
        productOutcome.setProduct(product);
        productOutcome.setVersion(33L);
        product.setEntries(Collections.singletonList(productOutcome));

        List<?> result = syncService.write(Arrays.asList(
                Sync.of(productOutcome, SyncType.UPDATE, null, null),
                Sync.of(product, SyncType.UPDATE, null, null)));
        
        assertThat(result).hasSize(2);
        check(result.get(0)).as(ProductOutcome.class)
                .is(productOutcomeInDb);
        check(((ProductOutcome) result.get(0)).getVersion()).is(0L);
        check(result.get(1)).as(Product.class)
                .is(productInDb);
        check(((Product) result.get(1)).getVersion()).is(0L);
    }
    
    @Test
    public void updateWithInnerDependency() {
        Product productIndDb = new Product();
        dataRepository.save(productIndDb);
        ProductOutcome productOutcomeInDb = new ProductOutcome();
        productOutcomeInDb.setProduct(productIndDb);
        dataRepository.save(productOutcomeInDb);

        Product product = new Product();
        product.setName("updated name");
        product.setId(productIndDb.getId());
        product.setVersion(0L);
        product.setEntries(Collections.emptyList());

        List<?> result = syncService.write(Collections.singletonList(
                Sync.of(product, SyncType.UPDATE, null, null)));

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isOfAnyClassIn(Product.class);
        Product resultProduct = (Product) result.get(0); 
        assertThat(resultProduct.getId()).isEqualTo(productIndDb.getId());
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
                Sync.of(entityOne, SyncType.UPDATE, null, null),
                Sync.of(entityTwo, SyncType.UPDATE, null, null)));

        assertThat(result.get(0).getInnerEntity()).isSameAs(result.get(1).getInnerEntity());
    }
}