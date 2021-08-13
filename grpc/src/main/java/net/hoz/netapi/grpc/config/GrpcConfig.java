package net.hoz.netapi.grpc.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Frantisek Novosad (fnovosad@monetplus.cz)
 */
@Slf4j
@Data
public class GrpcConfig {
    private UUID serviceId;
    private int threadsCount;
    private int activeServers;

    private List<String> address;
    private List<String> token;

    private Integer checkTimeSeconds = 15;

    private Map<String, String> mappedCache;

    public Map<String, String> toMap() {
        if (address.size() != token.size()) {
            log.warn("Configured addresses and tokens for gRPC are different in size!");
            System.exit(1);
        }

        if (mappedCache != null) {
            return mappedCache;
        }

        final var addressI = address.iterator();
        final var tokenI = token.iterator();

        mappedCache = IntStream
                .range(0, activeServers).boxed()
                .collect(Collectors.toMap(number -> addressI.next(), number -> tokenI.next()));
        return mappedCache;
    }
}
