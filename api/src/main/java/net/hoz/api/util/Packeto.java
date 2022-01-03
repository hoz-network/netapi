package net.hoz.api.util;

import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import com.iamceph.resulter.core.DataResultable;
import com.iamceph.resulter.core.Resultable;
import lombok.experimental.UtilityClass;
import net.hoz.api.data.ResultableData;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.screamingsandals.lib.utils.ProtoWrapper;

@UtilityClass
public class Packeto {

    /**
     * Packs the {@link DataResultable} into a {@link ResultableData}.
     * <p>
     * NOTE: the {@link DataResultable#data()} object needs to implement {@link ProtoWrapper}!
     *
     * @param input input for packing
     * @return packed {@link ResultableData}.
     */
    @NotNull
    public ResultableData pack(@NotNull DataResultable<?> input) {
        if (input.isFail()) {
            return ResultableData.newBuilder()
                    .setResult(input.convertor().grpc())
                    .build();
        }

        final var data = input.data();
        if (data instanceof ProtoWrapper wrapper) {
            if (wrapper.asProto() instanceof Message message) {
                return ResultableData.newBuilder()
                        .setResult(input.convertor().grpc())
                        .setData(Any.pack(message))
                        .build();
            } else {
                return ResultableData.newBuilder()
                        .setResult(DataResultable.fail("Cannot convert data - not proto message - " + data.getClass().getSimpleName())
                                .convertor()
                                .grpc())
                        .build();
            }
        }

        return ResultableData.newBuilder()
                .setResult(DataResultable.fail("Cannot convert data - " + data.getClass().getSimpleName())
                        .convertor()
                        .grpc())
                .build();
    }

    /**
     * Unpacks given {@link ResultableData} into a {@link DataResultable}.
     *
     * @param input input to unpack
     * @param type  type to unpack with
     * @param <K>   type of the unpacking class
     * @return Unpacked data in {@link DataResultable}
     */
    @NotNull
    public <K extends Message> DataResultable<K> unpack(@NotNull ResultableData input, @NotNull Class<K> type) {
        final var result = Resultable.convert(input.getResult());
        if (result.isFail()) {
            return result.transform();
        }

        try {
            return DataResultable.failIfNull(input.getData().unpack(type));
        } catch (Exception e) {
            return DataResultable.fail(e);
        }
    }

    /**
     * Unpacks given {@link ResultableData} into a {@link DataResultable}.
     *
     * @param input input to unpack
     * @param type  type to unpack with
     * @param <K>   type of the unpacking class
     * @return Unpacked data in {@link DataResultable}
     */
    @NotNull
    public <K extends Message> DataResultable<K> unpack(@NotNull Any input, @NotNull Class<K> type) {
        try {
            return DataResultable.failIfNull(input.unpack(type));
        } catch (Exception e) {
            return DataResultable.fail(e);
        }
    }

    /**
     * Unpacks given {@link Any} data.
     * NOTE: This method is unsafe, use {@link Packeto#unpack(ResultableData, Class)} or {@link Packeto#unpack(Any, Class)} rather.
     *
     * @param input input to unpack
     * @param type  type to unpack with
     * @param <K>   type of the unpacking class
     * @return Unpacked data in {@link DataResultable}
     */
    @ApiStatus.Internal
    @Nullable
    public <K extends Message> K unpackUnsafe(@NotNull Any input, @NotNull Class<K> type) {
        try {
            return input.unpack(type);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}