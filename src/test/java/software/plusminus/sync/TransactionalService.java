package software.plusminus.sync;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionalService {

    @Transactional
    public void inTransaction(Runnable runnable) {
        runnable.run();
    }
}
