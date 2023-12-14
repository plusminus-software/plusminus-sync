package software.plusminus.sync.service.fetcher;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.Callable;

@Service
public class SyncTransactionService {
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> T newTransaction(Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional
    public void run(Runnable runnable) {
        runnable.run();
    }
    
}
