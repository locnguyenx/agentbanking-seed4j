package com.agentbanking.prepaid.infrastructure.registry;

import com.agentbanking.prepaid.domain.model.TopUpProvider;
import com.agentbanking.prepaid.domain.port.out.TopUpProviderRegistryPort;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class PrepaidProviderRegistry implements TopUpProviderRegistryPort {

    private static final List<TopUpProvider> PROVIDERS = Arrays.stream(TopUpProvider.values()).toList();

    @Override
    public Optional<TopUpProvider> findByCode(String providerCode) {
        if (providerCode == null || providerCode.isBlank()) {
            return Optional.empty();
        }
        return PROVIDERS.stream()
            .filter(p -> p.name().equalsIgnoreCase(providerCode))
            .findFirst();
    }

    @Override
    public List<TopUpProvider> findAll() {
        return PROVIDERS;
    }
}