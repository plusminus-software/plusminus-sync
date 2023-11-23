package software.plusminus.sync.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.Callable;

@Service
public class SyncTransactionService {
    
    @Transactional
    public void run(Runnable runnable) {
        runnable.run();
    }
    
    @Transactional
    public <T> T call(Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
    
}
