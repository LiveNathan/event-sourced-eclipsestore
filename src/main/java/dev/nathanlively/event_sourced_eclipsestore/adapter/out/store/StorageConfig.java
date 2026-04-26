package dev.nathanlively.event_sourced_eclipsestore.adapter.out.store;

import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    @Bean
    public DataRoot dataRoot(EmbeddedStorageManager storageManager) {
        Object root = storageManager.root();
        if (root instanceof DataRoot existing) {
            return existing;
        }
        DataRoot fresh = new DataRoot();
        storageManager.setRoot(fresh);
        storageManager.storeRoot();
        return fresh;
    }

    @Bean
    public EclipseStoreEventStore eclipseStoreEventStore(EmbeddedStorageManager storageManager, DataRoot dataRoot) {
        return new EclipseStoreEventStore(storageManager, dataRoot);
    }
}
